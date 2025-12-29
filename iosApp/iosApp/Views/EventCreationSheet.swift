import SwiftUI
import Shared

// MARK: - Event Creation Sheet (iOS Calendar Style with Liquid Glass)

/// Bottom sheet modal for creating new events with Liquid Glass design
/// Redesigned to match iOS Calendar app style with:
/// - Liquid Glass materials for cards (.regularMaterial)
/// - Continuous corner radius (12pt/16pt)
/// - 44pt minimum row heights
/// - iOS system colors (blue, green, orange)
struct EventCreationSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    
    // Form State
    @State private var eventTitle = ""
    @State private var eventDescription = ""
    @State private var location = ""
    @State private var isAllDay = false
    @State private var startDate = Date()
    @State private var endDate = Date().addingTimeInterval(3600)
    @State private var deadline = Date().addingTimeInterval(7 * 24 * 60 * 60) // 1 week
    
    // UI State
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showStartDatePicker = false
    @State private var showEndDatePicker = false
    @State private var showDeadlinePicker = false
    
    // Dependencies
    let userId: String
    let repository: EventRepositoryInterface
    let onEventCreated: (String) -> Void
    
    // MARK: - Colors
    
    private var backgroundColor: Color {
        colorScheme == .dark ? Color(uiColor: .systemGroupedBackground) : Color(uiColor: .systemGroupedBackground)
    }
    
    private var separatorColor: Color {
        colorScheme == .dark ? .iOSDarkSeparator : Color(uiColor: .separator)
    }
    
    private var secondaryLabelColor: Color {
        colorScheme == .dark ? .iOSSecondaryLabel : Color(uiColor: .secondaryLabel)
    }
    
    // MARK: - Body
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Basic Info Section
                    basicInfoSection
                    
                    // Date & Time Section
                    dateTimeSection
                    
                    // Voting Deadline Section
                    votingDeadlineSection
                    
                    // Bottom padding for scroll
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 16)
                .padding(.top, 20)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(backgroundColor.ignoresSafeArea())
            .navigationTitle("Nouvel événement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    closeButton
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    saveButton
                }
            }
            .alert("Erreur", isPresented: .constant(errorMessage != nil)) {
                Button("OK") { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.hidden)
        .interactiveDismissDisabled(isLoading)
        .accessibilityAddTraits(.isModal)
    }
    
    // MARK: - Close Button
    
    private var closeButton: some View {
        Button {
            hapticFeedback(.light)
            dismiss()
        } label: {
            Image(systemName: "xmark.circle.fill")
                .font(.system(size: 28))
                .symbolRenderingMode(.hierarchical)
                .foregroundStyle(secondaryLabelColor)
        }
        .accessibilityLabel("Fermer")
    }
    
    // MARK: - Save Button (Checkmark)
    
    private var saveButton: some View {
        Button {
            Task { await createEvent() }
        } label: {
            if isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
            } else {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 28))
                    .symbolRenderingMode(.hierarchical)
                    .foregroundStyle(canCreate ? .iOSSystemBlue : secondaryLabelColor)
            }
        }
        .disabled(!canCreate || isLoading)
        .accessibilityLabel(canCreate ? "Créer l'événement" : "Titre requis")
    }
    
    // MARK: - Basic Info Section
    
    private var basicInfoSection: some View {
        VStack(spacing: 0) {
            // Title Field
            FormRow(showSeparator: true) {
                TextField("Titre", text: $eventTitle)
                    .font(.body)
                    .accessibilityLabel("Titre de l'événement")
            }
            
            // Location Field
            FormRow(showSeparator: true) {
                HStack(spacing: 12) {
                    Image(systemName: "location.fill")
                        .foregroundStyle(secondaryLabelColor)
                        .frame(width: 20)
                    
                    TextField("Lieu ou appel vidéo", text: $location)
                        .font(.body)
                        .accessibilityLabel("Lieu de l'événement")
                }
            }
            
            // Description Field
            FormRow(showSeparator: false) {
                HStack(spacing: 12) {
                    Image(systemName: "text.alignleft")
                        .foregroundStyle(secondaryLabelColor)
                        .frame(width: 20)
                    
                    TextField("Notes", text: $eventDescription, axis: .vertical)
                        .font(.body)
                        .lineLimit(1...4)
                        .accessibilityLabel("Notes de l'événement")
                }
            }
        }
        .formCard()
    }
    
    // MARK: - Date & Time Section
    
    private var dateTimeSection: some View {
        VStack(spacing: 0) {
            // All Day Toggle
            FormRow(showSeparator: true) {
                Toggle(isOn: $isAllDay) {
                    Text("Toute la journée")
                        .font(.body)
                }
                .tint(.iOSSystemGreen)
            }
            
            // Start Date/Time
            FormRow(showSeparator: true) {
                HStack {
                    Text("Début")
                        .font(.body)
                    
                    Spacer()
                    
                    DateTimeButtons(
                        date: $startDate,
                        showDatePicker: $showStartDatePicker,
                        isAllDay: isAllDay
                    )
                }
            }
            
            // Start Date Picker (Expandable)
            if showStartDatePicker {
                DatePicker("", selection: $startDate, displayedComponents: isAllDay ? .date : [.date, .hourAndMinute])
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .onChange(of: startDate) { _, newValue in
                        // Ensure end date is after start date
                        if endDate < newValue {
                            endDate = newValue.addingTimeInterval(3600)
                        }
                    }
            }
            
            // End Date/Time
            FormRow(showSeparator: showEndDatePicker) {
                HStack {
                    Text("Fin")
                        .font(.body)
                    
                    Spacer()
                    
                    DateTimeButtons(
                        date: $endDate,
                        showDatePicker: $showEndDatePicker,
                        isAllDay: isAllDay
                    )
                }
            }
            
            // End Date Picker (Expandable)
            if showEndDatePicker {
                DatePicker("", selection: $endDate, in: startDate..., displayedComponents: isAllDay ? .date : [.date, .hourAndMinute])
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
            }
        }
        .formCard()
    }
    
    // MARK: - Voting Deadline Section
    
    private var votingDeadlineSection: some View {
        VStack(spacing: 0) {
            // Deadline Row
            FormRow(showSeparator: showDeadlinePicker) {
                HStack {
                    HStack(spacing: 12) {
                        Image(systemName: "clock.badge.exclamationmark.fill")
                            .foregroundStyle(Color.iOSSystemOrange)
                            .frame(width: 20)
                        
                        Text("Date limite de vote")
                            .font(.body)
                    }
                    
                    Spacer()
                    
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            showDeadlinePicker.toggle()
                            // Close other pickers
                            if showDeadlinePicker {
                                showStartDatePicker = false
                                showEndDatePicker = false
                            }
                        }
                    } label: {
                        Text(deadline, format: .dateTime.day().month().year())
                            .font(.body)
                            .foregroundStyle(Color.iOSSystemBlue)
                    }
                }
            }
            
            // Deadline Picker (Expandable)
            if showDeadlinePicker {
                DatePicker("", selection: $deadline, in: Date()..., displayedComponents: [.date, .hourAndMinute])
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
            }
        }
        .formCard()
    }
    
    // MARK: - Helpers
    
    private var canCreate: Bool {
        !eventTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    private func hapticFeedback(_ style: UIImpactFeedbackGenerator.FeedbackStyle) {
        let generator = UIImpactFeedbackGenerator(style: style)
        generator.impactOccurred()
    }
    
    // MARK: - Create Event Action
    
    private func createEvent() async {
        guard canCreate else { return }
        
        isLoading = true
        errorMessage = nil
        
        do {
            let formatter = ISO8601DateFormatter()
            let startString = formatter.string(from: startDate)
            let endString = formatter.string(from: endDate)
            let deadlineString = formatter.string(from: deadline)
            let now = formatter.string(from: Date())
            
            let timeSlot = TimeSlot(
                id: UUID().uuidString,
                start: startString,
                end: endString,
                timezone: TimeZone.current.identifier
            )
            
            let event = Event(
                id: UUID().uuidString,
                title: eventTitle.trimmingCharacters(in: .whitespacesAndNewlines),
                description: eventDescription.trimmingCharacters(in: .whitespacesAndNewlines),
                organizerId: userId,
                participants: [],
                proposedSlots: [timeSlot],
                deadline: deadlineString,
                status: EventStatus.draft,
                finalDate: nil,
                createdAt: now,
                updatedAt: now
            )
            
            let result = try await repository.createEvent(event: event)
            
            if let createdEvent = result as? Event {
                isLoading = false
                
                let generator = UINotificationFeedbackGenerator()
                generator.notificationOccurred(.success)
                
                UIAccessibility.post(notification: .announcement, argument: "Événement créé avec succès")
                
                onEventCreated(createdEvent.id)
                dismiss()
            } else {
                isLoading = false
                errorMessage = "Échec de la création de l'événement"
                
                let generator = UINotificationFeedbackGenerator()
                generator.notificationOccurred(.error)
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            
            let generator = UINotificationFeedbackGenerator()
            generator.notificationOccurred(.error)
        }
    }
}

// MARK: - Form Row Component

/// A single row in a grouped form card (iOS Calendar style)
/// - Height: 44pt minimum
/// - Horizontal padding: 16pt
/// - Optional separator
struct FormRow<Content: View>: View {
    let showSeparator: Bool
    let content: Content
    
    @Environment(\.colorScheme) private var colorScheme
    
    private var separatorColor: Color {
        colorScheme == .dark ? .iOSDarkSeparator : Color(uiColor: .separator)
    }
    
    init(showSeparator: Bool = true, @ViewBuilder content: () -> Content) {
        self.showSeparator = showSeparator
        self.content = content()
    }
    
    var body: some View {
        VStack(spacing: 0) {
            content
                .frame(minHeight: 44)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            
            if showSeparator {
                separatorColor
                    .frame(height: 0.5)
                    .padding(.leading, 16)
            }
        }
    }
}

// MARK: - Form Card Modifier

extension View {
    func formCard() -> some View {
        modifier(FormCardModifier())
    }
}

struct FormCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 3)
    }
}

