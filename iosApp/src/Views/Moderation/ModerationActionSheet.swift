import SwiftUI

struct ModerationActionSheet: View {
    let target: ModerationActionTarget
    var service: ModerationService = ModerationService()

    @Environment(\.dismiss) private var dismiss
    @State private var selectedReason: ModerationReportReason = .harassment
    @State private var details: String = ""
    @State private var isSubmitting = false
    @State private var statusMessage: String?
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(target.displayName)
                    Picker(String(localized: "moderation.reason"), selection: $selectedReason) {
                        ForEach(ModerationReportReason.allCases) { reason in
                            Text(reason.displayName).tag(reason)
                        }
                    }
                    TextField(String(localized: "moderation.details_placeholder"), text: $details, axis: .vertical)
                        .lineLimit(3...6)
                } header: {
                    Text(String(localized: "moderation.report_title"))
                } footer: {
                    Text(String(localized: "moderation.report_help"))
                }

                Section {
                    Button {
                        submitReport()
                    } label: {
                        Label(String(localized: "moderation.report_content"), systemImage: "exclamationmark.bubble")
                    }
                    .disabled(isSubmitting)

                    if target.allowsBlock, let authorId = target.authorId {
                        Button(role: .destructive) {
                            blockUser(authorId)
                        } label: {
                            Label(String(localized: "moderation.block_user"), systemImage: "person.crop.circle.badge.xmark")
                        }
                        .disabled(isSubmitting)

                        Button {
                            unblockUser(authorId)
                        } label: {
                            Label(String(localized: "moderation.unblock_user"), systemImage: "person.crop.circle.badge.checkmark")
                        }
                        .disabled(isSubmitting)
                    }
                }

                Section {
                    Link(
                        String(localized: "moderation.contact_support"),
                        destination: URL(string: "mailto:support@wakeve.app?subject=Wakeve%20abuse%20report")!
                    )
                    Link(
                        String(localized: "moderation.privacy_help"),
                        destination: URL(string: "https://wakeve.app/support")!
                    )
                } footer: {
                    Text(String(localized: "moderation.response_process"))
                }

                if let statusMessage {
                    Section {
                        Text(statusMessage)
                            .foregroundStyle(.green)
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle(String(localized: "moderation.title"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) {
                        dismiss()
                    }
                }
            }
        }
    }

    private func submitReport() {
        isSubmitting = true
        errorMessage = nil
        Task {
            do {
                _ = try await service.report(target: target, reason: selectedReason, details: details)
                statusMessage = String(localized: "moderation.report_submitted")
            } catch {
                errorMessage = String(localized: "moderation.submit_error")
            }
            isSubmitting = false
        }
    }

    private func blockUser(_ userId: String) {
        isSubmitting = true
        errorMessage = nil
        Task {
            do {
                _ = try await service.blockUser(userId: userId, eventId: target.eventId, reason: selectedReason)
                statusMessage = String(localized: "moderation.user_blocked")
            } catch {
                errorMessage = String(localized: "moderation.submit_error")
            }
            isSubmitting = false
        }
    }

    private func unblockUser(_ userId: String) {
        isSubmitting = true
        errorMessage = nil
        Task {
            do {
                try await service.unblockUser(userId: userId, eventId: target.eventId)
                statusMessage = String(localized: "moderation.user_unblocked")
            } catch {
                errorMessage = String(localized: "moderation.submit_error")
            }
            isSubmitting = false
        }
    }
}

struct ModerationStatusBadge: View {
    let status: String

    var body: some View {
        if status == "PENDING_REVIEW" || status == "REJECTED" || status == "HIDDEN" {
            Label(labelText, systemImage: iconName)
                .font(.caption.weight(.semibold))
                .foregroundColor(.secondary)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(.thinMaterial, in: Capsule())
                .accessibilityIdentifier("moderationStatusBadge")
        }
    }

    private var labelText: String {
        switch status {
        case "PENDING_REVIEW": return String(localized: "moderation.pending_review")
        case "REJECTED": return String(localized: "moderation.rejected")
        case "HIDDEN": return String(localized: "moderation.hidden")
        default: return ""
        }
    }

    private var iconName: String {
        switch status {
        case "PENDING_REVIEW": return "clock.badge.exclamationmark"
        case "REJECTED": return "xmark.octagon"
        case "HIDDEN": return "eye.slash"
        default: return "shield"
        }
    }
}

#if DEBUG
private let moderationPreviewTarget = ModerationActionTarget(
    type: .comment,
    targetId: "comment-preview",
    eventId: "event-preview",
    authorId: "user-preview",
    displayName: "Commentaire de Camille",
    allowsBlock: true
)

#Preview("Moderation Action - Light") {
    ModerationActionSheet(target: moderationPreviewTarget)
        .preferredColorScheme(.light)
}

#Preview("Moderation Action - Dark") {
    ModerationActionSheet(target: moderationPreviewTarget)
        .preferredColorScheme(.dark)
}
#endif
