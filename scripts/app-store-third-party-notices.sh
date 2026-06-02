#!/usr/bin/env bash
# Generate third-party notices from the App Store license inventory draft.

set -euo pipefail

INVENTORY="docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md"
MARKDOWN_OUTPUT=""
WEB_OUTPUT=""

usage() {
    cat <<'EOF'
Usage: ./scripts/app-store-third-party-notices.sh [options]

Options:
  --inventory PATH        Inventory Markdown input
  --markdown-output PATH  Write a Markdown notices file
  --web-output PATH       Write a Svelte public notices route
  --help, -h             Show this help
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --inventory)
            INVENTORY="$2"
            shift 2
            ;;
        --markdown-output)
            MARKDOWN_OUTPUT="$2"
            shift 2
            ;;
        --web-output)
            WEB_OUTPUT="$2"
            shift 2
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

ruby - "$PROJECT_DIR" "$INVENTORY" "$MARKDOWN_OUTPUT" "$WEB_OUTPUT" <<'RUBY'
require "cgi"
require "date"
require "fileutils"

project_dir, inventory_path, markdown_output, web_output = ARGV
inventory_full_path = File.join(project_dir, inventory_path)

unless File.file?(inventory_full_path)
  warn "Inventory not found: #{inventory_path}"
  exit 1
end

Dependency = Struct.new(:ecosystem, :name, :version, :source, :scope, :license, keyword_init: true)

deps = []
summary = {}

File.readlines(inventory_full_path, chomp: true).each do |line|
  if line =~ /^- (Dependencies listed|Unknown licenses|Copyleft keywords detected|Submitted iOS unknown\/copyleft risks):\s+(.+)$/
    summary[Regexp.last_match(1)] = Regexp.last_match(2)
    next
  end

  next unless line.start_with?("| ")
  next if line.include?("| --- |") || line.include?("| Ecosystem |")

  cells = line.split("|").map(&:strip)
  next unless cells.length >= 6

  if cells.length >= 7
    ecosystem = cells[1]
    name = cells[2]
    version = cells[3]
    source = cells[4]
    scope = cells[5]
    license = cells[6]
  else
    ecosystem = cells[1]
    name = cells[2]
    version = cells[3]
    source = cells[4]
    scope = "unclassified"
    license = cells[5]
  end

  deps << Dependency.new(
    ecosystem: ecosystem,
    name: name,
    version: version,
    source: source,
    scope: scope,
    license: license
  )
end

deps = deps.sort_by { |dep| [dep.license, dep.ecosystem, dep.name, dep.version] }
generated = Date.today.iso8601

license_groups = deps.group_by(&:license).sort_by { |license, grouped| [license, grouped.length] }

def markdown_table(deps)
  lines = []
  lines << "| Name | Version | Ecosystem | App Store Scope | License |"
  lines << "| --- | --- | --- | --- | --- |"
  deps.each do |dep|
    values = [dep.name, dep.version, dep.ecosystem, dep.scope, dep.license].map { |value| value.to_s.gsub("|", "\\|") }
    lines << "| #{values.join(" | ")} |"
  end
  lines.join("\n")
end

def html(value)
  CGI.escapeHTML(value.to_s)
end

markdown = <<~MARKDOWN
  # Third-Party Notices - Wakeve

  Generated: #{generated}

  Status: DRAFT

  This notice file is generated from `#{inventory_path}` for App Store submission preparation. It lists third-party dependency names, versions, ecosystems, and license identifiers found in the current inventory. It does not prove App Store readiness by itself. Final release approval still requires review against the exact signed App Store build and confirmation that any required full notice text is bundled in-app or published through the approved legal/support surface.

  ## Summary

  - Dependencies listed: #{summary.fetch("Dependencies listed", deps.length.to_s)}
  - Unknown licenses: #{summary.fetch("Unknown licenses", "unknown")}
  - Copyleft keywords detected: #{summary.fetch("Copyleft keywords detected", "unknown")}
  - Submitted iOS unknown/copyleft risks: #{summary.fetch("Submitted iOS unknown/copyleft risks", "unknown")}
  - Public notice URL: `https://wakeve.app/third-party-notices`

  ## License Groups

  #{license_groups.map { |license, grouped| "- #{license}: #{grouped.length}" }.join("\n")}

  ## Dependency Notices

  #{markdown_table(deps)}

  ## Release Review Rule

  Do not treat this file as final legal signoff until `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` contains `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true` for the submitted build.
