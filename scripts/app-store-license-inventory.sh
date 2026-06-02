#!/usr/bin/env bash
# Generate a local App Store license inventory draft for Wakeve.
# This is a repository-side evidence helper; final AS-21 closure still requires
# review against the exact signed App Store build.

set -euo pipefail

OUTPUT=""
FAIL_ON_UNKNOWN=false
FETCH_MAVEN_POMS=false
FETCH_RUBYGEMS=false

usage() {
    cat <<'EOF'
Usage: ./scripts/app-store-license-inventory.sh [options]

Options:
  --output PATH       Write the Markdown inventory to PATH instead of stdout
  --fail-on-unknown  Exit non-zero if the submitted iOS scope has an unknown license or copyleft-risk keyword
  --fetch-maven-poms Fetch missing Maven POMs from Google Maven/Maven Central
  --fetch-rubygems   Fetch missing Ruby gem licenses from the RubyGems API
  --fetch-remote-metadata
                     Enable all supported remote metadata fetchers
  --help, -h         Show this help

The report is a draft input for docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md.
It does not prove final App Store readiness by itself.
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --output)
            if [ "$#" -lt 2 ]; then
                echo "Missing value for --output" >&2
                exit 2
            fi
            OUTPUT="$2"
            shift 2
            ;;
        --fail-on-unknown)
            FAIL_ON_UNKNOWN=true
            shift
            ;;
        --fetch-maven-poms)
            FETCH_MAVEN_POMS=true
            shift
            ;;
        --fetch-rubygems)
            FETCH_RUBYGEMS=true
            shift
            ;;
        --fetch-remote-metadata)
            FETCH_MAVEN_POMS=true
            FETCH_RUBYGEMS=true
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage
            exit 2
            ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