// MARK: - Date Time Buttons

/// Date and time buttons styled like iOS Calendar with Liquid Glass
struct DateTimeButtons: View {
    @Binding var date: Date
    @Binding var showDatePicker: Bool
    let isAllDay: Bool
    
    var body: some View {
        HStack(spacing: 8) {
            // Date Button
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    showDatePicker.toggle()
                }
            } label: {
                Text(date, format: .dateTime.day().month().year())
                    .font(.body)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background {
                        if showDatePicker {
                            Color.blue.opacity(0.15)
                        } else {
                            Color.clear.background(.ultraThinMaterial)
                        }
                    }
                    .overlay(
                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                            .stroke(Color.blue.opacity(showDatePicker ? 0.5 : 0), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                    .foregroundStyle(Color.blue)
                    .shadow(color: .black.opacity(showDatePicker ? 0.08 : 0.03), radius: showDatePicker ? 4 : 2, x: 0, y: 2)
            }
            
            // Time Button (if not all day)
            if !isAllDay {
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        showDatePicker.toggle()
                    }
                } label: {
                    Text(date, format: .dateTime.hour().minute())
                        .font(.body)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background {
                            if showDatePicker {
                                Color.blue.opacity(0.15)
                            } else {
                                Color.clear.background(.ultraThinMaterial)
                            }
                        }
                        .overlay(
                            RoundedRectangle(cornerRadius: 6, style: .continuous)
                                .stroke(Color.blue.opacity(showDatePicker ? 0.5 : 0), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                        .foregroundStyle(Color.blue)
                        .shadow(color: .black.opacity(showDatePicker ? 0.08 : 0.03), radius: showDatePicker ? 4 : 2, x: 0, y: 2)
                }
            }
        }
    }
}

