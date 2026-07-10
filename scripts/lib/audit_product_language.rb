#!/usr/bin/env ruby

require 'set'
require 'rexml/document'
require 'rexml/xpath'

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

ios_paths = locales.to_h do |locale|
  base = fixture ? 'ios' : 'iosApp/src/Resources'
  [locale, File.join(root, base, "#{locale}.lproj/Localizable.strings")]
end
siri_paths = locales.to_h do |locale|
  base = fixture ? 'siri' : 'iosApp/src/Siri'
  [locale, File.join(root, base, "#{locale}.lproj/SiriIntents.strings")]
end

def android_entries(path, locale, findings)
  return {} unless File.file?(path)

  document = REXML::Document.new(File.read(path))
  unless document.root&.name == 'resources'
    findings << "android #{locale}: invalid XML: root must be resources"
    return {}
  end

  entries = {}
  document.root.elements.each do |element|
    next unless %w[string plurals string-array].include?(element.name)

    key = element.attributes['name']
    next unless key

    findings << "android #{locale}: duplicate key #{key}" if entries.key?(key)
    value = REXML::XPath.match(element, './/text()').map(&:value).join(' ')
    entries[key] = value.gsub(/\s+/, ' ').strip
  end
  entries
rescue REXML::ParseException => error
  findings << "android #{locale}: invalid XML: #{error.message.lines.first.to_s.strip}"
  {}
end

def apple_entries(path)
  return {} unless File.file?(path)

  File.read(path).scan(/^\s*"((?:\\.|[^"])*)"\s*=\s*"((?:\\.|[^"])*)"\s*;/).to_h
end

catalogs = {
  'android' => android_paths.to_h { |locale, path| [locale, android_entries(path, locale, findings)] },
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

  def without_comments(source)
    result = +''
    index = 0
    state = :code
    while index < source.length
      pair = source[index, 2]
      char = source[index]
      case state
      when :code
        if pair == '//'
          result << '  '
          index += 2
          state = :line_comment
        elsif pair == '/*'
          result << '  '
          index += 2
          state = :block_comment
        elsif char == '"'
          result << char
          index += 1
          state = :string
        else
          result << char
          index += 1
        end
      when :line_comment
        result << (char == "\n" ? "\n" : ' ')
        index += 1
        state = :code if char == "\n"
      when :block_comment
        if pair == '*/'
          result << '  '
          index += 2
          state = :code
        else
          result << (char == "\n" ? "\n" : ' ')
          index += 1
        end
      when :string
        result << char
        index += 1
        if char == '\\' && index < source.length
          result << source[index]
          index += 1
        elsif char == '"'
          state = :code
        end
      end
    end
    result
  end

  def visible_literals(path)
    source = without_comments(File.read(path))
    string = /"((?:\\.|[^"\\])*)"/
    patterns = if path.end_with?('.kt')
                 [
                   /\b(?:Text|Label|Button|Wakeve[A-Za-z0-9_]*(?:Text|Label|Button))\s*\(\s*(?:(?:text|title|label)\s*=\s*)?#{string}/m,
                   /\b(?:contentDescription|accessibilityLabel)\s*=\s*#{string}/m
                 ]
               else
                 [
                   /\b(?:Text|Label|Button|Wakeve[A-Za-z0-9_]*(?:Text|Label|Button))\s*\(\s*(?:(?:text|title|label)\s*:\s*)?#{string}/m,
                   /\.(?:accessibilityLabel|accessibilityHint)\s*\(\s*#{string}/m
                 ]
               end
    patterns.flat_map do |pattern|
      source.to_enum(:scan, pattern).map do
        match = Regexp.last_match
        literal_offset = match.begin(0) + match[0].index('"')
        [source[0...literal_offset].count("\n") + 1, match.captures.compact.last]
      end
    end.uniq
  end

  source_globs = ['composeApp/src/androidMain/**/*.kt', 'iosApp/src/**/*.swift']
  source_globs.flat_map { |glob| Dir.glob(File.join(root, glob)) }.sort.each do |path|
    next if path.include?('/Preview') || path.include?('/Tests/')

    relative = path.delete_prefix("#{root}/")
    visible_literals(path).each do |line, value|
      identity = "#{relative}:#{line}"
      next if allowlist.include?(identity)

      findings << "#{identity}: production literal"
      findings << "#{identity}: forbidden visible term" if value.match?(forbidden)
    end
  end
end

puts findings.uniq.sort
exit findings.empty? ? 0 : 1