generate_report() {
    local ruby_cmd=(ruby)
    if [ -f "$PROJECT_DIR/Gemfile.lock" ] && command -v bundle >/dev/null 2>&1; then
        ruby_cmd=(bundle exec ruby)
    fi

    FETCH_MAVEN_POMS="$FETCH_MAVEN_POMS" FETCH_RUBYGEMS="$FETCH_RUBYGEMS" BUNDLE_GEMFILE="$PROJECT_DIR/Gemfile" "${ruby_cmd[@]}" - "$PROJECT_DIR" <<'RUBY'
require "date"
require "json"
require "net/http"
require "rexml/document"
require "rexml/xpath"
require "fileutils"
require "rubygems"
require "rubygems/package"
require "set"
require "tmpdir"
require "uri"

project_dir = ARGV.fetch(0)

Dependency = Struct.new(:ecosystem, :name, :version, :source, :scope, :license, keyword_init: true)
FETCH_MAVEN_POMS = ENV["FETCH_MAVEN_POMS"] == "true"
FETCH_RUBYGEMS = ENV["FETCH_RUBYGEMS"] == "true"

def read_json(path)
  return {} unless File.file?(path)
  JSON.parse(File.read(path))
rescue JSON::ParserError
  {}
end

def normalize(value)
  value.to_s.strip.empty? ? "unknown" : value.to_s.strip
end

def node_license(package_json)
  license = package_json["license"]
  license = package_json.dig("licenses", 0, "type") if license.to_s.strip.empty?
  license = package_json["licenses"].map { |entry| entry["type"] }.compact.join(", ") if license.to_s.strip.empty? && package_json["licenses"].is_a?(Array)
  normalize(license)
end

def installed_node_packages(package_path, ecosystem)
  root = File.dirname(package_path)
  node_modules = File.join(root, "node_modules")
  return [] unless Dir.exist?(node_modules)

  package_paths = []
  package_paths.concat(Dir.glob(File.join(node_modules, "*", "package.json")))
  package_paths.concat(Dir.glob(File.join(node_modules, "@*", "*", "package.json")))
  package_paths.concat(Dir.glob(File.join(node_modules, ".pnpm", "*", "node_modules", "*", "package.json")))
  package_paths.concat(Dir.glob(File.join(node_modules, ".pnpm", "*", "node_modules", "@*", "*", "package.json")))

  package_paths.uniq.map do |installed_path|
    installed_json = read_json(installed_path)
    name = installed_json["name"]
    next nil if name.to_s.strip.empty?
    next nil if installed_json["private"] == true

    Dependency.new(
      ecosystem: ecosystem,
      name: name,
      version: normalize(installed_json["version"]),
      source: "node_modules",
      scope: ecosystem == "npm-web" ? "web-notice-surface" : "workspace-tooling",
      license: node_license(installed_json)
    )
  end.compact
end

def package_manager_dependencies(project_dir)
  deps = []

  {
    "npm-root" => File.join(project_dir, "package.json"),
    "npm-web" => File.join(project_dir, "webApp", "package.json")
  }.each do |ecosystem, package_path|
    package_json = read_json(package_path)
    next if package_json.empty?

    deps.concat(installed_node_packages(package_path, ecosystem))

    node_modules_dir = File.join(File.dirname(package_path), "node_modules")
    %w[dependencies devDependencies optionalDependencies peerDependencies].each do |section|
      package_json.fetch(section, {}).sort.each do |name, requested_version|
        installed_path = File.join(node_modules_dir, name, "package.json")
        installed_json = read_json(installed_path)
        deps << Dependency.new(
          ecosystem: ecosystem,
          name: name,
          version: normalize(installed_json["version"] || requested_version),
          source: section,
          scope: ecosystem == "npm-web" ? "web-notice-surface" : "workspace-tooling",
          license: node_license(installed_json)
        )
      end
    end
  end

  deps
end

def gradle_pom_license(module_name, version)
  group, artifact = module_name.split(":", 2)
  return "unknown" if group.to_s.empty? || artifact.to_s.empty? || version.to_s.empty? || version == "unknown"

  cache_root = File.join(Dir.home, ".gradle", "caches", "modules-2", "files-2.1")
  pom_paths = Dir.glob(File.join(cache_root, group, artifact, version, "*", "#{artifact}-#{version}.pom"))
  pom_paths = Dir.glob(File.join(cache_root, group, artifact, version, "*", "*.pom")) if pom_paths.empty?

  pom_paths.each do |pom_path|
    license = parse_pom_license(File.read(pom_path))
    return license unless license == "unknown"
  rescue Errno::ENOENT
    next
  end

  return "unknown" unless FETCH_MAVEN_POMS

  fetch_remote_pom_license(group, artifact, version)
end

def parse_pom_license(xml)
  doc = REXML::Document.new(xml)
  licenses = []
  REXML::XPath.each(doc, "//license/name") { |node| licenses << node.text.to_s.strip }
  licenses = licenses.reject(&:empty?).uniq
  licenses.empty? ? "unknown" : licenses.join(", ")
rescue REXML::ParseException
  "unknown"
end

def fetch_remote_pom_license(group, artifact, version)
  group_path = group.tr(".", "/")
  pom_path = "#{group_path}/#{artifact}/#{version}/#{artifact}-#{version}.pom"
  repositories = [
    "https://dl.google.com/dl/android/maven2",
    "https://repo1.maven.org/maven2"
  ]

  repositories.each do |base_url|
    uri = URI("#{base_url}/#{pom_path}")
    response = Net::HTTP.start(uri.host, uri.port, use_ssl: uri.scheme == "https", open_timeout: 5, read_timeout: 10) do |http|
      http.get(uri.request_uri)
    end
    next unless response.is_a?(Net::HTTPSuccess)

    license = parse_pom_license(response.body)
    return "#{license} (fetched POM)" unless license == "unknown"

    parent_license = fetch_parent_pom_license(response.body)
    return parent_license unless parent_license == "unknown"
  rescue StandardError
    next
  end

  "unknown"
end

def fetch_parent_pom_license(xml)
  doc = REXML::Document.new(xml)
  group = REXML::XPath.first(doc, "//parent/groupId")&.text.to_s.strip
  artifact = REXML::XPath.first(doc, "//parent/artifactId")&.text.to_s.strip
  version = REXML::XPath.first(doc, "//parent/version")&.text.to_s.strip
  return "unknown" if group.empty? || artifact.empty? || version.empty?

  parent_license = fetch_remote_pom_license(group, artifact, version)
  parent_license == "unknown" ? parent_license : "#{parent_license}; inherited from #{group}:#{artifact}:#{version}"
rescue REXML::ParseException
  "unknown"
end

def gradle_alias_accessor(alias_name)
  "libs." + alias_name.tr("-_", ".")
end

def gradle_alias_usages(project_dir)
  gradle_files = Dir.glob(File.join(project_dir, "**", "*.gradle.kts")).reject do |path|
    path.split(File::SEPARATOR).any? { |part| part == "build" || part == ".gradle" }
  end

  usages = Hash.new { |hash, key| hash[key] = Set.new }
  gradle_files.each do |path|
    relative_path = path.delete_prefix(project_dir + File::SEPARATOR)
    File.readlines(path, chomp: true).each do |line|
      line.scan(/libs\.([A-Za-z0-9_.]+)/).each do |match|
        usages[match.first] << relative_path
      end
    end
  end

  usages.transform_values { |value| value.to_a.sort }
end

def gradle_app_store_scope(usage_paths)
  return "submitted-ios" if usage_paths.any? { |path| path.start_with?("shared/") || path.start_with?("composeApp/") }
  return "backend-server" if usage_paths.any? { |path| path.start_with?("server/") }
  "gradle-tooling"
end

def parse_gradle_catalog(project_dir)
  path = File.join(project_dir, "gradle", "libs.versions.toml")
  return [] unless File.file?(path)

  versions = {}
  libraries = []
  section = nil
  alias_usages = gradle_alias_usages(project_dir)

  File.readlines(path, chomp: true).each do |raw_line|
    line = raw_line.sub(/#.*/, "").strip
    next if line.empty?

    if line =~ /^\[(.+)\]$/
      section = Regexp.last_match(1)
      next
    end

    if section == "versions" && line =~ /^([A-Za-z0-9_.-]+)\s*=\s*"([^"]+)"/
      versions[Regexp.last_match(1)] = Regexp.last_match(2)
      next
    end

    next unless section == "libraries"
    next unless line =~ /^([A-Za-z0-9_.-]+)\s*=\s*\{(.+)\}\s*$/

    alias_name = Regexp.last_match(1)
    alias_accessor = alias_name.tr("-_", ".")
    next unless alias_usages.key?(alias_accessor)

    body = Regexp.last_match(2)
    module_name = body[/module\s*=\s*"([^"]+)"/, 1]
    version_ref = body[/version\.ref\s*=\s*"([^"]+)"/, 1]
    explicit_version = body[/version\s*=\s*"([^"]+)"/, 1]
    version = explicit_version || versions[version_ref] || "unknown"
    next unless module_name

    libraries << Dependency.new(
      ecosystem: "gradle-version-catalog",
      name: module_name,
      version: version,
      source: "#{alias_name} (#{alias_usages.fetch(alias_accessor).join(", ")})",
      scope: gradle_app_store_scope(alias_usages.fetch(alias_accessor)),
      license: gradle_pom_license(module_name, version)
    )
  end

  libraries