// MARK: - Quick Event Creation Sheet (Simplified with Liquid Glass)

/// A simpler version for quick event creation from any context
struct QuickEventCreationSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    
    @State private var eventTitle = ""
    @State private var isLoading = false
    
    let onCreateTapped: (String) -> Void
    
    private var backgroundColor: Color {
        colorScheme == .dark ? Color(uiColor: .systemGroupedBackground) : Color(uiColor: .systemGroupedBackground)
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                // Icon
                Image(systemName: "calendar.badge.plus")
                    .font(.system(size: 56))
                    .foregroundStyle(Color.iOSSystemBlue)
                    .padding(.top, 32)
                
                // Title
                Text("Créer un événement")
                    .font(.title2.weight(.bold))
                
                // Quick Title Input
                TextField("Nom de l'événement", text: $eventTitle)
                    .font(.title3)
                    .multilineTextAlignment(.center)
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(.regularMaterial)
                            .shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 3)
                    )
                    .padding(.horizontal, 32)
                    .submitLabel(.done)
                    .onSubmit {
                        if !eventTitle.isEmpty {
                            onCreateTapped(eventTitle)
                            dismiss()
                        }
                    }
                
                Text("Vous pourrez ajouter plus de détails ensuite")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                
                Spacer()
                
                // Create Button
                Button {
                    guard !eventTitle.isEmpty else { return }
                    
                    let generator = UIImpactFeedbackGenerator(style: .medium)
                    generator.impactOccurred()
                    
                    onCreateTapped(eventTitle)
                    dismiss()
                } label: {
                    Text("Continuer")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(eventTitle.isEmpty ? Color.gray.opacity(0.5) : .iOSSystemBlue)
                        )
                        .foregroundColor(.white)
                }
                .disabled(eventTitle.isEmpty)
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
            }
            .background(backgroundColor.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 28))
                            .symbolRenderingMode(.hierarchical)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
        .presentationCornerRadius(24)
    }
}

// MARK: - Preview

struct EventCreationSheet_Previews: PreviewProvider {
    static var previews: some View {
        // Dark Mode Preview
        Text("Background Content")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.black)
            .sheet(isPresented: .constant(true)) {
                EventCreationSheet(
                    userId: "user123",
                    repository: RepositoryProvider.shared.repository,
                    onEventCreated: { _ in }
                )
            }
            .preferredColorScheme(.dark)
            .previewDisplayName("Dark Mode")
        
        // Light Mode Preview
        Text("Background Content")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white)
            .sheet(isPresented: .constant(true)) {
                EventCreationSheet(
                    userId: "user123",
                    repository: RepositoryProvider.shared.repository,
                    onEventCreated: { _ in }
                )
            }
            .preferredColorScheme(.light)
            .previewDisplayName("Light Mode")
        
        // Quick Sheet Preview
        Text("Background Content")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.black)
            .sheet(isPresented: .constant(true)) {
                QuickEventCreationSheet(
                    onCreateTapped: { _ in }
                )
            }
            .preferredColorScheme(.dark)
            .previewDisplayName("Quick Creation")
    }
}
