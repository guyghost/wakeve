#!/usr/bin/env ruby

require 'set'

root, mode = ARGV
abort 'usage: audit_product_language.rb ROOT MODE' unless root && %w[all catalogs terms].include?(mode)

locales = %w[en fr de es it pt]
findings = []
fixture = File.directory?(File.join(root, 'android'))
allowlist_path = File.join(root, 'scripts/product-language-allowlist.txt')
allowlist = if File.file?(allowlist_path)
              File.readlines(allowlist_path, chomp: true)
                  .reject { |line| line.empty? || line.start_with?('#') }
                  .to_set
            else
              Set.new
            end
forbidden = /Wakeve AI|\bSc[eé]nario(?:s)?\b|\bScenario(?:s)?\b|\bInbox\b|\bGenerate\b|\bGénérer\b|Party Animal|Social Butterfly|Chatterbox|Event Master|Maître du vote/i

android_paths = locales.to_h do |locale|
  directory = locale == 'en' ? 'values' : "values-#{locale}"
  base = fixture ? 'android' : 'composeApp/src/androidMain/res'
  [locale, File.join(root, base, directory, 'strings.xml')]
end

ios_paths = if fixture
              {}
            else
              locales.to_h do |locale|
                [locale, File.join(root, "iosApp/src/Resources/#{locale}.lproj/Localizable.strings")]
              end
            end
siri_paths = if fixture
               {}
             else
               locales.to_h do |locale|
                 [locale, File.join(root, "iosApp/src/Siri/#{locale}.lproj/SiriIntents.strings")]
               end
             end

def android_entries(path)
  return {} unless File.file?(path)

  File.read(path)
      .scan(/<(?:string|plurals|string-array)\b[^>]*\bname="([^"]+)"[^>]*>(.*?)<\/(?:string|plurals|string-array)>/m)
      .to_h
      .transform_values { |value| value.gsub(/<[^>]+>/, ' ').gsub(/\s+/, ' ').strip }
end

def apple_entries(path)
  return {} unless File.file?(path)

  File.read(path).scan(/^\s*"((?:\\.|[^"])*)"\s*=\s*"((?:\\.|[^"])*)"\s*;/).to_h
end

catalogs = {
  'android' => android_paths.transform_values { |path| android_entries(path) },
  'ios' => ios_paths.transform_values { |path| apple_entries(path) },
  'siri' => siri_paths.transform_values { |path| apple_entries(path) }
}
paths = { 'android' => android_paths, 'ios' => ios_paths, 'siri' => siri_paths }

if %w[all catalogs].include?(mode)
  catalogs.each do |platform, entries_by_locale|
    next if entries_by_locale.empty?

    base_keys = entries_by_locale.fetch('en').keys.to_set
    locales.each do |locale|
      path = paths.fetch(platform).fetch(locale)
      findings << "#{platform} #{locale}: missing file #{path}" unless File.file?(path)
      keys = entries_by_locale.fetch(locale, {}).keys.to_set
      (base_keys - keys).sort.each { |key| findings << "#{platform} #{locale}: missing #{key}" }
      (keys - base_keys).sort.each { |key| findings << "#{platform} #{locale}: extra #{key}" }
    end
  end
end

if %w[all terms].include?(mode)
  catalogs.each do |platform, entries_by_locale|
    entries_by_locale.each do |locale, entries|
      entries.each do |key, value|
        identity = "#{platform}/#{locale}:#{key}"
        findings << "#{identity}: forbidden visible term" if value.match?(forbidden) && !allowlist.include?(identity)
      end
    end
  end

  source_globs = ['composeApp/src/androidMain/**/*.kt', 'iosApp/src/**/*.swift']
  source_globs.flat_map { |glob| Dir.glob(File.join(root, glob)) }.sort.each do |path|
    next if path.include?('/Preview') || path.include?('/Tests/')

    File.readlines(path, chomp: true).each_with_index do |line, index|
      relative = path.delete_prefix("#{root}/")
      identity = "#{relative}:#{index + 1}"
      findings << "#{identity}: forbidden visible term" if line.match?(forbidden) && !allowlist.include?(identity)
      literal = line.match?(/(?:Text|Button|accessibilityLabel|contentDescription)\s*[=(]\s*"[^"\\]+"/)
      findings << "#{identity}: production literal" if literal && !allowlist.include?(identity)
    end
  end
end

puts findings.uniq.sort
exit findings.empty? ? 0 : 1
