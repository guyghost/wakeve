import SwiftUI
import Shared
import MapKit

// MARK: - Create Event Sheet

/// Full-screen bottom sheet for creating an event with immersive gradient design
struct CreateEventSheet: View {
    let userId: String
    let userName: String?
    
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = CreateEventViewModel()

    @State private var title = ""
    @State private var description = ""
    @State private var selectedDate: Date?
    @State private var proposedSlotDrafts: [EventSlotDraft] = []
    @State private var selectedLocation: String?
    @State private var selectedBackground: EventBackground = .gradient
    @State private var showingBackgroundPicker = false
    @State private var showingImageDescription = false
    @State private var backgroundDescription = ""

    // EventType selection
    // EventType stored as index (KotlinEnum not directly @State-compatible)
    @State private var selectedEventTypeIndex: Int = 0

    private let eventTypes: [Shared.EventType] = [
        Shared.EventType.other, Shared.EventType.birthday, Shared.EventType.wedding,
        Shared.EventType.teamBuilding, Shared.EventType.conference, Shared.EventType.workshop,
        Shared.EventType.party, Shared.EventType.sportsEvent, Shared.EventType.culturalEvent,
        Shared.EventType.familyGathering, Shared.EventType.outdoorActivity,
        Shared.EventType.foodTasting, Shared.EventType.techMeetup, Shared.EventType.wellnessEvent,
        Shared.EventType.creativeWorkshop
    ]

    var selectedEventType: Shared.EventType { eventTypes[selectedEventTypeIndex] }
    @State private var showingEventTypePicker = false
    @State private var expectedParticipants: Int? = nil
    @State private var planningMode: EventPlanningMode = .timeSlotPoll

    // Date picker sheet state
    @State private var showingDatePicker = false
    @State private var isAllDay = false
    @State private var startDate = Date()
    @State private var startTime = Date()
    @State private var hasEndTime = false
    @State private var endTime = Date().addingTimeInterval(3600) // +1 hour by default
    @State private var editingSlotID: UUID?
    
    // Event info sheet state
    @State private var showingEventInfoSheet = false
    
    // Location sheet state
    @State private var showingLocationSheet = false

    // Preview state
    @State private var showingPreview = false
    @State private var showValidationError = false
    @State private var validationMessage: String? = nil
    @State private var currentStep: CreateEventStep = .name
    @State private var showNameAdvancedOptions = false
    @State private var showConfirmAdvancedOptions = false
    
    var onEventCreated: (Event) -> Void = { _ in }
    
    var body: some View {
        ZStack {
            WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                .ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                    createStepHeader
                    createStepProgress
                    createStepContent
                }
                .padding(.horizontal, WakeveTheme.Spacing.page)
                .padding(.top, 92)
                .padding(.bottom, 124)
            }
            
