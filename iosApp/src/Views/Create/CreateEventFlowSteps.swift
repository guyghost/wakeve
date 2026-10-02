import SwiftUI
import Shared

// Étapes du flux de création (couche 7, #47). Les erreurs reçues sont des clés de localisation,
// vides tant que l'utilisateur n'a pas tenté de continuer.

private func localized(_ key: String) -> String {
    String(localized: String.LocalizationValue(key))
}

// MARK: - Quoi ?

struct CreateFlowWhatStep: View {
    @Binding var form: CreateEventForm
    let errors: [CreateEventField: String]

    /// Types en pastilles : les courants, plus celui d'un modèle s'il n'en fait pas partie.
    private var typeNames: [String] {
        var names = CreateEventForm.commonEventTypeNames
        if !names.contains(form.eventTypeName),
           form.eventTypeName != CreateEventForm.otherTypeName,
           form.eventTypeName != CreateEventForm.customTypeName {
            names.insert(form.eventTypeName, at: 0)
        }
        return names
    }

    var body: some View {
        VStack(alignment: .leading, spacing: WK.Space.lg) {
            CreateFlowTextField(
                label: String(localized: "create_flow.field.title"),
                placeholder: String(localized: "create_flow.field.title_placeholder"),
                text: $form.title,
                error: errors[.title],
                accessibilityID: "create_flow.title"
            )
            CreateFlowTextField(
                label: String(localized: "create_flow.field.description"),
                placeholder: String(localized: "create_flow.field.description_placeholder"),
                text: $form.description,
                error: errors[.description],
                multiline: true,
                accessibilityID: "create_flow.description"
            )

            CreateFlowSection(title: String(localized: "create_flow.field.templates")) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: WK.Space.xs) {
                        ForEach(Array(EventScenario.allScenarios.enumerated()), id: \.offset) { index, scenario in
                            let isSelected = form.scenarioId == CreateEventForm.scenarioID(scenario)
                            WKChip(
                                title: scenario.title,
                                systemImage: scenario.icon,
                                isSelected: isSelected,
                                accessibilityID: "create_flow.template.\(index)"
                            ) {
                                if isSelected {
                                    form.clearScenario()
                                } else {
                                    form.apply(scenario: scenario)
                                }
                            }
                        }
                    }
                }
                .scrollClipDisabled()
            }

            CreateFlowSection(title: String(localized: "create_flow.field.type")) {
                CreateFlowChipLayout {
                    ForEach(typeNames, id: \.self) { name in
                        let isSelected = form.eventTypeName == name
                        WKChip(
                            title: eventTypeDisplayName(CreateEventForm.eventType(named: name)),
                            isSelected: isSelected,
                            accessibilityID: "create_flow.type.\(name)"
                        ) {
                            form.eventTypeName = isSelected ? CreateEventForm.otherTypeName : name
                        }
                    }
                    WKChip(
                        title: String(localized: "create_flow.type.other"),
                        isSelected: form.isCustomType,
                        accessibilityID: "create_flow.type.CUSTOM"
                    ) {
                        form.eventTypeName = form.isCustomType ? CreateEventForm.otherTypeName : CreateEventForm.customTypeName
                    }
                }
                if form.isCustomType {
                    CreateFlowTextField(
                        label: String(localized: "create_flow.field.custom_type"),
                        placeholder: String(localized: "create_flow.field.custom_type"),
                        text: $form.eventTypeCustom,
                        error: errors[.customType],
                        accessibilityID: "create_flow.custom_type"
                    )
                }
            }
        }
    }
}

// MARK: - Qui ?

struct CreateFlowWhoStep: View {
    @Binding var form: CreateEventForm
    let errors: [CreateEventField: String]

