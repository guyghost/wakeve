import AppIntents
import SwiftUI

extension View {
    @ViewBuilder
    func wakeveEventAppEntityIdentifier(_ eventId: String) -> some View {
        if #available(iOS 18.4, *) {
            appEntityIdentifier(EntityIdentifier(for: EventEntity.self, identifier: eventId))
        } else {
            self
        }
    }

    @ViewBuilder
    func wakevePollAppEntityIdentifier(_ pollId: String) -> some View {
        if #available(iOS 18.4, *) {
            appEntityIdentifier(EntityIdentifier(for: PollEntity.self, identifier: pollId))
        } else {
            self
        }
    }

    @ViewBuilder
    func wakeveGroupAppEntityIdentifier(_ groupId: String) -> some View {
        if #available(iOS 18.4, *) {
            appEntityIdentifier(EntityIdentifier(for: GroupEntity.self, identifier: groupId))
        } else {
            self
        }
    }

    @ViewBuilder
    func wakeveTransportAppEntityIdentifier(_ transportId: String) -> some View {
        if #available(iOS 18.4, *) {
            appEntityIdentifier(EntityIdentifier(for: TransportEntity.self, identifier: transportId))
        } else {
            self
        }
    }
}

#if DEBUG
struct WakeveAppIntentAnnotationTestSurface: View {
    let screen: WakeveIntentTestScreen
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            annotatedContent
                .navigationTitle(title)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Done", action: onDismiss)
                    }
                }
        }
    }

    @ViewBuilder
    private var annotatedContent: some View {
        switch screen.kind {
        case "poll":
            contentBody("PollDetailView", entityId: screen.entityId)
                .wakevePollAppEntityIdentifier(screen.entityId)
        case "group":
            contentBody("GroupDetailView", entityId: screen.entityId)
                .wakeveGroupAppEntityIdentifier(screen.entityId)
        case "transport":
            contentBody("TransportDetailView", entityId: screen.entityId)
                .wakeveTransportAppEntityIdentifier(screen.entityId)
        default:
            contentBody("EventDetailView", entityId: screen.entityId)
                .wakeveEventAppEntityIdentifier(screen.entityId)
        }
    }

    private var title: String {
        switch screen.kind {
        case "poll":
            return "PollDetailView"
        case "group":
            return "GroupDetailView"
        case "transport":
            return "TransportDetailView"
        default:
            return "EventDetailView"
        }
    }

    private func contentBody(_ heading: String, entityId: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(heading)
                .font(.title)
                .fontWeight(.semibold)
            Text(entityId)
                .font(.body)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding()
    }
}
#endif