            // Date Picker Bottom Sheet Overlay
            if showingDatePicker {
                DateTimePickerPopup(
                    isAllDay: $isAllDay,
                    startDate: $startDate,
                    startTime: $startTime,
                    hasEndTime: $hasEndTime,
                    endTime: $endTime,
                    onSave: {
                        showingDatePicker = false
                        appendOrUpdateCurrentSlotDraft()
                    },
                    onCancel: {
                        editingSlotID = nil
                        showingDatePicker = false
                    }
                )
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(100)
            }
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            createToolbar
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            createBottomAction
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: showingDatePicker)
        .animation(WakeveTheme.Motion.standardSpring, value: currentStep)
        .sheet(isPresented: $showingBackgroundPicker) {
            BackgroundPickerSheet(selectedBackground: $selectedBackground)
                .presentationDetents([.large])
        }
        .alert(String(localized: "events.background.edit_description"), isPresented: $showingImageDescription) {
            TextField(String(localized: "events.background.description_placeholder"), text: $backgroundDescription)
            Button(String(localized: "common.cancel"), role: .cancel) {}
            Button(String(localized: "common.save")) {}
        }
        .sheet(isPresented: $showingEventInfoSheet) {
            EventInfoSheet(
                description: $description,
                organizerName: .constant(userName ?? String(localized: "leaderboard.you")),
                organizerPhotoUrl: .constant(nil),
                onDismiss: {
                    showingEventInfoSheet = false
                },
                onConfirm: {
                    showingEventInfoSheet = false
                }
            )
        }
        .sheet(isPresented: $showingLocationSheet) {
            LocationSelectionSheet(
                onDismiss: {
                    showingLocationSheet = false
                },
                onConfirm: { location in
                    selectedLocation = location.name
                    showingLocationSheet = false
                }
            )
        }
        .sheet(isPresented: $showingEventTypePicker) {
            EventTypePickerSheet(
                selectedIndex: $selectedEventTypeIndex,
                types: eventTypes
            )
            .presentationDetents([.medium, .large])
        }
        .fullScreenCover(isPresented: $showingPreview) {
            EventPreviewSheet(
                title: title,
                description: description,
                userName: userName,
                proposedSlots: proposedSlotDrafts,
                selectedDate: selectedDate,
                selectedLocation: selectedLocation,
                isAllDay: isAllDay,
                startDate: startDate,
                startTime: startTime,
                hasEndTime: hasEndTime,
                endTime: endTime,
                eventType: selectedEventType,
                expectedParticipants: expectedParticipants,
                planningMode: planningMode,
                matrixScenarioCount: scenarioMatrixPreviewCount,
                onNext: {
                    showingPreview = false
                    createEvent()
                }
            )
        }
    }

    // MARK: - Gradient Background

    private var createToolbar: some View {
        LiquidGlassToolbar(title: "Nouvel événement", subtitle: currentStep.title) {
            WakeveCircleButton(
                systemImage: "xmark",
                accessibilityLabel: String(localized: "common.close"),
                variant: .glass,
                size: 40
            ) {
                dismiss()
            }
        } trailing: {
            Button {
                openPreviewIfValid()
            } label: {
                Image(systemName: "eye.fill")
                    .font(.body.weight(.bold))
                    .foregroundColor(canCreate ? primaryTextColor : disabledControlForeground)
                    .frame(width: 40, height: 40)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())
            }
            .disabled(!canCreate)
            .accessibilityLabel(String(localized: "events.preview"))
        }
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .padding(.top, WakeveTheme.Spacing.sm)
        .padding(.bottom, WakeveTheme.Spacing.xs)
        .background(
            LinearGradient(
                colors: [
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme),
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
    }

    private var createStepHeader: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
            Text(currentStep.eyebrow)
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                .textCase(.uppercase)

            Text(currentStep.question)
                .font(WakeveTheme.Typography.largeTitle)
                .foregroundColor(primaryTextColor)
                .lineLimit(3)
                .minimumScaleFactor(0.74)

            Text(currentStep.subtitle)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(secondaryTextColor)
                .lineLimit(3)
        }
    }

    private var createStepProgress: some View {
        LiquidGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                HStack {
                    Text("Étape \(currentStep.index + 1) / \(CreateEventStep.allCases.count)")
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(secondaryTextColor)
                    Spacer()
                    Text(currentStep.title)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(primaryTextColor)
                }

                GeometryReader { proxy in
                    Capsule()
                        .fill(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                        .overlay(alignment: .leading) {
                            Capsule()
                                .fill(WakeveTheme.ColorToken.progress(for: colorScheme))
                                .frame(width: proxy.size.width * currentStep.progress)
                        }
                }
                .frame(height: 8)
            }
        }
    }

    @ViewBuilder
    private var createStepContent: some View {
        switch currentStep {
        case .name:
            nameStep
        case .date:
            dateStep
        case .confirm:
            confirmStep
        }
    }

    private var nameStep: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                TextField(String(localized: "events.title_placeholder"), text: $title)
                    .font(WakeveTheme.Typography.title2)
                    .foregroundColor(primaryTextColor)
                    .textFieldStyle(.plain)
                    .padding(WakeveTheme.Spacing.md)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))

                TextField("Description courte", text: $description, axis: .vertical)
                    .font(WakeveTheme.Typography.body)
                    .foregroundColor(primaryTextColor)
                    .lineLimit(3...5)
                    .padding(WakeveTheme.Spacing.md)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))

                DisclosureGroup(isExpanded: $showNameAdvancedOptions) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                        smartEventDraftCard

                        Button(action: { showingEventTypePicker = true }) {
                            CreateEventChoiceRow(
                                systemImage: "sparkles",
                                title: String(localized: "events.type"),
                                value: selectedEventType == Shared.EventType.other ? String(localized: "events.type.choose") : eventTypeDisplayName(selectedEventType)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.top, WakeveTheme.Spacing.sm)
                } label: {
                    Label(String(localized: "events.advanced_options"), systemImage: "slider.horizontal.3")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(primaryTextColor)
                }
                .tint(WakeveTheme.ColorToken.accent(for: colorScheme))
            }
        }
    }

    private var smartEventDraftCard: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
            HStack {
                Label("Créer avec une phrase", systemImage: "sparkles")
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(primaryTextColor)

                Spacer()

                Text("Suggestion")
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                    .padding(.horizontal, WakeveTheme.Spacing.sm)
                    .padding(.vertical, WakeveTheme.Spacing.xxs)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Capsule())
            }

            TextField(
                "Décris ton événement",
                text: Binding(
                    get: { viewModel.smartEventDraftState.phrase },
                    set: { viewModel.updateSmartEventDraftPhrase($0) }
                ),
                axis: .vertical
            )
            .font(WakeveTheme.Typography.body)
            .foregroundColor(primaryTextColor)
            .lineLimit(2...4)
            .padding(WakeveTheme.Spacing.md)
            .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))

            smartEventDraftStatus

            HStack(spacing: WakeveTheme.Spacing.sm) {
                Button {
                    viewModel.generateSmartEventDraft()
                } label: {
                    Label("Préparer", systemImage: "wand.and.stars")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .foregroundColor(viewModel.smartEventDraftState.canGenerate ? previewButtonForeground : disabledControlForeground)
                        .background(viewModel.smartEventDraftState.canGenerate ? previewButtonBackground : disabledControlBackground)
                        .clipShape(Capsule())
                }
                .disabled(!viewModel.smartEventDraftState.canGenerate)
                .buttonStyle(.plain)

                if case .streaming = viewModel.smartEventDraftState.phase {
                    Button {
                        viewModel.cancelSmartEventDraft()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.headline.weight(.bold))
                            .foregroundColor(primaryTextColor)
                            .frame(width: 44, height: 44)
                            .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(String(localized: "ai.cancel_suggestion_accessibility"))
                }
            }
        }
        .padding(WakeveTheme.Spacing.md)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme).opacity(0.72))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous))
    }

    @ViewBuilder
    private var smartEventDraftStatus: some View {
        switch viewModel.smartEventDraftState.phase {
        case .idle:
            EmptyView()
        case .checkingAvailability, .preparing:
            smartEventDraftLoadingRow(String(localized: "ai.preparing"))
        case .streaming:
            smartEventDraftPreview(
                title: viewModel.smartEventDraftState.streamedTitle,
                description: viewModel.smartEventDraftState.streamedDescription,
                dateOptions: viewModel.smartEventDraftState.streamedDateOptions,
                checklist: viewModel.smartEventDraftState.streamedChecklist,
                polls: viewModel.smartEventDraftState.streamedPolls,
                draft: nil
            )
        case .ready(let draft):
            smartEventDraftPreview(
                title: draft.title,
                description: draft.description,
                dateOptions: draft.dateOptions,
                checklist: draft.checklist,
                polls: draft.suggestedPolls,
                draft: draft
            )
        case .unavailable(let availability):
            Text(availability.discreetMessage)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(secondaryTextColor)
        case .failed(let message):
            Text(message)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(WakeveTheme.ColorToken.destructive(for: colorScheme))
        case .cancelled:
            Text("Suggestion annulée")
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(secondaryTextColor)
        }
    }

    private func smartEventDraftLoadingRow(_ title: String) -> some View {
        HStack(spacing: WakeveTheme.Spacing.sm) {
            ProgressView()
                .controlSize(.small)
                .accessibilityLabel(title)
            Text(title)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(secondaryTextColor)
            Spacer()
        }
        .frame(minHeight: 44)
    }

    private func smartEventDraftPreview(
        title: String,
        description: String,
        dateOptions: [DateOption],
        checklist: [ChecklistItem],
        polls: [PollSuggestion],
        draft: EventDraft?
    ) -> some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
            if !title.isEmpty {
                Text(title)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(primaryTextColor)
                    .lineLimit(2)
            }

            if !description.isEmpty {
                Text(description)
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(secondaryTextColor)
                    .lineLimit(3)
            }

            if !dateOptions.isEmpty {
                smartEventDraftSection(
                    icon: "calendar",
                    title: "Dates",
                    values: dateOptions.map { $0.label }
                )
            }

            if !checklist.isEmpty {
                smartEventDraftSection(
                    icon: "checklist",
                    title: "Checklist",
                    values: checklist.map(\.title)
                )
            }

            if !polls.isEmpty {
                smartEventDraftSection(
                    icon: "chart.bar.doc.horizontal",
                    title: "Sondages",
                    values: polls.map(\.question)
                )
            }

            if let draft {
                HStack(spacing: WakeveTheme.Spacing.sm) {
                    Button("Modifier") {
                        applySmartEventDraft(draft, advance: false)
                    }
                    .buttonStyle(.bordered)

                    Button(String(localized: "common.apply")) {
                        applySmartEventDraft(draft, advance: true)
                    }
                    .buttonStyle(.borderedProminent)

                    Button("Ignorer") {
                        viewModel.ignoreSmartEventDraft()
                    }
                    .buttonStyle(.bordered)
                }
                .controlSize(.small)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(WakeveTheme.Spacing.sm)
        .background(WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0.42))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }

    private func smartEventDraftSection(icon: String, title: String, values: [String]) -> some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
            Label(title, systemImage: icon)
                .font(WakeveTheme.Typography.tiny)
                .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                .textCase(.uppercase)

            ForEach(values.prefix(3), id: \.self) { value in
                Text(value)
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(primaryTextColor)
                    .lineLimit(2)
            }
        }
    }

    private var dateStep: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Button(action: prepareNewSlotDraft) {
                    CreateEventChoiceRow(
                        systemImage: "calendar.badge.plus",
                        title: "Ajouter un créneau",
                        value: proposedSlotsSummary
                    )
                }
                .buttonStyle(.plain)

                if proposedSlotDrafts.isEmpty {
                    Text("Ajoutez au moins un créneau pour ouvrir le sondage.")
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(secondaryTextColor)
                } else {
                    VStack(spacing: WakeveTheme.Spacing.sm) {
                        ForEach(proposedSlotDrafts) { slot in
                            CreateEventSlotRow(
                                title: slot.displayTitle,
                                subtitle: slot.displaySubtitle,
                                onEdit: { editProposedSlot(slot) },
                                onRemove: { removeProposedSlot(slot) }
                            )
                        }
                    }
                }
            }
        }
    }

    private var placeStep: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            Button(action: { showingLocationSheet = true }) {
                CreateEventChoiceRow(
                    systemImage: "mappin.circle.fill",
                    title: "Lieu",
                    value: selectedLocation ?? String(localized: "events.location")
                )
            }
            .buttonStyle(.plain)
        }
    }

    private var inviteStep: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text("Combien de personnes attendez-vous ?")
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryTextColor)

                Stepper(value: expectedParticipantsBinding, in: 1...200) {
                    let count = expectedParticipants ?? 1
                    Text("\(count) participant\(count > 1 ? "s" : "")")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(primaryTextColor)
                }

                Text("Vous pourrez inviter les personnes précises après la création.")
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(secondaryTextColor)
            }
        }
    }

    private var confirmStep: some View {
        LiquidGlassCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text("Prêt à créer")
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryTextColor)

                CreateEventChoiceRow(systemImage: "textformat", title: "Nom", value: title.isEmpty ? "Sans titre" : title)
                CreateEventChoiceRow(systemImage: "calendar", title: "Créneaux", value: proposedSlotsSummaryForConfirm)
                CreateEventChoiceRow(systemImage: "mappin.and.ellipse", title: "Lieu", value: selectedLocation ?? "À choisir plus tard")
                let count = expectedParticipants ?? 1
                CreateEventChoiceRow(systemImage: "person.2.fill", title: "Invités", value: "\(count) attendu\(count > 1 ? "s" : "")")
                planningModePicker
                if planningMode == .scenarioMatrix {
                    CreateEventChoiceRow(
                        systemImage: "square.grid.2x2.fill",
                        title: "Options",
                        value: "\(scenarioMatrixPreviewCount) combinaison(s) créneau × destination"
                    )
                }

                Text("Un aperçu s’ouvrira avant la création de l’événement.")
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(secondaryTextColor)

                DisclosureGroup(isExpanded: $showConfirmAdvancedOptions) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                        Button(action: { showingLocationSheet = true }) {
                            CreateEventChoiceRow(
                                systemImage: "mappin.circle.fill",
                                title: "Lieu",
                                value: selectedLocation ?? "À choisir plus tard"
                            )
                        }
                        .buttonStyle(.plain)

                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Text("Taille du groupe")
                                .font(WakeveTheme.Typography.caption)
                                .foregroundColor(secondaryTextColor)
                                .textCase(.uppercase)

                            Stepper(value: expectedParticipantsBinding, in: 1...200) {
                                let count = expectedParticipants ?? 1
                                Text("\(count) participant\(count > 1 ? "s" : "") attendu\(count > 1 ? "s" : "")")
                                    .font(WakeveTheme.Typography.bodySemibold)
                                    .foregroundColor(primaryTextColor)
                            }

                            Button("Réinitialiser") {
                                expectedParticipants = nil
                            }
                            .buttonStyle(.bordered)
                            .controlSize(.small)
                            .disabled(expectedParticipants == nil)
                        }

                        Button(action: { showingBackgroundPicker = true }) {
                            CreateEventChoiceRow(
                                systemImage: "photo.on.rectangle",
                                title: "Fond",
                                value: hasCustomBackground ? "Personnalisé" : "Par défaut"
                            )
                        }
                        .buttonStyle(.plain)

                        planningModePicker

                        if planningMode == .scenarioMatrix {
                            Text("Le mode options combine les créneaux avec le lieu choisi. Gardez le mode créneaux pour un sondage simple.")
                                .font(WakeveTheme.Typography.callout)
                                .foregroundColor(secondaryTextColor)
                        }
                    }
                    .padding(.top, WakeveTheme.Spacing.sm)
                } label: {
                    Label("Options avancées", systemImage: "slider.horizontal.3")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(primaryTextColor)
                }
                .tint(WakeveTheme.ColorToken.accent(for: colorScheme))

                if showValidationError, let msg = validationMessage {
                    Text(msg)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.destructive(for: colorScheme))
                }
            }
        }
    }

    private var createBottomAction: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0),
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 32)
            .allowsHitTesting(false)

            HStack(spacing: WakeveTheme.Spacing.sm) {
                if currentStep != .name {
                    Button {
                        moveToPreviousStep()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.headline.weight(.bold))
                            .foregroundColor(primaryTextColor)
                            .frame(width: 52, height: 52)
                            .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                            .clipShape(Circle())
                            .liquidGlass(cornerRadius: WakeveTheme.Radius.full)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(String(localized: "common.back"))
                }

                LiquidGlassButton(
                    currentStep == .confirm ? "Voir l’aperçu" : "Continuer",
                    systemImage: currentStep == .confirm ? "eye.fill" : "arrow.right",
                    variant: .primary,
                    isDisabled: !canAdvanceStep,
                    action: advanceCreateStep
                )
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.bottom, WakeveTheme.Spacing.sm)
            .background(WakeveTheme.ColorToken.pageBackground(for: colorScheme))
        }
    }

    private var expectedParticipantsBinding: Binding<Int> {
        Binding(
            get: { expectedParticipants ?? 1 },
            set: { expectedParticipants = $0 }
        )
    }

    private var canAdvanceStep: Bool {
        switch currentStep {
        case .name:
            return hasRequiredEventText
        case .date:
            return !proposedSlotDrafts.isEmpty
        case .confirm:
            return canCreate
        }
    }

    private func advanceCreateStep() {
        guard canAdvanceStep else {
            showValidationError = true
            validationMessage = validationMessageForCurrentStep
            if currentStep == .confirm {
                showConfirmAdvancedOptions = true
            }
            return
        }

        showValidationError = false
        validationMessage = nil

        if currentStep == .confirm {
            openPreviewIfValid()
            return
        }

        currentStep = currentStep.next
    }

    private func moveToPreviousStep() {
        currentStep = currentStep.previous
    }
    
    @ViewBuilder
    private var gradientBackground: some View {
        switch selectedBackground {
        case .gradient:
            eventBackgroundGradient
                .ignoresSafeArea()
            
        case .preset(let bg):
            GeometryReader { geometry in
                let topHeight = geometry.size.height * 0.45
                let pageBackground = WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                VStack(spacing: 0) {
                    // Top half: background color + emojis
                    ZStack {
                        bg.backgroundColor
                        EmojiScatterView(emojis: bg.emojis, density: .high)
                    }
                    .frame(height: topHeight)
                    
                    // Gradient fade from bg color to darker
                    LinearGradient(
                        colors: [
                            bg.backgroundColor,
                            bg.backgroundColor.opacity(colorScheme == .dark ? 0.85 : 0.42),
                            colorScheme == .dark ? darkenColor(bg.backgroundColor, by: 0.35) : pageBackground
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 80)
                    
                    // Bottom half: darker shade, no emojis
                    (colorScheme == .dark ? darkenColor(bg.backgroundColor, by: 0.35) : pageBackground)
                        .frame(maxHeight: .infinity)
                }
            }
            .ignoresSafeArea()
            
        case .photo(let image):
            GeometryReader { geometry in
                let topHeight = geometry.size.height * 0.45
                let pageBackground = WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                VStack(spacing: 0) {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(height: topHeight)
                        .clipped()
                    
                    // Gradient fade
                    LinearGradient(
                        colors: [
                            Color(hex: "1A1A3E").opacity(colorScheme == .dark ? 1 : 0.22),
                            pageBackground
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 60)
                    
                    pageBackground
                        .frame(maxHeight: .infinity)
                }
            }
            .ignoresSafeArea()
        }
    }
    
    // MARK: - Header View
    
    private var headerView: some View {
        HStack {
            WakeveCircleButton(
                systemImage: "xmark",
                accessibilityLabel: String(localized: "common.close"),
                variant: colorScheme == .dark || hasCustomBackground ? .glass : .light,
                size: 48
            ) {
                dismiss()
            }
            
            Spacer()
            
            Button(action: openPreviewIfValid) {
                Text(String(localized: "events.preview"))
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(canCreate ? previewButtonForeground : disabledControlForeground)
                    .padding(.horizontal, 24)
                    .frame(height: 54)
                    .background(canCreate ? previewButtonBackground : disabledControlBackground)
                    .clipShape(Capsule())
            }
            .disabled(!canCreate)
            .accessibilityLabel(String(localized: "events.preview"))
        }
    }
    
    // MARK: - Background Image Selector
    
    private var backgroundImageSelector: some View {
        VStack(spacing: 16) {
            if case .gradient = selectedBackground {
                // Default state - show add background button
                Button(action: { showingBackgroundPicker = true }) {
                    ZStack {
                        Circle()
                            .fill(heroControlBackground)
                            .frame(width: 80, height: 80)
                        
                        Image(systemName: "photo")
                            .font(.system(size: 32))
                            .foregroundColor(heroControlForeground.opacity(0.82))
                    }
                }
                .accessibilityLabel(String(localized: "events.add_background"))

                Button(action: { showingBackgroundPicker = true }) {
                    Text(String(localized: "events.add_background"))
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(heroControlForeground)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(heroControlBackground)
                        .cornerRadius(24)
                }
            } else {
                // Background selected - context menu button
                Menu {
                    Button(action: { showingBackgroundPicker = true }) {
                        Label(String(localized: "events.background.adjust"), systemImage: "crop")
                    }
                    
                    Button(action: { showingBackgroundPicker = true }) {
                        Label(String(localized: "events.background.modify"), systemImage: "photo.on.rectangle")
                    }
                    
                    Button(action: { showingImageDescription = true }) {
                        Label(String(localized: "events.background.edit_description"), systemImage: "text.below.photo")
                    }
                    
                    Divider()
                    
                    Button(role: .destructive, action: { selectedBackground = .gradient }) {
                        Label(String(localized: "events.background.delete"), systemImage: "trash")
                    }
                } label: {
                    WakeveGlassControl {
                        Text(String(localized: "events.background.modify"))
                            .font(WakeveTheme.Typography.metadata)
                            .foregroundColor(heroControlForeground)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                    }
                }
            }
        }
    }
    
    // MARK: - Main Event Card
    
    private var mainEventCard: some View {
        WakeveEventPanel(cornerRadius: 28, padding: 0) {
            VStack(spacing: 0) {
                // Event Title Input (inside the card now)
                ZStack(alignment: .center) {
                    if title.isEmpty {
                        Text(String(localized: "events.title_placeholder"))
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(placeholderTextColor)
                            .multilineTextAlignment(.center)
                            .lineSpacing(4)
                    }
                    TextField("", text: $title)
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(primaryTextColor)
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.5)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 32)
                        .accessibilityLabel(String(localized: "events.title_placeholder"))
                }

                Divider()
                    .background(separatorColor)
                    .padding(.horizontal, 24)

                // Date & Time Row
                DetailRow(
                    icon: "calendar.badge.plus",
                    label: proposedSlotsSummary,
                    isPlaceholder: proposedSlotDrafts.isEmpty,
                    iconColor: colorScheme == .dark ? WakeveTheme.ColorToken.eventLilacAction : WakeveTheme.ColorToken.permissionBlue
                ) {
                    prepareNewSlotDraft()
                }

                Divider()
                    .background(separatorColor)
                    .padding(.horizontal, 24)

                // Location Row
                DetailRow(
                    icon: "mappin.circle.fill",
                    label: selectedLocation ?? String(localized: "events.location"),
                    isPlaceholder: selectedLocation == nil,
                    iconColor: WakeveTheme.ColorToken.permissionBlue
                ) {
                    showingLocationSheet = true
                }
            }
        }
    }
    
    // MARK: - Organizer Card (separate card)
    
    private var organizerCard: some View {
        WakeveEventPanel(cornerRadius: 28) {
            VStack(spacing: 16) {
                // Profile photo
                ZStack {
                    Circle()
                        .fill(WakeveTheme.ColorToken.profileWarmTop)
                        .frame(width: 56, height: 56)

                    if let name = userName {
                        Text(String(name.prefix(1)).uppercased())
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                    } else {
                        Image(systemName: "person.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.white)
                    }
                }

                // Organizer text
                Text(String(format: String(localized: "events.organized_by"), userName ?? String(localized: "leaderboard.you")))
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(primaryTextColor)
                
                // Description or Button
                if description.isEmpty {
                    WakeveActionButton(
                        String(localized: "events.add_description"),
                        variant: .primary
                    ) {
                        showingEventInfoSheet = true
                    }
                    .frame(maxWidth: 260)
                } else {
                    // Description text (tappable to edit)
                    Button(action: {
                        showingEventInfoSheet = true
                    }) {
                        Text(description)
                            .font(WakeveTheme.Typography.body)
                            .foregroundColor(secondaryTextColor)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 8)
                    }
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Event Type Selector

    private var eventTypeCard: some View {
        WakeveEventPanel(cornerRadius: 20, padding: 0) {
            VStack(alignment: .leading, spacing: 0) {
                // Current selection
                Button(action: { showingEventTypePicker = true }) {
                    HStack(spacing: 12) {
                        Text(eventTypeEmoji(selectedEventType))
                            .font(.system(size: 20))

                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "events.type"))
                                .font(WakeveTheme.Typography.tiny)
                                .foregroundColor(secondaryTextColor)
                            Text(selectedEventType == Shared.EventType.other ? String(localized: "events.type.choose") : eventTypeDisplayName(selectedEventType))
                                .font(WakeveTheme.Typography.bodySemibold)
                                .foregroundColor(selectedEventType == Shared.EventType.other ? placeholderTextColor : primaryTextColor)
                        }

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(secondaryTextColor.opacity(0.7))
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                }
            }
        }
        .sheet(isPresented: $showingEventTypePicker) {
            EventTypePickerSheet(
                selectedIndex: $selectedEventTypeIndex,
                types: eventTypes
            )
            .presentationDetents([.medium, .large])
        }
    }

    // MARK: - Create Button
    
    private var createButton: some View {
        VStack(spacing: 8) {
            WakeveActionButton(
                String(localized: "events.create_event_button"),
                systemImage: "checkmark",
                variant: .eventNext
            ) {
                if canCreate {
                    showValidationError = false
                    validationMessage = nil
                    openPreviewIfValid()
                } else {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        showValidationError = true
                        validationMessage = validationMessageForCurrentStep
                    }
                }
            }

            if showValidationError, let msg = validationMessage {
                Text(msg)
                    .font(.system(size: 13))
                    .foregroundColor(Color(hex: "FF6B6B"))
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }
    
    // MARK: - Helpers
    
    private var hasCustomBackground: Bool {
        if case .gradient = selectedBackground { return false }
        return true
    }

    private var eventBackgroundGradient: LinearGradient {
        if colorScheme == .dark {
            return WakeveTheme.EventGradient.invitation
        }

        return LinearGradient(
            colors: [
                Color(hex: "FFF4FB"),
                Color(hex: "E9DDFF"),
                Color(hex: "D7E8FF"),
                WakeveTheme.ColorToken.appLight
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private var primaryTextColor: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var secondaryTextColor: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    private var placeholderTextColor: Color {
        colorScheme == .dark ? Color.white.opacity(0.62) : Color(hex: "6F6475")
    }

    private var separatorColor: Color {
        WakeveTheme.ColorToken.separator(for: colorScheme)
    }

    private var heroControlForeground: Color {
        colorScheme == .dark || hasCustomBackground ? .white : primaryTextColor
    }

    private var heroControlBackground: Color {
        colorScheme == .dark || hasCustomBackground ? Color.white.opacity(0.15) : Color.white.opacity(0.82)
    }

    private var previewButtonBackground: Color {
        colorScheme == .dark ? WakeveTheme.ColorToken.eventLilacAction : WakeveTheme.ColorToken.permissionBlue
    }

    private var previewButtonForeground: Color {
        colorScheme == .dark ? WakeveTheme.ColorToken.eventLilacText : .white
    }

    private var disabledControlBackground: Color {
        colorScheme == .dark ? Color.white.opacity(0.12) : Color.black.opacity(0.08)
    }

    private var disabledControlForeground: Color {
        colorScheme == .dark ? Color.white.opacity(0.42) : Color.black.opacity(0.34)
    }

    private var hasRequiredEventText: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty &&
        !description.trimmingCharacters(in: .whitespaces).isEmpty
    }
    
    private var canCreate: Bool {
        hasRequiredEventText &&
        !proposedSlotDrafts.isEmpty &&
        (planningMode == .timeSlotPoll || selectedLocation != nil)
    }

    private var validationMessageForCurrentStep: String {
        if !hasRequiredEventText {
            return "Le titre et la description sont requis pour créer l’événement."
        }

        if proposedSlotDrafts.isEmpty {
            return "Ajoutez au moins un créneau avant de continuer."
        }

        if planningMode == .scenarioMatrix && selectedLocation == nil {
            return "Ajoutez au moins une destination pour le mode scénarios."
        }

        return "Vérifiez les détails avant de continuer."
    }

    private var scenarioMatrixPreviewCount: Int {
        planningMode == .scenarioMatrix && selectedLocation != nil ? proposedSlotDrafts.count : 0
    }

    private var planningModePicker: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
            Text("Mode de vote")
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(secondaryTextColor)
                .textCase(.uppercase)

            Picker("Mode de vote", selection: $planningMode) {
                Text("Créneaux").tag(EventPlanningMode.timeSlotPoll)
                Text("Options").tag(EventPlanningMode.scenarioMatrix)
            }
            .pickerStyle(.segmented)
        }
    }

    private var proposedSlotsSummary: String {
        switch proposedSlotDrafts.count {
        case 0:
            return String(localized: "events.date_and_time")
        case 1:
            return proposedSlotDrafts[0].displayTitle
        default:
            return "\(proposedSlotDrafts.count) créneaux proposés"
        }
    }

    private var proposedSlotsSummaryForConfirm: String {
        switch proposedSlotDrafts.count {
        case 0:
            return "Aucun créneau"
        case 1:
            return proposedSlotDrafts[0].displayTitle
        default:
            return "\(proposedSlotDrafts.count) options de vote"
        }
    }

    private func prepareNewSlotDraft() {
        editingSlotID = nil
        let now = Date()
        startDate = now
        startTime = now
        hasEndTime = false
        isAllDay = false
        endTime = Calendar.current.date(byAdding: .hour, value: 1, to: now) ?? now.addingTimeInterval(3600)
        showingDatePicker = true
    }

    private func editProposedSlot(_ slot: EventSlotDraft) {
        editingSlotID = slot.id
        isAllDay = slot.isAllDay
        startDate = slot.startDate
        startTime = slot.startTime
        hasEndTime = slot.hasEndTime
        endTime = slot.endTime
        showingDatePicker = true
    }

    private func removeProposedSlot(_ slot: EventSlotDraft) {
        proposedSlotDrafts.removeAll { $0.id == slot.id }
        selectedDate = proposedSlotDrafts.first?.startDate
    }

    private func appendOrUpdateCurrentSlotDraft() {
        let draft = EventSlotDraft(
            id: editingSlotID ?? UUID(),
            startDate: startDate,
            startTime: startTime,
            isAllDay: isAllDay,
            hasEndTime: hasEndTime,
            endTime: endTime
        )

        if let editingSlotID,
           let index = proposedSlotDrafts.firstIndex(where: { $0.id == editingSlotID }) {
            proposedSlotDrafts[index] = draft
        } else {
            proposedSlotDrafts.append(draft)
        }

        selectedDate = proposedSlotDrafts.first?.startDate
        self.editingSlotID = nil
    }

    private func selectedSlotInputs() -> [EventTimeSlotInput] {
        let iso = ISO8601DateFormatter()
        return proposedSlotDrafts.map { $0.timeSlotInput(using: iso) }
    }

    private func openPreviewIfValid() {
        guard canCreate else {
            withAnimation(.easeInOut(duration: 0.2)) {
                showValidationError = true
                validationMessage = validationMessageForCurrentStep
            }
            return
        }

        showValidationError = false
        validationMessage = nil
        showingPreview = true
    }

    private func applySmartEventDraft(_ draft: EventDraft, advance: Bool) {
        title = draft.title
        description = draft.description

        let location = draft.destinationName.isEmpty ? draft.locationHint : draft.destinationName
        if !location.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            selectedLocation = location
        }

        if let participantHint = draft.participantHints.first {
            let digits = participantHint.filter(\.isNumber)
            if let count = Int(digits), count > 0 {
                expectedParticipants = count
            }
        }

        viewModel.ignoreSmartEventDraft()

        if advance {
            currentStep = proposedSlotDrafts.isEmpty ? .date : .confirm
        }
    }
    
    private func darkenColor(_ color: Color, by amount: CGFloat) -> Color {
        let uiColor = UIColor(color)
        var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        uiColor.getHue(&h, saturation: &s, brightness: &b, alpha: &a)
        return Color(hue: Double(h), saturation: Double(min(s + 0.1, 1.0)), brightness: Double(max(b - amount, 0)), opacity: Double(a))
    }
    
    private func formattedDateTime() -> String {
        if let firstSlot = proposedSlotDrafts.first {
            return firstSlot.displayTitle
        }

        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        dateFormatter.locale = Locale(identifier: "fr_FR")
        
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"
        
        if isAllDay {
            return dateFormatter.string(from: startDate) + " (\(String(localized: "events.all_day")))"
        } else {
            return dateFormatter.string(from: startDate) + " à " + timeFormatter.string(from: startTime)
        }
    }
    
    private func createEvent() {
        guard canCreate else {
            showValidationError = true
            validationMessage = validationMessageForCurrentStep
            return
        }

        viewModel.onEventCreated = { event in
            onEventCreated(event)
        }
        viewModel.createEvent(
            title: title,
            description: description,
            userId: userId,
            eventType: selectedEventType,
            selectedSlots: selectedSlotInputs(),
            expectedParticipants: expectedParticipants.map { Int32($0) },
            planningMode: planningMode
        )

        dismiss()
    }

}

private enum CreateEventStep: CaseIterable {
    case name
    case date
    case confirm

    var index: Int {
        Self.allCases.firstIndex(of: self) ?? 0
    }

    var progress: CGFloat {
        CGFloat(index + 1) / CGFloat(Self.allCases.count)
    }

    var title: String {
        switch self {
        case .name: return "Nom"
        case .date: return "Date"
        case .confirm: return "Confirmer"
        }
    }

    var eyebrow: String {
        switch self {
        case .name: return "Nom"
        case .date: return "Date"
        case .confirm: return "Confirmation"
        }
    }

    var question: String {
        switch self {
        case .name: return "Comment s’appelle l’événement ?"
        case .date: return "Quand aura-t-il lieu ?"
        case .confirm: return "Vérifiez les détails."
        }
    }

    var subtitle: String {
        switch self {
        case .name: return "Un nom clair et une courte description aident les invités à comprendre l’événement."
        case .date: return "Ajoutez un ou plusieurs créneaux pour lancer le sondage avec de vraies options."
        case .confirm: return "Ajustez seulement les options utiles, puis prévisualisez l’invitation."
        }
    }

    var next: CreateEventStep {
        let steps = Self.allCases
        let nextIndex = min(index + 1, steps.count - 1)
        return steps[nextIndex]
    }

    var previous: CreateEventStep {
        let steps = Self.allCases
        let previousIndex = max(index - 1, 0)
        return steps[previousIndex]
    }
}

private struct CreateEventChoiceRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let systemImage: String
    let title: String
    let value: String

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.md) {
            Image(systemName: systemImage)
                .font(.body.weight(.semibold))
                .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                .frame(width: 42, height: 42)
                .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.sm, style: .continuous))

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                Text(title)
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .textCase(.uppercase)

                Text(value)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(2)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
        }
        .padding(WakeveTheme.Spacing.md)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