    var body: some View {
        VStack(alignment: .leading, spacing: WK.Space.sm) {
            CreateFlowCountRow(
                title: String(localized: "create_flow.field.min"),
                value: $form.minParticipants,
                defaultValue: 2,
                error: errors[.minParticipants],
                accessibilityID: "create_flow.count.min"
            )
            CreateFlowCountRow(
                title: String(localized: "create_flow.field.expected"),
                value: $form.expectedParticipants,
                defaultValue: max(form.minParticipants ?? 0, 6),
                error: errors[.expectedParticipants],
                accessibilityID: "create_flow.count.expected"
            )
            CreateFlowCountRow(
                title: String(localized: "create_flow.field.max"),
                value: $form.maxParticipants,
                defaultValue: max(form.expectedParticipants ?? 0, form.minParticipants ?? 0, 10),
                error: errors[.maxParticipants],
                accessibilityID: "create_flow.count.max"
            )
            Label(String(localized: "create_flow.invite_hint"), systemImage: "person.2")
                .font(WK.Typo.caption)
                .foregroundStyle(WK.Colors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, WK.Space.xs)
                .accessibilityIdentifier("create_flow.invite_hint")
        }
    }
}

/// Effectif optionnel : interrupteur pour l'activer, puis `Stepper` (≥ 1).
struct CreateFlowCountRow: View {
    let title: String
    @Binding var value: Int?
    let defaultValue: Int
    let error: String?
    let accessibilityID: String

    var body: some View {
        WKCard {
            Toggle(isOn: Binding(
                get: { value != nil },
                set: { isOn in value = isOn ? (value ?? max(defaultValue, 1)) : nil }
            )) {
                Text(title)
                    .font(WK.Typo.body)
                    .foregroundStyle(WK.Colors.textPrimary)
            }
            .tint(WK.Colors.accent)
            .frame(minHeight: WK.Size.minTapTarget)
            .accessibilityIdentifier("\(accessibilityID).toggle")

            if let current = value {
                Stepper(value: Binding(get: { current }, set: { value = $0 }), in: 1...999) {
                    Text(current, format: .number)
                        .font(WK.Typo.headline)
                        .monospacedDigit()
                        .foregroundStyle(WK.Colors.textPrimary)
                }
                .frame(minHeight: WK.Size.minTapTarget)
                .accessibilityLabel(title)
                .accessibilityValue(Text(current, format: .number))
                .accessibilityIdentifier("\(accessibilityID).stepper")
            } else {
                Text(String(localized: "create_flow.field.count_unset"))
                    .font(WK.Typo.caption)
                    .foregroundStyle(WK.Colors.textSecondary)
            }

            if let error {
                CreateFlowFieldError(message: localized(error), accessibilityID: "\(accessibilityID).error")
            }
        }
    }
}

// MARK: - Où ?

struct CreateFlowPlaceStep: View {
    @Binding var form: CreateEventForm
    let errors: [CreateEventField: String]

    @State private var newName = ""
    @State private var additionError: String?
    @State private var showingSearch = false

    var body: some View {
        VStack(alignment: .leading, spacing: WK.Space.sm) {
            if form.locations.isEmpty {
                Text(String(localized: "create_flow.empty.locations"))
                    .font(WK.Typo.body)
                    .foregroundStyle(WK.Colors.textSecondary)
            }
            ForEach(Array(form.locations.enumerated()), id: \.offset) { index, name in
                CreateFlowLocationRow(name: name, index: index, error: errors[.location(index)]) {
                    form.locations.remove(at: index)
                }
            }

            CreateFlowTextField(
                label: String(localized: "create_flow.add_location"),
                placeholder: String(localized: "create_flow.field.location_placeholder"),
                text: $newName,
                error: additionError,
                accessibilityID: "create_flow.location.input"
            )
            .onSubmit { add(newName) }
            .onChange(of: newName) { _, _ in additionError = nil }

            CreateFlowChipLayout {
                WKChip(
                    title: String(localized: "create_flow.add_location"),
                    systemImage: "plus",
                    accessibilityID: "create_flow.location.add"
                ) { add(newName) }
                WKChip(
                    title: String(localized: "create_flow.search_location"),
                    systemImage: "magnifyingglass",
                    accessibilityID: "create_flow.location.search"
                ) { showingSearch = true }
            }
        }
        .sheet(isPresented: $showingSearch) {
            LocationSelectionSheet(
                onDismiss: { showingSearch = false },
                onConfirm: { location in
                    showingSearch = false
                    add(location.name)
                }
            )
        }
    }

