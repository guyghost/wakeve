#!/usr/bin/env ruby
# Capture local build facts only; this does not authorize a signed release.
require 'digest'
require 'fileutils'
require 'json'
require 'open3'
require 'time'

root = File.expand_path('..', __dir__)
validate = ARGV.first == '--validate'
abort 'Usage: capture-unsigned-release-evidence.rb [--validate REPORT]' unless ARGV.empty? || (!validate && ARGV.length == 1) || (validate && ARGV.length == 2)
output = validate ? ARGV.fetch(1) : (ARGV.first || ENV.fetch('APP_STORE_UNSIGNED_RELEASE_EVIDENCE_FILE'))
abort 'Evidence output path is empty' if output.empty?
output = File.expand_path(output, root)
FileUtils.rm_f(output) unless validate
app = File.join(root, 'build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app')
artifacts = {
  'app_binary' => File.join(app, 'Wakeve'),
  'shared_binary' => File.join(app, 'Frameworks/Shared.framework/Shared'),
  'info_plist' => File.join(app, 'Info.plist'),
  'privacy_manifest' => File.join(app, 'PrivacyInfo.xcprivacy')
}
artifacts.each_value { |path| abort "Missing release artifact: #{path}" unless File.file?(path) }

%w[app_binary shared_binary].each do |key|
  _, stderr, status = Open3.capture3('/usr/bin/codesign', '-dv', artifacts.fetch(key))
  abort "Expected unsigned #{key}: #{stderr}" unless !status.success? && stderr.include?('code object is not signed at all')
end

def uuids(path)
  stdout, stderr, status = Open3.capture3('/usr/bin/dwarfdump', '--uuid', path)
  abort "Cannot read UUIDs from #{path}: #{stderr}" unless status.success?
  result = stdout.scan(/^UUID: ([0-9A-Fa-f-]+) \(([^)]+)\)/).map do |uuid, architecture|
    { 'uuid' => uuid.upcase, 'architecture' => architecture }
  end.sort_by { |entry| entry.fetch('architecture') }
  abort "No UUIDs in #{path}" if result.empty?
  result
end

uuid_paths = {
  'app_binary' => artifacts.fetch('app_binary'),
  'app_dsym' => "#{app}.dSYM",
  'shared_binary' => artifacts.fetch('shared_binary'),
  'shared_dsym' => File.join(root, 'shared/build/bin/iosArm64/releaseFramework/Shared.framework.dSYM')
}
recorded_uuids = uuid_paths.transform_values { |path| uuids(path) }
%w[app shared].each do |prefix|
  abort "#{prefix} binary/dSYM UUID mismatch" unless recorded_uuids.fetch("#{prefix}_binary") == recorded_uuids.fetch("#{prefix}_dsym")
end
revision, stderr, status = Open3.capture3('git', '-C', root, 'rev-parse', 'HEAD')
abort "Cannot capture revision: #{stderr}" unless status.success?
evidence = {
  'schema_version' => 1,
  'unsigned' => true,
  'captured_at' => Time.now.utc.iso8601,
  'git_revision' => revision.strip,
  'app_path' => app.delete_prefix("#{root}/"),
  'hashes' => artifacts.transform_values { |path| Digest::SHA256.file(path).hexdigest },
  'uuids' => recorded_uuids
}
if validate
  begin
    recorded = JSON.parse(File.read(output))
    abort 'Evidence schema differs' unless recorded.is_a?(Hash) && recorded.keys.sort == evidence.keys.sort
    captured_at = recorded.fetch('captured_at')
    abort 'Invalid capture timestamp' unless captured_at.is_a?(String) && Time.iso8601(captured_at).utc.iso8601 == captured_at
    %w[schema_version unsigned git_revision app_path hashes uuids].each do |key|
      abort "Evidence #{key} differs from actual unsigned Release artifacts" unless recorded.fetch(key).class == evidence.fetch(key).class && recorded.fetch(key) == evidence.fetch(key)
    end
  rescue JSON::ParserError, Errno::ENOENT, ArgumentError => error
    abort "Invalid unsigned Release evidence: #{error.message}"
  end
  puts "Validated actual unsigned Release evidence: #{output}"
else
  FileUtils.mkdir_p(File.dirname(output))
  File.write(output, JSON.pretty_generate(evidence) + "\n")
  puts "Captured actual unsigned Release evidence: #{output}"
end