private struct EventSlotDraft: Identifiable, Equatable {
    let id: UUID
    var startDate: Date
    var startTime: Date
    var isAllDay: Bool
    var hasEndTime: Bool
    var endTime: Date

    var displayTitle: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: startDateTime)
    }

    var displaySubtitle: String {
        if isAllDay {
            return String(localized: "events.all_day")
        }

        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        let start = formatter.string(from: startDateTime)

        if let endDateTime {
            return "\(start) - \(formatter.string(from: endDateTime))"
        }

        return start
    }

    func timeSlotInput(using formatter: ISO8601DateFormatter) -> EventTimeSlotInput {
        EventTimeSlotInput(
            start: formatter.string(from: startDateTime),
            end: endDateTime.map { formatter.string(from: $0) },
            timeOfDay: isAllDay ? .allDay : .specific
        )
    }

    private var startDateTime: Date {
        combinedDateTime(date: startDate, time: startTime)
    }

    private var endDateTime: Date? {
        guard !isAllDay else { return nil }

        if hasEndTime {
            let combinedEnd = combinedDateTime(date: startDate, time: endTime)
            if combinedEnd <= startDateTime {
                return Calendar.current.date(byAdding: .day, value: 1, to: combinedEnd)
            }
            return combinedEnd
        }

        return Calendar.current.date(byAdding: .hour, value: 1, to: startDateTime)
    }

    private func combinedDateTime(date: Date, time: Date) -> Date {
        let calendar = Calendar.current
        let dateComponents = calendar.dateComponents([.year, .month, .day], from: date)
        let timeComponents = calendar.dateComponents([.hour, .minute], from: time)

        var components = DateComponents()
        components.year = dateComponents.year
        components.month = dateComponents.month
        components.day = dateComponents.day
        components.hour = timeComponents.hour
        components.minute = timeComponents.minute

        return calendar.date(from: components) ?? date
    }
}