end

def gem_license(name, version)
  spec = Gem::Specification.find_all_by_name(name).find { |candidate| candidate.version.to_s == version }
  spec ||= Gem.loaded_specs[name] if Gem.loaded_specs[name]&.version&.to_s == version
  if spec
    licenses = spec.licenses
    licenses = [spec.license] if licenses.empty? && spec.respond_to?(:license)
    license = normalize(licenses.compact.join(", "))
    return license unless license == "unknown"
  end

  return "unknown" unless FETCH_RUBYGEMS

  fetch_rubygems_license(name, version)
end

def fetch_rubygems_license(name, version)
  escaped_name = URI.encode_www_form_component(name)
  escaped_version = URI.encode_www_form_component(version)
  uri = URI("https://rubygems.org/api/v2/rubygems/#{escaped_name}/versions/#{escaped_version}.json")
  response = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 5, read_timeout: 10) do |http|
    http.get(uri.request_uri)
  end
  return "unknown" unless response.is_a?(Net::HTTPSuccess)

  payload = JSON.parse(response.body)
  license = normalize(payload["licenses"].is_a?(Array) ? payload["licenses"].join(", ") : payload["licenses"])
  return "#{license} (fetched RubyGems version)" unless license == "unknown"

  fetch_rubygems_latest_license(name, version)