    private func add(_ name: String) {
        if let error = form.locationAdditionError(name) {
            additionError = error
            AccessibilityNotification.Announcement(localized(error)).post()
            return
        }
        form.locations.append(CreateEventForm.trimmed(name))
        newName = ""
        additionError = nil
    }
}

struct CreateFlowLocationRow: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    let name: String
    let index: Int
    let error: String?
    let onRemove: () -> Void

    var body: some View {
        WKCard {
            HStack(spacing: WK.Space.sm) {
                // Icône décorative masquée aux tailles d'accessibilité : la place va au texte.
                if !dynamicTypeSize.isAccessibilitySize {
                    Image(systemName: "mappin.and.ellipse")
                        .font(WK.Typo.body)
                        .foregroundStyle(WK.Colors.accent)
                        .accessibilityHidden(true)
                }
                Text(name)
                    .font(WK.Typo.body)
                    .foregroundStyle(WK.Colors.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
                CreateFlowRemoveButton(
                    accessibilityLabel: String(
                        format: String(localized: "create_flow.a11y.remove_location_format"), name
                    ),
                    accessibilityID: "create_flow.location.remove.\(index)",
                    action: onRemove
                )
            }
            if let error {
                CreateFlowFieldError(message: localized(error), accessibilityID: "create_flow.location.error.\(index)")
            }
        }
    }
}

// MARK: - Quand ?

struct CreateFlowTimeStep: View {
    @Binding var form: CreateEventForm
    let errors: [CreateEventField: String]

    @State private var showingEditor = false

    var body: some View {
        VStack(alignment: .leading, spacing: WK.Space.sm) {
            if form.slots.isEmpty {
                Text(String(localized: "create_flow.empty.slots"))
                    .font(WK.Typo.body)
                    .foregroundStyle(WK.Colors.textSecondary)
            }
            ForEach(Array(form.slots.enumerated()), id: \.element.id) { index, slot in
                CreateFlowSlotRow(slot: slot, index: index, error: errors[.slot(slot.id)]) {
                    form.slots.removeAll { $0.id == slot.id }
                }
            }
            if let error = errors[.slots] {
                CreateFlowFieldError(message: localized(error), accessibilityID: "create_flow.slots.error")
            }
            CreateFlowChipLayout {
                WKChip(
                    title: String(localized: "create_flow.add_slot"),
                    systemImage: "plus",
                    accessibilityID: "create_flow.slot.add"
                ) { showingEditor = true }
            }
        }
        .sheet(isPresented: $showingEditor) {
            CreateFlowSlotEditor { slot in
                form.slots.append(slot)
            }
            .presentationDetents([.medium, .large])
        }
    }
}