private struct CreateEventSlotRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let onEdit: () -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.sm) {
            Button(action: onEdit) {
                HStack(spacing: WakeveTheme.Spacing.md) {
                    Image(systemName: "calendar")
                        .font(.body.weight(.semibold))
                        .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                        .frame(width: 38, height: 38)
                        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.sm, style: .continuous))

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(title)
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)

                        Text(subtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)
                    }

                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Button(role: .destructive, action: onRemove) {
                Image(systemName: "trash")
                    .font(.body.weight(.semibold))
                    .foregroundColor(WakeveTheme.ColorToken.destructive(for: colorScheme))
                    .frame(width: 38, height: 38)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: "events.remove_time_slot_accessibility"))
        }
        .padding(WakeveTheme.Spacing.sm)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

// MARK: - Date Time Picker Popup

struct DateTimePickerPopup: View {
    @Environment(\.colorScheme) private var colorScheme

    @Binding var isAllDay: Bool
    @Binding var startDate: Date
    @Binding var startTime: Date
    @Binding var hasEndTime: Bool
    @Binding var endTime: Date
    
    let onSave: () -> Void
    let onCancel: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            // Semi-transparent overlay to block interaction with background
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture { onCancel() }
            
            // Bottom sheet card
            VStack(spacing: 0) {
                // Header
                HStack(spacing: 0) {
                    // Close button
                    Button(action: onCancel) {
                        Image(systemName: "xmark")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(primaryTextColor)
                            .frame(width: 24, height: 24)
                    }
                    .accessibilityLabel(String(localized: "common.close"))

                    Spacer()

                    Text(String(localized: "events.date_and_time"))
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(primaryTextColor)

                    Spacer()

                    // Save button
                    Button(action: onSave) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(saveButtonForeground)
                            .frame(width: 28, height: 28)
                            .background(saveButtonBackground)
                            .clipShape(Circle())
                    }
                    .accessibilityLabel(String(localized: "common.save"))
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 12)

