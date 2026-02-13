import SwiftUI

/// Bottom sheet for editing event description and organizer profile
/// Matches the design from description.png
struct EventInfoSheet: View {
    @Binding var description: String
    @Binding var organizerName: String
    @Binding var organizerPhotoUrl: String?
    var onDismiss: () -> Void
    var onConfirm: () -> Void
    
    @State private var tempDescription: String = ""
    @State private var tempName: String = ""
    @FocusState private var descriptionFocused: Bool
    
    private let characterLimit = 1000
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // Description Section
                    descriptionSection
                    
                    // Profile Section
                    profileSection
                    
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }
            .navigationTitle("Informations sur l'évènement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        description = tempDescription
                        organizerName = tempName
                        onConfirm()
                    } label: {
                        Image(systemName: "checkmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 32, height: 32)
                            .background(
                                Circle()
                                    .fill(Color.blue)
                            )
                    }
                    .accessibilityLabel("Confirmer")
                }
            }
            .background(Color(.systemBackground))
        }
        .onAppear {
            tempDescription = description
            tempName = organizerName
        }
    }
    
    // MARK: - Description Section
    
    private var descriptionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Description de l'évènement")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
            
            // Text Editor
            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.secondarySystemBackground))
                
                if tempDescription.isEmpty && !descriptionFocused {
                    Text("Informez vos invités de l'organisation de votre évènement.")
                        .font(.body)
                        .foregroundColor(.secondary.opacity(0.6))
                        .padding(16)
                }
                
                TextEditor(text: $tempDescription)
                    .font(.body)
                    .foregroundColor(.primary)
                    .padding(12)
                    .focused($descriptionFocused)
                    .scrollContentBackground(.hidden)
            }
            .frame(minHeight: 200)
            
            // Character limit
            Text("Limite de caractères : \(tempDescription.count)/\(characterLimit)")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
    
    // MARK: - Profile Section
    
    private var profileSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Nom et photo de profil")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
            
            // Profile Card
            Button {
                // Show photo picker / name editor
            } label: {
                HStack(spacing: 12) {
                    // Profile Photo
                    ZStack {
                        Circle()
                            .fill(Color(hex: "FF6B35"))
                            .frame(width: 44, height: 44)
                        
                        if let photoUrl = organizerPhotoUrl {
                            // AsyncImage would go here
                            Text(String(tempName.prefix(1)).uppercased())
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                        } else {
                            Text(String(tempName.prefix(1)).uppercased())
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                    
                    Text(tempName)
                        .font(.body.weight(.medium))
                        .foregroundColor(.primary)
                    
                    Spacer()
                    
                    Text("Modifier")
                        .font(.body.weight(.medium))
                        .foregroundColor(.primary)
                }
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color(.secondarySystemBackground))
                )
            }
            
            // Help text
            Text("Il s'agit de la photo et du nom qui seront visibles pour les invités de cet évènement. Vous pouvez utiliser une photo de profil et un nom différents pour chaque évènement.")
                .font(.caption)
                .foregroundColor(.secondary)
                .lineSpacing(4)
        }
    }
}

// MARK: - Preview

struct EventInfoSheet_Previews: PreviewProvider {
    static var previews: some View {
        EventInfoSheet(
            description: .constant(""),
            organizerName: .constant("Guy MANDINA NZEZA"),
            organizerPhotoUrl: .constant(nil),
            onDismiss: {},
            onConfirm: {}
        )
    }
}
