import SwiftUI
import Shared

/// Modern event creation view inspired by Apple Invites
/// Features: Clean white design, card-based layout, minimal inputs
struct ModernEventCreationView: View {
    @State private var eventTitle = ""
    @State private var eventDescription = ""
    @State private var timeSlots: [TimeSlot] = []
    @State private var deadline = Date().addingTimeInterval(7 * 24 * 60 * 60) // 1 week from now
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false

    let userId: String
    let repository: EventRepository
    let onEventCreated: (String) -> Void
    let onBack: () -> Void

    var body: some View {
        ZStack {
            // Clean background
            Color(.systemGroupedBackground)
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    // Header Section
                    VStack(spacing: 16) {
                        HStack {
                            Button(action: onBack) {
                                Image(systemName: "xmark")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(.secondary)
                                    .frame(width: 36, height: 36)
                                    .background(Color(.tertiarySystemFill))
                                    .clipShape(Circle())
                            }

                            Spacer()

                            Button {
                                Task {
                                    await createEvent()
                                }
                            } label: {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle())
                                } else {
                                    Text("Create")
                                        .font(.system(size: 17, weight: .semibold))
                                        .foregroundColor(canCreate ? .blue : .secondary)
                                }
                            }
                            .disabled(!canCreate || isLoading)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 60)

                        VStack(spacing: 4) {
                            Text("New Event")
                                .font(.system(size: 34, weight: .bold))
                                .foregroundColor(.primary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 8)
                    }

                    // Form Cards
                    VStack(spacing: 16) {
                        // Basic Info Card
                        VStack(spacing: 20) {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Title")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(.secondary)
                                    .textCase(.uppercase)

                                TextField("Event name", text: $eventTitle)
                                    .font(.system(size: 17))
                                    .textFieldStyle(.plain)
                                    .padding(16)
                                    .background(Color(.tertiarySystemFill))
                                    .cornerRadius(12)
                            }

                            VStack(alignment: .leading, spacing: 8) {
                                Text("Description")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(.secondary)
                                    .textCase(.uppercase)

                                TextEditor(text: $eventDescription)
                                    .frame(height: 100)
                                    .font(.system(size: 17))
                                    .padding(12)
                                    .background(Color(.tertiarySystemFill))
                                    .cornerRadius(12)
                                    .scrollContentBackground(.hidden)
                            }

                            VStack(alignment: .leading, spacing: 8) {
                                Text("Voting Deadline")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(.secondary)
                                    .textCase(.uppercase)

                                DatePicker(
                                    "Select deadline",
                                    selection: $deadline,
                                    in: Date()...,
                                    displayedComponents: [.date, .hourAndMinute]
                                )
                                .datePickerStyle(.compact)
                                .labelsHidden()
                                .padding(12)
                                .background(Color(.tertiarySystemFill))
                                .cornerRadius(12)
                            }
                        }
                        .padding(20)
                        .background(Color(.systemBackground))
                        .cornerRadius(16)

                        // Time Slots Card
                        VStack(spacing: 16) {
                            HStack {
                                Text("Time Options")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundColor(.primary)

                                Spacer()

                                Button(action: addTimeSlot) {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.system(size: 28))
                                        .foregroundColor(.blue)
                                }
                            }

                            if timeSlots.isEmpty {
                                VStack(spacing: 16) {
                                    Image(systemName: "calendar.badge.clock")
                                        .font(.system(size: 48))
                                        .foregroundColor(.secondary.opacity(0.5))
                                        .padding(.top, 20)

                                    Text("No time slots yet")
                                        .font(.system(size: 17, weight: .medium))
                                        .foregroundColor(.secondary)

                                    Text("Add time options for participants to vote on")
                                        .font(.system(size: 15))
                                        .foregroundColor(.secondary)
                                        .multilineTextAlignment(.center)
                                        .padding(.horizontal, 40)
                                        .padding(.bottom, 20)
                                }
                            } else {
                                VStack(spacing: 12) {
                                    ForEach(timeSlots.indices, id: \.self) { index in
                                        ModernTimeSlotRow(
                                            timeSlot: $timeSlots[index],
                                            onDelete: { removeTimeSlot(at: index) }
                                        )
                                    }
                                }
                            }
                        }
                        .padding(20)
                        .background(Color(.systemBackground))
                        .cornerRadius(16)

                        Spacer()
                            .frame(height: 40)
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }

    private var canCreate: Bool {
        !eventTitle.isEmpty && !timeSlots.isEmpty
    }

    private func addTimeSlot() {
        let newSlot = TimeSlot(
            id: UUID().uuidString,
            start: ISO8601DateFormatter().string(from: Date()),
            end: ISO8601DateFormatter().string(from: Date().addingTimeInterval(2 * 60 * 60)),
            timezone: TimeZone.current.identifier
        )
        timeSlots.append(newSlot)
    }

    private func removeTimeSlot(at index: Int) {
        timeSlots.remove(at: index)
    }

    private func createEvent() async {
        guard canCreate else { return }

        isLoading = true

        do {
            let now = ISO8601DateFormatter().string(from: Date())
            let event = Event(
                id: UUID().uuidString,
                title: eventTitle,
                description: eventDescription,
                organizerId: userId,
                participants: [],
                proposedSlots: timeSlots,
                deadline: ISO8601DateFormatter().string(from: deadline),
                status: EventStatus.draft,
                finalDate: nil,
                createdAt: now,
                updatedAt: now
            )

            let result = try await repository.createEvent(event: event)

            if let createdEvent = result as? Event {
                isLoading = false
                onEventCreated(createdEvent.id)
            } else {
                isLoading = false
                errorMessage = "Failed to create event"
                showError = true
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}

// MARK: - Modern Time Slot Row

struct ModernTimeSlotRow: View {
    @Binding var timeSlot: TimeSlot
    let onDelete: () -> Void

    @State private var startDate = Date()
    @State private var endDate = Date().addingTimeInterval(2 * 60 * 60)

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 12) {
                    // Start Time
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Start")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(.secondary)
                            .textCase(.uppercase)

                        DatePicker("Start", selection: $startDate, displayedComponents: [.date, .hourAndMinute])
                            .datePickerStyle(.compact)
                            .labelsHidden()
                    }

                    // End Time
                    VStack(alignment: .leading, spacing: 4) {
                        Text("End")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(.secondary)
                            .textCase(.uppercase)

                        DatePicker("End", selection: $endDate, displayedComponents: [.date, .hourAndMinute])
                            .datePickerStyle(.compact)
                            .labelsHidden()
                    }
                }

                Spacer()

                Button(action: onDelete) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.secondary.opacity(0.5))
                }
            }
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
        .onAppear {
            if let start = ISO8601DateFormatter().date(from: timeSlot.start) {
                startDate = start
            }
            if let end = ISO8601DateFormatter().date(from: timeSlot.end) {
                endDate = end
            }
        }
        .onChange(of: startDate) { _, newValue in
            timeSlot = TimeSlot(
                id: timeSlot.id,
                start: ISO8601DateFormatter().string(from: newValue),
                end: timeSlot.end,
                timezone: timeSlot.timezone
            )
        }
        .onChange(of: endDate) { _, newValue in
            timeSlot = TimeSlot(
                id: timeSlot.id,
                start: timeSlot.start,
                end: ISO8601DateFormatter().string(from: newValue),
                timezone: timeSlot.timezone
            )
        }
    }
}