                // Grouped section with lighter background
                VStack(spacing: 0) {
                    // All Day Toggle
                    HStack {
                        Text(String(localized: "events.all_day"))
                            .font(.system(size: 16))
                            .foregroundColor(primaryTextColor)

                        Spacer()

                        Toggle("", isOn: $isAllDay)
                            .toggleStyle(SwitchToggleStyle(tint: Color(hex: "34C759")))
                            .frame(width: 48, height: 28)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)

                    Divider()
                        .background(separatorColor)

                    // Start Date/Time Section
                    HStack(spacing: 6) {
                        Text(String(localized: "events.start"))
                            .font(.system(size: 16))
                            .foregroundColor(primaryTextColor)

                        Spacer()

                        DatePicker("", selection: $startDate, displayedComponents: .date)
                            .labelsHidden()
                            .datePickerStyle(.compact)

                        if !isAllDay {
                            DatePicker("", selection: $startTime, displayedComponents: .hourAndMinute)
                                .labelsHidden()
                                .datePickerStyle(.compact)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)

                    if hasEndTime && !isAllDay {
                        Divider()
                            .background(separatorColor)

                        HStack(spacing: 6) {
                            Text(String(localized: "events.end"))
                                .font(.system(size: 16))
                                .foregroundColor(primaryTextColor)

                            Spacer()

                            DatePicker("", selection: $endTime, displayedComponents: .hourAndMinute)
                                .labelsHidden()
                                .datePickerStyle(.compact)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    }

                    // Add End Time Link
                    if !hasEndTime && !isAllDay {
                        Divider()
                            .background(separatorColor)

                        Button(action: { hasEndTime = true }) {
                            HStack(spacing: 8) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 15, weight: .semibold))
                                Text(String(localized: "events.add_end_time"))
                                    .font(.system(size: 16, weight: .semibold))
                            }
                            .foregroundColor(colorScheme == .dark ? WakeveTheme.ColorToken.eventLilacAction : WakeveTheme.ColorToken.permissionBlue)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                        }
                    }
                }
                .background(groupedSurfaceColor)
                .cornerRadius(16)
                .padding(.horizontal, 12)
                .padding(.bottom, 16)
                .tint(colorScheme == .dark ? .white : WakeveTheme.ColorToken.permissionBlue)
            }
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(sheetSurfaceColor)
                    .background(.ultraThinMaterial)
            )
            .cornerRadius(24)
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
            .shadow(color: Color.black.opacity(0.25), radius: 40, x: 0, y: -10)
        }
    }

    private var primaryTextColor: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var separatorColor: Color {
        WakeveTheme.ColorToken.separator(for: colorScheme)
    }

    private var sheetSurfaceColor: Color {
        colorScheme == .dark ? Color(hex: "1A1A3E").opacity(0.95) : Color.white.opacity(0.96)
    }

    private var groupedSurfaceColor: Color {
        colorScheme == .dark ? Color.white.opacity(0.08) : Color.black.opacity(0.045)
    }

    private var saveButtonBackground: Color {
        colorScheme == .dark ? Color.white : WakeveTheme.ColorToken.permissionBlue
    }

    private var saveButtonForeground: Color {
        colorScheme == .dark ? .black : .white
    }
    
    private func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: date)
    }
    
    private func formattedTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
}