struct CreateFlowSlotRow: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    let slot: CreateEventSlot
    let index: Int
    let error: String?
    let onRemove: () -> Void

    static func title(for slot: CreateEventSlot, locale: Locale = WK.appLocale) -> String {
        guard let date = ISO8601DateFormatter().date(from: slot.input.start) else { return slot.input.start }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.setLocalizedDateFormatFromTemplate("EEEEdMMMMy")
        return formatter.string(from: date)
    }

    static func detail(for slot: CreateEventSlot, locale: Locale = WK.appLocale) -> String {
        guard slot.moment == .specific else {
            return WK.localizedFormat(slot.moment.titleKey, locale: locale)
        }
        return TimeSlotDisplayFormatter.hoursText(
            start: slot.input.start,
            end: slot.input.end,
            timezone: TimeZone.current.identifier,
            timeOfDay: .specific,
            locale: locale
        )
    }

    var body: some View {
        WKCard {
            HStack(spacing: WK.Space.sm) {
                // Icône décorative masquée aux tailles d'accessibilité : la place va au texte.
                if !dynamicTypeSize.isAccessibilitySize {
                    Image(systemName: "calendar")
                        .font(WK.Typo.body)
                        .foregroundStyle(WK.Colors.accent)
                        .accessibilityHidden(true)
                }
                VStack(alignment: .leading, spacing: WK.Space.xxxs) {
                    Text(Self.title(for: slot))
                        .font(WK.Typo.headline)
                        .foregroundStyle(WK.Colors.textPrimary)
                    Text(Self.detail(for: slot))
                        .font(WK.Typo.caption)
                        .foregroundStyle(WK.Colors.textSecondary)
                }
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .combine)
                CreateFlowRemoveButton(
                    accessibilityLabel: String(localized: "create_flow.a11y.remove_slot"),
                    accessibilityID: "create_flow.slot.remove.\(index)",
                    action: onRemove
                )
            }
            if let error {
                CreateFlowFieldError(message: localized(error), accessibilityID: "create_flow.slot.error.\(index)")
            }
        }
    }
}

/// Ajout d'un créneau : moment de la journée, puis jour (ou début et fin pour une heure précise).
struct CreateFlowSlotEditor: View {
    let onAdd: (CreateEventSlot) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var moment: CreateEventMoment = .evening
    @State private var day: Date
    @State private var start: Date
    @State private var end: Date

    init(onAdd: @escaping (CreateEventSlot) -> Void) {
        self.onAdd = onAdd
        let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        let start = EventSlotInputBuilder.defaultStartTime(on: tomorrow)
        _day = State(initialValue: tomorrow)
        _start = State(initialValue: start)
        _end = State(initialValue: start.addingTimeInterval(3 * 3600))
    }

    private var slot: CreateEventSlot {
        CreateEventSlotBuilder.slot(day: moment == .specific ? start : day, moment: moment, start: start, end: end)
    }

    private var rangeError: String? {
        moment == .specific && !slot.hasValidRange ? "create_flow.error.slot_end_before_start" : nil
    }

    private var today: Date { Calendar.current.startOfDay(for: Date()) }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker(selection: $moment) {
                        ForEach(CreateEventMoment.allCases) { moment in
                            Text(localized(moment.titleKey)).tag(moment)
                        }
                    } label: {
                        Text(String(localized: "create_flow.slot.editor_title"))
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                    .accessibilityIdentifier("create_flow.slot.moment")
                }
                Section {
                    if moment == .specific {
                        DatePicker(
                            String(localized: "create_flow.slot.start"),
                            selection: $start,
                            in: today...,
                            displayedComponents: [.date, .hourAndMinute]
                        )
                        .accessibilityIdentifier("create_flow.slot.start")
                        DatePicker(
                            String(localized: "create_flow.slot.end"),
                            selection: $end,
                            in: today...,
                            displayedComponents: [.date, .hourAndMinute]
                        )
                        .accessibilityIdentifier("create_flow.slot.end")
                        if let rangeError {
                            CreateFlowFieldError(message: localized(rangeError), accessibilityID: "create_flow.slot.range_error")
                        }
                    } else {
                        DatePicker(
                            String(localized: "create_flow.slot.day"),
                            selection: $day,
                            in: today...,
                            displayedComponents: .date
                        )
                        .accessibilityIdentifier("create_flow.slot.day")
                    }
                }
            }
            .navigationTitle(String(localized: "create_flow.slot.editor_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "common.add")) {
                        onAdd(slot)
                        dismiss()
                    }
                    .disabled(rangeError != nil)
                    .accessibilityIdentifier("create_flow.slot.confirm")
                }
            }
            .onChange(of: start) { _, newStart in
                if end <= newStart { end = newStart.addingTimeInterval(3 * 3600) }
            }
        }
    }
}

