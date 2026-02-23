import SwiftUI
import CoreImage.CIFilterBuiltins
#if canImport(UIKit)
import UIKit
#endif

/// Bottom sheet for sharing event invitation links.
/// Features: copy link, share via UIActivityViewController, QR code display.
struct InvitationShareSheet: View {
    let eventId: String
    let eventTitle: String
    let invitationCode: String
    let onDismiss: () -> Void

    @State private var showCopiedFeedback = false
    @State private var qrImage: UIImage?

    /// The shareable invitation URL
    private var inviteUrl: String {
        "https://wakeve.app/invite/\(invitationCode)"
    }

    /// The deep link URL
    private var deepLinkUrl: String {
        "wakeve://invite/\(invitationCode)"
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Event info header
                    VStack(spacing: 8) {
                        Image(systemName: "link.badge.plus")
                            .font(.system(size: 48))
                            .foregroundColor(.wakevePrimary)

                        Text(String(localized: "invitation.title"))
                            .font(.title2.weight(.bold))
                            .foregroundColor(.primary)

                        Text(eventTitle)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 8)

                    // QR Code Section
                    VStack(spacing: 12) {
                        Text(String(localized: "invitation.qr_code"))
                            .font(.headline)
                            .foregroundColor(.primary)

                        if let qrImage = qrImage {
                            Image(uiImage: qrImage)
                                .interpolation(.none)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 200, height: 200)
                                .background(Color.white)
                                .cornerRadius(12)
                                .shadow(color: .black.opacity(0.1), radius: 8)
                        } else {
                            ProgressView()
                                .frame(width: 200, height: 200)
                        }

                        Text(String(localized: "invitation.scan_to_join"))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(16)

                    // Link Section
                    VStack(spacing: 12) {
                        Text(String(localized: "invitation.link"))
                            .font(.headline)
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        HStack {
                            Text(inviteUrl)
                                .font(.system(.footnote, design: .monospaced))
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                                .truncationMode(.middle)

                            Spacer()

                            Button(action: copyLink) {
                                HStack(spacing: 4) {
                                    Image(systemName: showCopiedFeedback ? "checkmark" : "doc.on.doc")
                                        .font(.system(size: 14, weight: .medium))
                                    Text(showCopiedFeedback ? String(localized: "invitation.copied") : String(localized: "invitation.copy"))
                                        .font(.caption.weight(.medium))
                                }
                                .foregroundColor(showCopiedFeedback ? .green : .wakevePrimary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(
                                    (showCopiedFeedback ? Color.green : Color.wakevePrimary)
                                        .opacity(0.1)
                                )
                                .cornerRadius(8)
                            }
                        }
                        .padding(12)
                        .background(Color(.tertiarySystemGroupedBackground))
                        .cornerRadius(10)
                    }
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(16)

                    // Share Button
                    Button(action: shareInvitation) {
                        HStack(spacing: 12) {
                            Image(systemName: "square.and.arrow.up")
                                .font(.system(size: 18, weight: .semibold))

                            Text(String(localized: "invitation.share"))
                                .font(.headline)
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color.wakevePrimary,
                                    Color.wakeveAccent
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .cornerRadius(14)
                    }

                    Spacer()
                }
                .padding(.horizontal, 20)
            }
            .background(Color(.systemGroupedBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onDismiss) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .onAppear {
            generateQRCode()
        }
    }

    // MARK: - Actions

    /// Copy invitation link to clipboard
    private func copyLink() {
        #if canImport(UIKit)
        UIPasteboard.general.string = inviteUrl
        #endif

        withAnimation(.easeInOut(duration: 0.2)) {
            showCopiedFeedback = true
        }

        // Haptic feedback
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)

        // Reset after delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation {
                showCopiedFeedback = false
            }
        }
    }

    /// Share invitation via UIActivityViewController
    private func shareInvitation() {
        let shareText = String(format: String(localized: "invitation.share_text"), eventTitle, inviteUrl)
        var items: [Any] = [shareText]

        // Include QR code image if available
        if let qrImage = qrImage {
            items.append(qrImage)
        }

        #if canImport(UIKit)
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else {
            return
        }

        let activityVC = UIActivityViewController(
            activityItems: items,
            applicationActivities: nil
        )

        // iPad support
        if let popover = activityVC.popoverPresentationController {
            popover.sourceView = rootVC.view
            popover.sourceRect = CGRect(
                x: rootVC.view.bounds.midX,
                y: rootVC.view.bounds.midY,
                width: 0,
                height: 0
            )
        }

        rootVC.present(activityVC, animated: true)
        #endif
    }

    // MARK: - QR Code Generation

    /// Generate QR code image from invitation URL using CoreImage
    private func generateQRCode() {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()

        guard let data = inviteUrl.data(using: .utf8) else { return }
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")

        guard let outputImage = filter.outputImage else { return }

        // Scale up the QR code for better quality
        let scale = 10.0
        let transform = CGAffineTransform(scaleX: scale, y: scale)
        let scaledImage = outputImage.transformed(by: transform)

        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else { return }

        qrImage = UIImage(cgImage: cgImage)
    }
}

// MARK: - Preview

#if DEBUG
struct InvitationShareSheet_Previews: PreviewProvider {
    static var previews: some View {
        InvitationShareSheet(
            eventId: "event-1",
            eventTitle: "Weekend entre amis",
            invitationCode: "ABC12345",
            onDismiss: {}
        )
    }
}
#endif