// MARK: - Detail Row

struct DetailRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let icon: String
    let label: String
    let isPlaceholder: Bool
    let iconColor: Color
    let action: () -> Void
    
    init(icon: String, label: String, isPlaceholder: Bool, iconColor: Color = Color(hex: "6C5CE7"), action: @escaping () -> Void) {
        self.icon = icon
        self.label = label
        self.isPlaceholder = isPlaceholder
        self.iconColor = iconColor
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 28))
                    .foregroundColor(iconColor)
                
                Text(label)
                    .font(.system(size: 18, weight: isPlaceholder ? .regular : .medium))
                    .foregroundColor(isPlaceholder ? placeholderTextColor : primaryTextColor)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
        }
    }

    private var primaryTextColor: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var placeholderTextColor: Color {
        colorScheme == .dark ? Color.white.opacity(0.9) : WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }
}

// MARK: - Event Preview Sheet

private struct EventPreviewSheet: View {
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let description: String
    let userName: String?
    let proposedSlots: [EventSlotDraft]
    let selectedDate: Date?
    let selectedLocation: String?
    let isAllDay: Bool
    let startDate: Date
    let startTime: Date
    let hasEndTime: Bool
    let endTime: Date
    var eventType: Shared.EventType = Shared.EventType.other
    var expectedParticipants: Int? = nil
    var planningMode: EventPlanningMode = .timeSlotPoll
    var matrixScenarioCount: Int = 0
    var onNext: () -> Void = {}