rescue StandardError
  "unknown"
end

def fetch_rubygems_latest_license(name, version)
  escaped_name = URI.encode_www_form_component(name)
  uri = URI("https://rubygems.org/api/v1/gems/#{escaped_name}.json")
  response = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 5, read_timeout: 10) do |http|
    http.get(uri.request_uri)
  end
  return "unknown" unless response.is_a?(Net::HTTPSuccess)

  payload = JSON.parse(response.body)
  license = normalize(payload["licenses"].is_a?(Array) ? payload["licenses"].join(", ") : payload["licenses"])
  return "#{license} (fetched RubyGems gem)" unless license == "unknown"

  fetch_rubygems_archive_license(name, version)
rescue StandardError
  "unknown"
end

def fetch_rubygems_archive_license(name, version)
  escaped_file = URI.encode_www_form_component("#{name}-#{version}.gem")
  uri = URI("https://rubygems.org/downloads/#{escaped_file}")
  response = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 5, read_timeout: 20) do |http|
    http.get(uri.request_uri)
  end
  return "unknown" unless response.is_a?(Net::HTTPSuccess)

  Dir.mktmpdir("wakeve-gem-license") do |dir|
    gem_path = File.join(dir, "#{name}-#{version}.gem")
    File.binwrite(gem_path, response.body)
    extract_dir = File.join(dir, "extract")
    FileUtils.mkdir_p(extract_dir)
    Gem::Package.new(gem_path).extract_files(extract_dir)
    license_file = Dir.glob(File.join(extract_dir, "**", "*"), File::FNM_DOTMATCH).find do |path|
      File.file?(path) && File.basename(path) =~ /\A(license|licence|copying)(\..*)?\z/i
    end
    return "unknown" unless license_file

    text = File.read(license_file)
    if text.include?("Permission is hereby granted, free of charge") && text.include?("THE SOFTWARE IS PROVIDED")
      return "MIT (fetched RubyGems archive LICENSE)"
    end
  end

  "unknown"
rescue StandardError
  "unknown"
end

def parse_gemfile_lock(project_dir)
  path = File.join(project_dir, "Gemfile.lock")
  return [] unless File.file?(path)

  deps = []
  in_specs = false

  File.readlines(path, chomp: true).each do |line|
    if line.strip == "specs:"
      in_specs = true
      next
    end

    if in_specs && line =~ /^[A-Z][A-Z ]+$/
      in_specs = false
      next
    end

    next unless in_specs
    next unless line =~ /^\s{4}([A-Za-z0-9_.-]+)\s+\(([^)]+)\)/

    deps << Dependency.new(
      ecosystem: "ruby-gem",
      name: Regexp.last_match(1),
      version: Regexp.last_match(2),
      source: "Gemfile.lock",
      scope: "release-tooling",
      license: gem_license(Regexp.last_match(1), Regexp.last_match(2))
    )
  end

  deps
end

def local_notice_files(project_dir)
  roots = %w[iosApp shared composeApp webApp fastlane server gradle]
  roots.flat_map do |root|
    root_path = File.join(project_dir, root)
    next [] unless Dir.exist?(root_path)

    Dir.glob(File.join(root_path, "**", "*"), File::FNM_DOTMATCH).select do |path|
      File.file?(path) &&
        path.split(File::SEPARATOR).none? { |part| part == "build" || part == "node_modules" || part == ".gradle" } &&
        File.basename(path) =~ /\A(license|licence|notice|copying)(\..*)?\z/i
    end
  end.sort.map { |path| path.delete_prefix(project_dir + File::SEPARATOR) }
end

deps = []
deps.concat(package_manager_dependencies(project_dir))
deps.concat(parse_gradle_catalog(project_dir))
deps.concat(parse_gemfile_lock(project_dir))
deps = deps.uniq { |dep| [dep.ecosystem, dep.name, dep.version, dep.source, dep.scope] }