MARKDOWN

if markdown_output && !markdown_output.empty?
  output_path = File.join(project_dir, markdown_output)
  FileUtils.mkdir_p(File.dirname(output_path))
  File.write(output_path, markdown)
end

if web_output && !web_output.empty?
  rows = deps.map do |dep|
    <<~ROW.chomp
                <tr class="border-b border-border/70">
                  <td class="px-3 py-3 align-top font-medium text-gray-950">#{html(dep.name)}</td>
                  <td class="px-3 py-3 align-top text-gray-700">#{html(dep.version)}</td>
                  <td class="px-3 py-3 align-top text-gray-700">#{html(dep.ecosystem)}</td>
                  <td class="px-3 py-3 align-top text-gray-700">#{html(dep.scope)}</td>
                  <td class="px-3 py-3 align-top text-gray-700">#{html(dep.license)}</td>
                </tr>
    ROW
  end.join("\n")

  group_items = license_groups.map do |license, grouped|
    %Q{          <li><span class="font-medium text-gray-950">#{html(license)}</span>: #{grouped.length}</li>}
  end.join("\n")

  svelte = <<~SVELTE
    <svelte:head>
      <title>Third-Party Notices - Wakeve</title>
      <meta
        name="description"
        content="Wakeve third-party notices and open-source dependency license inventory."
      />
    </svelte:head>

    <main class="min-h-screen bg-surface-alt">
      <article class="mx-auto max-w-5xl px-5 py-10 sm:px-8 sm:py-14">
        <a
          href="/"
          class="mb-8 inline-flex text-sm font-medium text-wakeve-600 hover:text-wakeve-700 hover:underline"
        >
          Wakeve
        </a>

        <header class="mb-10 border-b border-border pb-7">
          <p class="mb-3 text-sm font-medium text-wakeve-600">Generated #{generated}</p>
          <h1 class="text-3xl font-semibold text-gray-950 sm:text-4xl">Third-Party Notices</h1>
          <p class="mt-4 max-w-3xl text-base leading-7 text-gray-600">
            Wakeve uses third-party open-source and platform libraries. This page lists the
            dependency inventory prepared for App Store review and links each dependency to its
            recorded license identifier.
          </p>
        </header>

        <div class="space-y-10 text-gray-700">
          <section class="space-y-4">
            <h2 class="text-xl font-semibold text-gray-950">Summary</h2>
            <ul class="list-disc space-y-2 pl-5 leading-7">
              <li>Dependencies listed: #{html(summary.fetch("Dependencies listed", deps.length.to_s))}</li>
              <li>Unknown licenses: #{html(summary.fetch("Unknown licenses", "unknown"))}</li>
              <li>Copyleft keywords detected: #{html(summary.fetch("Copyleft keywords detected", "unknown"))}</li>
              <li>Submitted iOS unknown/copyleft risks: #{html(summary.fetch("Submitted iOS unknown/copyleft risks", "unknown"))}</li>
            </ul>
          </section>

          <section class="space-y-4">
            <h2 class="text-xl font-semibold text-gray-950">License Groups</h2>
            <ul class="list-disc space-y-2 pl-5 leading-7">
    #{group_items}
            </ul>
          </section>

          <section class="space-y-4">
            <h2 class="text-xl font-semibold text-gray-950">Dependency Notices</h2>
            <div class="overflow-x-auto rounded border border-border bg-white">
              <table class="min-w-full text-left text-sm">
                <thead class="bg-surface-alt text-gray-950">
                  <tr>
                    <th class="px-3 py-3 font-semibold">Name</th>
                    <th class="px-3 py-3 font-semibold">Version</th>
                    <th class="px-3 py-3 font-semibold">Ecosystem</th>
                    <th class="px-3 py-3 font-semibold">App Store Scope</th>
                    <th class="px-3 py-3 font-semibold">License</th>
                  </tr>
                </thead>
                <tbody>
    #{rows}
                </tbody>
              </table>
            </div>
          </section>

          <section class="space-y-4">
            <h2 class="text-xl font-semibold text-gray-950">Review Status</h2>
            <p class="leading-7">
              This inventory is prepared for App Store submission review. Final release approval
              requires checking the exact signed build and the license notices evidence record.
            </p>
          </section>
        </div>
      </article>
    </main>
  SVELTE

  output_path = File.join(project_dir, web_output)
  FileUtils.mkdir_p(File.dirname(output_path))
  File.write(output_path, svelte)
end
RUBY