    @State private var mapPosition: MapCameraPosition = .automatic
    @State private var geocodedCoordinate: CLLocationCoordinate2D?
    private let previewHeaderReservedHeight: CGFloat = 108

    var body: some View {
        VStack(spacing: 0) {
            // Scrollable gradient section
            ZStack(alignment: .top) {
                // Gradient background
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color(hex: "FF6B35"),
                        Color(hex: "FF4757"),
                        Color(hex: "8B5CF6"),
                        Color(hex: "6366F1"),
                        Color(hex: "3B82F6"),
                        Color(hex: "1E3A8A"),
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea(edges: .top)

                // Scrollable content
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        // Spacer for fixed header
                        Color.clear.frame(height: previewHeaderReservedHeight)

                        // Event title
                        Text(title)
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)
                            .lineLimit(2)
                            .minimumScaleFactor(0.74)
                            .padding(.horizontal, 64)

                        // EventType badge (if not default)
                        if eventType != Shared.EventType.other && eventType != Shared.EventType.custom {
                            HStack(spacing: 6) {
                                Text(eventTypeEmoji(eventType))
                                    .font(.system(size: 14))
                                Text(eventTypeDisplayName(eventType))
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.white.opacity(0.2))
                            .clipShape(Capsule())
                            .padding(.top, 4)
                        }

                        // Date and location
                        if !proposedSlots.isEmpty || selectedDate != nil || selectedLocation != nil {
                            VStack(spacing: 4) {
                                if !proposedSlots.isEmpty {
                                    Text(proposedSlots.count == 1 ? proposedSlots[0].displayTitle : "\(proposedSlots.count) créneaux proposés")
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundColor(.white.opacity(0.7))
                                } else if selectedDate != nil {
                                    Text(formattedDateTime())
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundColor(.white.opacity(0.7))
                                }
                                if let location = selectedLocation {
                                    Text(location)
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundColor(.white.opacity(0.7))
                                }
                            }
                        }

                        if planningMode == .scenarioMatrix {
                            Text("\(matrixScenarioCount) option(s) créneau × destination à préparer")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(.white.opacity(0.82))
                                .lineLimit(1)
                                .minimumScaleFactor(0.78)
                        }

                        // RSVP card
                        HStack(spacing: 0) {
                            rsvpColumn(icon: "checkmark.circle", label: String(localized: "common.yes"))
                            Rectangle().fill(Color.white.opacity(0.15)).frame(width: 1, height: 40)
                            rsvpColumn(icon: "xmark.circle", label: String(localized: "common.no"))
                            Rectangle().fill(Color.white.opacity(0.15)).frame(width: 1, height: 40)
                            rsvpColumn(icon: "questionmark.circle", label: String(localized: "events.rsvp.maybe"))
                        }
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: [Color(hex: "3B2078"), Color(hex: "2D1B69")],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .cornerRadius(20)
                        .padding(.top, 8)

                        if !proposedSlots.isEmpty {
                            proposedSlotsPreviewCard
                        }

                        // Organizer card
                        organizerPreviewCard

                        // Itinerary card (when location is selected)
                        if selectedLocation != nil {
                            itineraryPreviewCard
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }

                // Fixed header overlay
                HStack {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 40, height: 40)
                            .background(Color.white.opacity(0.15))
                            .clipShape(Circle())
                    }
                    .accessibilityLabel(String(localized: "common.back"))

                    Spacer()

                    Button(action: onNext) {
                        Text(String(localized: "events.create_event_button"))
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color(hex: "1A1A3E"))
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(Color(hex: "F5F0E8"))
                            .cornerRadius(20)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 60)
            }
            .clipShape(RoundedCorner(radius: 32, corners: [.bottomLeft, .bottomRight]))

            // Fixed bottom section
            VStack(alignment: .leading, spacing: 16) {
                Text(String(localized: "events.preview.invitation_title"))
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(previewPrimaryText)

                HStack(alignment: .top, spacing: 14) {
                    Image(systemName: "text.below.photo.fill")
                        .font(.system(size: 20))
                        .foregroundColor(Color(hex: "8B5CF6"))
                        .frame(width: 24)
                        .padding(.top, 2)

                    Text(String(localized: "events.preview.invitation_description"))
                        .font(.system(size: 15))
                        .foregroundColor(previewSecondaryText)
                        .lineSpacing(4)
                }

                if planningMode == .scenarioMatrix {
                    HStack(alignment: .top, spacing: 14) {
                        Image(systemName: "square.grid.2x2.fill")
                            .font(.system(size: 20))
                            .foregroundColor(Color(hex: "8B5CF6"))
                            .frame(width: 24)
                            .padding(.top, 2)

                        Text("Les participants voteront sur des options complètes combinant créneau et destination.")
                            .font(.system(size: 15))
                            .foregroundColor(previewSecondaryText)
                            .lineSpacing(4)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)

            Spacer()
        }
        .background(previewPageBackground)
        .ignoresSafeArea(edges: .top)
    }

    private var previewPageBackground: Color {
        WakeveTheme.ColorToken.pageBackground(for: colorScheme)
    }

    private var previewPrimaryText: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var previewSecondaryText: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    // MARK: - Preview Cards

    private var organizerPreviewCard: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color(hex: "FF6B35"))
                    .frame(width: 48, height: 48)

                if let name = userName {
                    Text(String(name.prefix(1)).uppercased())
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)
                } else {
                    Image(systemName: "person.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.white)
                }
            }

            Text(String(format: String(localized: "events.organized_by"), userName ?? String(localized: "leaderboard.you")))
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)

            if !description.isEmpty {
                Text(description)
                    .font(.system(size: 15))
                    .foregroundColor(.white.opacity(0.8))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .background(Color(hex: "12143A").opacity(0.7))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .cornerRadius(20)
    }

    private var proposedSlotsPreviewCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Créneaux proposés")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Color(hex: "8B9FFF"))

            ForEach(Array(proposedSlots.prefix(4))) { slot in
                HStack(spacing: 12) {
                    Image(systemName: slot.isAllDay ? "sun.max.fill" : "clock.fill")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white.opacity(0.84))
                        .frame(width: 28, height: 28)
                        .background(Color.white.opacity(0.14))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 2) {
                        Text(slot.displayTitle)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)

                        Text(slot.displaySubtitle)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.white.opacity(0.72))
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)
                    }

                    Spacer()
                }
            }

            if proposedSlots.count > 4 {
                Text("+\(proposedSlots.count - 4) autre\(proposedSlots.count - 4 > 1 ? "s" : "") option\(proposedSlots.count - 4 > 1 ? "s" : "")")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.white.opacity(0.72))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color(hex: "12143A").opacity(0.7))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .cornerRadius(20)
    }

    private var itineraryPreviewCard: some View {
        VStack(spacing: 12) {
            Text(String(localized: "events.preview.itinerary"))
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Color(hex: "8B9FFF"))

            if let location = selectedLocation {
                Text(location)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)
            }

            // Map view
            Map(position: $mapPosition) {
                if let coord = geocodedCoordinate, let location = selectedLocation {
                    Marker(location, coordinate: coord)
                }
            }
            .frame(height: 180)
            .cornerRadius(12)
            .padding(.horizontal, 12)
            .allowsHitTesting(false)
            .onAppear {
                geocodeLocation()
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(Color(hex: "12143A").opacity(0.7))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .cornerRadius(20)
    }

    private func rsvpColumn(icon: String, label: String) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 22))
                .foregroundColor(.white.opacity(0.9))
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.white.opacity(0.7))
        }
        .frame(maxWidth: .infinity)
    }

    private func formattedDateTime() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        dateFormatter.locale = Locale(identifier: "fr_FR")

        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"

        var result = dateFormatter.string(from: startDate)

        if isAllDay {
            result += " (\(String(localized: "events.all_day")))"
        } else {
            result += " à " + timeFormatter.string(from: startTime)
            if hasEndTime {
                result += " - " + timeFormatter.string(from: endTime)
            }
        }

        return result
    }

    private func geocodeLocation() {
        guard let address = selectedLocation else { return }
        let geocoder = CLGeocoder()
        geocoder.geocodeAddressString(address) { placemarks, _ in
            if let coordinate = placemarks?.first?.location?.coordinate {
                geocodedCoordinate = coordinate
                mapPosition = .region(MKCoordinateRegion(
                    center: coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
                ))
            }
        }
    }
}

