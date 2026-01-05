import SwiftUI

/**
 * Settings screen with language selector.
 * Uses Liquid Glass design system following iOS HIG.
 */
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedLocale: AppLocale = LocalizationService().getCurrentLocale()

    var body: some View {
        NavigationView {
            List {
                Section(header: Text(NSLocalizedString("language_title", comment: "Language settings header"))) {
                    Text(NSLocalizedString("language_description", comment: "Language settings description"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Section {
                    ForEach(AppLocale.allCases, id: \.self) { locale in
                        LanguageOption(
                            locale: locale,
                            isSelected: locale == selectedLocale
                        ) {
                            selectedLocale = locale
                            LocalizationService().setLocale(locale)
                        }
                    }
                }
            }
            .navigationTitle(NSLocalizedString("settings_title", comment: "Settings screen title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                            .accessibilityLabel(NSLocalizedString("back", comment: "Back button"))
                    }
                }
            }
        }
    }
}

struct LanguageOption: View {
    let locale: AppLocale
    let isSelected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(locale.displayName)
                        .font(.body)
                    Text(locale.nativeName)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color.accentColor)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    SettingsView()
}