// MARK: - Éléments communs

struct CreateFlowSection<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: WK.Space.xs) {
            Text(title)
                .font(WK.Typo.caption.weight(.semibold))
                .foregroundStyle(WK.Colors.textSecondary)
                .accessibilityAddTraits(.isHeader)
            content()
        }
    }
}

struct CreateFlowTextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var error: String? = nil
    var multiline = false
    let accessibilityID: String

    var body: some View {
        VStack(alignment: .leading, spacing: WK.Space.xs) {
            Text(label)
                .font(WK.Typo.caption.weight(.semibold))
                .foregroundStyle(WK.Colors.textSecondary)
                .accessibilityHidden(true)
            field
                .textFieldStyle(.plain)
                .font(WK.Typo.body)
                .foregroundStyle(WK.Colors.textPrimary)
                .padding(.horizontal, WK.Space.sm)
                .padding(.vertical, WK.Space.xs)
                .frame(minHeight: WK.Size.minTapTarget)
                .background(WK.Colors.card, in: WK.shape(WK.Radius.md))
                .overlay {
                    if error != nil {
                        WK.shape(WK.Radius.md).strokeBorder(WK.Status.actionNeeded.color, lineWidth: WK.Stroke.emphasis)
                    }
                }
                .accessibilityLabel(label)
                .accessibilityIdentifier(accessibilityID)
            if let error {
                CreateFlowFieldError(message: localized(error), accessibilityID: "\(accessibilityID).error")
            }
        }
    }

    @ViewBuilder
    private var field: some View {
        if multiline {
            TextField(placeholder, text: $text, axis: .vertical)
                .lineLimit(3...8)
        } else {
            TextField(placeholder, text: $text)
        }
    }
}

/// Bouton « retirer » d'une ligne : cible ≥ 44 pt, libellé VoiceOver explicite.
struct CreateFlowRemoveButton: View {
    let accessibilityLabel: String
    var accessibilityID: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "xmark.circle.fill")
                .font(WK.Typo.headline)
                .foregroundStyle(WK.Colors.textSecondary)
                .frame(minWidth: WK.Size.minTapTarget, minHeight: WK.Size.minTapTarget)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
        .wkAccessibilityID(accessibilityID)
    }
}

/// Pastilles qui passent à la ligne (types, actions) : rien ne déborde en AX5.
struct CreateFlowChipLayout: Layout {
    var spacing: CGFloat = WK.Space.xs

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(subviews: subviews, maxWidth: proposal.width ?? .infinity)
        let width = rows.map(\.width).max() ?? 0
        let height = rows.map(\.height).reduce(0, +) + spacing * CGFloat(max(rows.count - 1, 0))
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for row in arrange(subviews: subviews, maxWidth: bounds.width) {
            var x = bounds.minX
            for item in row.items {
                subviews[item.index].place(
                    at: CGPoint(x: x, y: y),
                    proposal: ProposedViewSize(width: item.size.width, height: item.size.height)
                )
                x += item.size.width + spacing
            }
            y += row.height + spacing
        }
    }

    private struct Row {
        var items: [(index: Int, size: CGSize)] = []
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func arrange(subviews: Subviews, maxWidth: CGFloat) -> [Row] {
        var rows: [Row] = []
        var current = Row()
        for index in subviews.indices {
            var size = subviews[index].sizeThatFits(.unspecified)
            if size.width > maxWidth {
                size = subviews[index].sizeThatFits(ProposedViewSize(width: maxWidth, height: nil))
            }
            let proposedWidth = current.items.isEmpty ? size.width : current.width + spacing + size.width
            if !current.items.isEmpty, proposedWidth > maxWidth {
                rows.append(current)
                current = Row()
            }
            current.width = current.items.isEmpty ? size.width : current.width + spacing + size.width
            current.height = max(current.height, size.height)
            current.items.append((index, size))
        }
        if !current.items.isEmpty { rows.append(current) }
        return rows
    }
}