unknown = deps.select { |dep| dep.license == "unknown" }
copyleft_license_pattern = /\b(agpl|gpl|lgpl)\b|general public license|lesser general public license/i
risky = deps.select { |dep| dep.license =~ copyleft_license_pattern }
submitted_ios = deps.select { |dep| dep.scope == "submitted-ios" }
submitted_unknown = submitted_ios.select { |dep| dep.license == "unknown" }
submitted_risky = submitted_ios.select { |dep| dep.license =~ copyleft_license_pattern }
notice_files = local_notice_files(project_dir)

puts "# App Store License Inventory Draft - Wakeve"
puts
puts "Generated: #{Date.today.iso8601}"
puts
puts "Status: DRAFT"
puts
puts "This inventory is generated from repository manifests and installed package metadata when available. It is input evidence for `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md`; it does not prove App Store readiness until reviewed against the signed IPA/archive."
puts
puts "## Summary"
puts
puts "- Dependencies listed: #{deps.length}"
puts "- Unknown licenses: #{unknown.length}"
puts "- Copyleft keywords detected: #{risky.length}"
puts "- Submitted iOS unknown/copyleft risks: #{submitted_unknown.length + submitted_risky.length}"
puts "- Local LICENSE/NOTICE/COPYING files found: #{notice_files.length}"
puts "- Remote Maven POM fetching: #{FETCH_MAVEN_POMS ? "enabled" : "disabled"}"
puts "- Remote RubyGems fetching: #{FETCH_RUBYGEMS ? "enabled" : "disabled"}"
puts
puts "## Dependency Inventory"
puts
puts "| Ecosystem | Name | Version | Source | App Store Scope | License |"
puts "| --- | --- | --- | --- | --- | --- |"
deps.sort_by { |dep| [dep.ecosystem, dep.name] }.each do |dep|
  values = [dep.ecosystem, dep.name, dep.version, dep.source, dep.scope, dep.license].map do |value|
    value.to_s.gsub("|", "\\|")
  end
  puts "| #{values.join(" | ")} |"
end
puts
puts "## Local Notice Files"
puts
if notice_files.empty?
  puts "- None found."
else
  notice_files.each { |path| puts "- `#{path}`" }
end
puts
puts "## Review Notes"
puts
if unknown.empty?
  puts "- No unknown licenses were found from available local metadata."
else
  puts "- Unknown licenses require manual lookup before `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true`."
end
if risky.empty?
  puts "- No AGPL/GPL/LGPL or General Public License keyword was detected in local dependency license metadata."
else
  puts "- Copyleft keywords were detected and require scope-aware legal review before submission."
end
if submitted_unknown.empty? && submitted_risky.empty?
  puts "- No unknown or copyleft-risk license was detected in dependencies marked as submitted-ios scope by this draft inventory."
else
  puts "- Unknown or copyleft-risk licenses were detected in submitted-ios scope and require resolution before App Store submission."
end
RUBY
}

CHECK_FILE=""
TEMP_REPORT=""

if [ -n "$OUTPUT" ]; then
    CHECK_FILE="$PROJECT_DIR/$OUTPUT"
    mkdir -p "$(dirname "$CHECK_FILE")"
    generate_report > "$CHECK_FILE"
else
    TEMP_REPORT="$(mktemp)"
    CHECK_FILE="$TEMP_REPORT"
    generate_report > "$TEMP_REPORT"
    cat "$TEMP_REPORT"
fi

if [ "$FAIL_ON_UNKNOWN" = true ]; then
    if grep -Eq "^- Submitted iOS unknown/copyleft risks: [1-9][0-9]*$" "$CHECK_FILE"; then
        echo "License inventory contains submitted iOS unknown or copyleft-risk licenses" >&2
        [ -n "$TEMP_REPORT" ] && rm -f "$TEMP_REPORT"
        exit 1
    fi
fi

if [ -n "$TEMP_REPORT" ]; then
    rm -f "$TEMP_REPORT"
fi

exit 0