// MARK: - Rounded Corner Shape

struct RoundedCorner: Shape {
    var radius: CGFloat
    var corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

// MARK: - EventType Picker Sheet

struct EventTypePickerSheet: View {
    @Binding var selectedIndex: Int
    let types: [Shared.EventType]
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            List {
                ForEach(Array(types.enumerated()), id: \.offset) { index, type in
                    Button(action: {
                        selectedIndex = index
                        dismiss()
                    }) {
                        HStack(spacing: 14) {
                            Text(eventTypeEmoji(type))
                                .font(.system(size: 24))
                                .frame(width: 36)

                            Text(eventTypeDisplayName(type))
                                .font(.system(size: 16))
                                .foregroundColor(.primary)

                            Spacer()

                            if selectedIndex == index {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                                    .fontWeight(.semibold)
                            }
                        }
                        .contentShape(Rectangle())
                    }
                    .listRowBackground(Color(uiColor: .secondarySystemGroupedBackground))
                }
            }
            .navigationTitle("Type d'événement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Fermer") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Preview

struct CreateEventSheet_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            CreateEventSheet(
                userId: "user-123",
                userName: "Guy MANDINA NZEZA"
            )
            .preferredColorScheme(.light)

            CreateEventSheet(
                userId: "user-123",
                userName: "Guy MANDINA NZEZA"
            )
            .preferredColorScheme(.dark)
        }
    }
}
