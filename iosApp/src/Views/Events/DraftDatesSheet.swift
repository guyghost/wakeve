import SwiftUI
import Shared

/// Lets the organizer of a DRAFT created with "date à décider avec le groupe" add the
/// possible dates the group will vote on, right before starting the poll.
struct DraftDatesSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    let eventId: String
    let repository: EventRepositoryInterface
    let onSaved: () -> Void

    @StateObject private var controller: EventDraftDatesController
    @State private var dates: [DraftDate] = []
    @State private var day = Calendar.current.startOfDay(for: Date().addingTimeInterval(7 * 24 * 3600))
    @State private var isAllDay = true
    @State private var time = Calendar.current.date(bySettingHour: 19, minute: 0, second: 0, of: Date()) ?? Date()
    @State private var errorMessage: String?

    init(eventId: String, repository: EventRepositoryInterface, onSaved: @escaping () -> Void) {
        self.eventId = eventId
        self.repository = repository
        self.onSaved = onSaved
        _controller = StateObject(wrappedValue: EventDraftDatesController(eventId: eventId, repository: repository))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                    Text(String(localized: "draft_dates.subtitle"))
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))

                    LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            DatePicker(
                                String(localized: "draft_dates.day"),
                                selection: $day,
                                in: Calendar.current.startOfDay(for: Date())...,
                                displayedComponents: .date
                            )
                            .datePickerStyle(.graphical)

                            Toggle(String(localized: "events.all_day"), isOn: $isAllDay)

                            if !isAllDay {
                                DatePicker(
                                    String(localized: "events.start"),
                                    selection: $time,
                                    displayedComponents: .hourAndMinute
                                )
                            }

                            WakeveActionButton(
                                String(localized: "draft_dates.add"),
                                systemImage: "plus",
                                variant: .secondary
                            ) {
                                addCurrentDate()
                            }
                            .accessibilityIdentifier("draftDatesAddAction")
                        }
                    }

                    if dates.isEmpty {
                        Text(String(localized: "draft_dates.empty"))
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    } else {
                        VStack(spacing: WakeveTheme.Spacing.xs) {
                            ForEach(dates) { date in
                                HStack {
                                    Image(systemName: "calendar")
                                        .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                                    Text(date.label)
                                        .font(WakeveTheme.Typography.bodySemibold)
                                        .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                                    Spacer()
                                    Button {
                                        dates.removeAll { $0.id == date.id }
                                    } label: {
                                        Image(systemName: "trash")
                                            .foregroundColor(WakeveTheme.ColorToken.destructive(for: colorScheme))
                                            .frame(width: 44, height: 44)
                                    }
                                    .accessibilityLabel(String(localized: "draft_dates.remove"))
                                }
                                .padding(.horizontal, WakeveTheme.Spacing.sm)
                                .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                            }
                        }
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.destructive(for: colorScheme))
                    }

                    WakeveActionButton(
                        String(localized: "draft_dates.save"),
                        systemImage: "checkmark",
                        variant: .primary,
                        isDisabled: dates.isEmpty,
                        isLoading: controller.isSaving
                    ) {
                        save()
                    }
                    .accessibilityIdentifier("draftDatesSaveAction")
                }
                .padding(WakeveTheme.Spacing.page)
            }
            .background(WakeveTheme.ColorToken.pageBackground(for: colorScheme).ignoresSafeArea())
            .navigationTitle(String(localized: "draft_dates.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
            }
        }
    }

    private func addCurrentDate() {
        let calendar = Calendar.current
        let start: Date
        if isAllDay {
            start = calendar.startOfDay(for: day)
        } else {
            let hm = calendar.dateComponents([.hour, .minute], from: time)
            start = calendar.date(bySettingHour: hm.hour ?? 19, minute: hm.minute ?? 0, second: 0, of: day) ?? day
        }
        let candidate = DraftDate(start: start, isAllDay: isAllDay)
        guard !dates.contains(where: { $0.start == candidate.start && $0.isAllDay == candidate.isAllDay }) else { return }
        WakeveHaptics.selection()
        dates.append(candidate)
        dates.sort { $0.start < $1.start }
        errorMessage = nil
    }

    private func save() {
        let formatter = ISO8601DateFormatter()
        controller.save(slots: dates.map { $0.slotInput(using: formatter) }) { outcome in
            switch outcome {
            case .saved:
                WakeveHaptics.success()
                onSaved()
                dismiss()
            case .failed(let message):
                WakeveHaptics.warning()
                errorMessage = message
            }
        }
    }
}

private struct DraftDate: Identifiable, Equatable {
    let id = UUID()
    let start: Date
    let isAllDay: Bool

    var label: String {
        let formatter = DateFormatter()
        formatter.locale = .autoupdatingCurrent
        formatter.dateStyle = .full
        formatter.timeStyle = isAllDay ? .none : .short
        let text = formatter.string(from: start)
        return isAllDay ? "\(text) · \(String(localized: "events.all_day"))" : text
    }

    func slotInput(using formatter: ISO8601DateFormatter) -> EventTimeSlotInput {
        let calendar = Calendar.current
        let end = isAllDay
            ? calendar.date(byAdding: .day, value: 1, to: start)
            : calendar.date(byAdding: .hour, value: 2, to: start)
        return EventTimeSlotInput(
            start: formatter.string(from: start),
            end: end.map { formatter.string(from: $0) },
            timeOfDay: isAllDay ? .allDay : .specific
        )
    }
}
