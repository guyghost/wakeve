import SwiftUI

/**
 * Settings screen with language selector.
 * Uses Liquid Glass design system following iOS HIG.
 */
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedLocale: AppLocale = LocalizationService().getCurrentLocale()
    @State private var showNotificationPreferences = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        // Notification Preferences Section
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Notifications")
                                .font(.title2.weight(.semibold))
                                .foregroundColor(.primary)

                            Text("Gerez vos preferences de notifications")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 20)
                        .padding(.top, 8)

                        LiquidGlassCard(cornerRadius: 20, padding: 16) {
                            Button(action: { showNotificationPreferences = true }) {
                                HStack(spacing: 14) {
                                    Image(systemName: "bell.badge.fill")
                                        .font(.system(size: 20))
                                        .foregroundColor(.accentColor)
                                        .frame(width: 28)

                                    Text("Preferences de notifications")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(.primary)

                                    Spacer()

                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundColor(.secondary)
                                }
                                .padding(.vertical, 4)
                            }
                        }
                        .padding(.horizontal, 16)

                        // Language Section Header
                        VStack(alignment: .leading, spacing: 8) {
                            Text(NSLocalizedString("language_title", comment: "Language settings header"))
                                .font(.title2.weight(.semibold))
                                .foregroundColor(.primary)

                            Text(NSLocalizedString("language_description", comment: "Language settings description"))
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
                        
                        // Language Options Container
                        LiquidGlassCard(cornerRadius: 20, padding: 16) {
                            VStack(spacing: 0) {
                                // Language items with dividers
                                ForEach(Array(AppLocale.allCases.enumerated()), id: \.element) { index, locale in
                                    LanguageListItem(
                                        locale: locale,
                                        isSelected: locale == selectedLocale,
                                        onTap: {
                                            selectedLocale = locale
                                            LocalizationService().setLocale(locale)
                                        }
                                    )
                                    
                                    // Add divider between items (except last)
                                    if index < AppLocale.allCases.count - 1 {
                                        LiquidGlassDivider(style: .subtle)
                                            .padding(.leading, 52)
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                        
                        Spacer(minLength: 20)
                    }
                    .padding(.bottom, 20)
                }
            }
            .navigationTitle(NSLocalizedString("settings_title", comment: "Settings screen title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.accentColor)
                            .accessibilityLabel(NSLocalizedString("back", comment: "Back button"))
                    }
                }
            }
            .sheet(isPresented: $showNotificationPreferences) {
                NotificationPreferencesView(
                    onDismiss: { showNotificationPreferences = false }
                )
            }
        }
    }
}

/**
 * Language selection list item with Liquid Glass styling.
 * Replaces the custom LanguageOption view with LiquidGlassListItem.
 */
struct LanguageListItem: View {
    let locale: AppLocale
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        LiquidGlassListItem(
            title: locale.displayName,
            subtitle: locale.nativeName,
            icon: iconName(for: locale),
            iconColor: iconColor,
            style: .default
        ) {
            if isSelected {
                LiquidGlassBadge(
                    text: NSLocalizedString("selected", comment: "Selected badge"),
                    icon: "checkmark.circle.fill",
                    style: .success
                )
            }
        }
        .onTapGesture(perform: onTap)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(locale.displayName), \(locale.nativeName)")
        .accessibilityHint(isSelected ? NSLocalizedString("currently_selected", comment: "Currently selected language") : NSLocalizedString("tap_to_select", comment: "Tap to select this language"))
    }
    
    private var iconColor: Color {
        // Use accent color for all language icons to maintain visual consistency
        return Color.accentColor
    }
    
    private func iconName(for locale: AppLocale) -> String {
        switch locale {
        case .english:
            return "flag.fill"
        case .french:
            return "flag.fill"
        case .spanish:
            return "flag.fill"
        }
    }
}

#Preview {
    SettingsView()
}
