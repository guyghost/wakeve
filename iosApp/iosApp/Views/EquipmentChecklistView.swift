import SwiftUI

// MARK: - Models

struct EquipmentItemModel: Identifiable {
    let id: String
    var eventId: String
    var name: String
    var description: String
    var category: EquipmentCategory
    var quantity: Int
    var assignedTo: String?
    var status: ItemStatus
    var estimatedCost: Int64
    var sharedItem: Bool
    var createdAt: String
    var updatedAt: String
}

enum EquipmentCategory: String, CaseIterable {
    case camping = "CAMPING"
    case cooking = "COOKING"
    case clothing = "CLOTHING"
    case sport = "SPORT"
    case entertainment = "ENTERTAINMENT"
    case hygiene = "HYGIENE"
    case medical = "MEDICAL"
    case safety = "SAFETY"
    case electronics = "ELECTRONICS"
    case other = "OTHER"
    
    var label: String {
        switch self {
        case .camping: return "Camping"
        case .cooking: return "Cuisine"
        case .clothing: return "Vêtements"
        case .sport: return "Sport"
        case .entertainment: return "Divertissement"
        case .hygiene: return "Hygiène"
        case .medical: return "Médical"
        case .safety: return "Sécurité"
        case .electronics: return "Électronique"
        case .other: return "Autre"
        }
    }
}

enum ItemStatus: String, CaseIterable {
    case needed = "NEEDED"
    case assigned = "ASSIGNED"
    case confirmed = "CONFIRMED"
    case packed = "PACKED"
    case cancelled = "CANCELLED"
    
    var label: String {
        switch self {
        case .needed: return "Requis"
        case .assigned: return "Assigné"
        case .confirmed: return "Confirmé"
        case .packed: return "Emballé"
        case .cancelled: return "Annulé"
        }
    }
    
    var color: Color {
        switch self {
        case .needed: return .gray
        case .assigned: return .blue
        case .confirmed: return .purple
        case .packed: return .green
        case .cancelled: return .red
        }
    }
}

struct EquipmentStats {
    let totalItems: Int
    let neededItems: Int
    let assignedItems: Int
    let confirmedItems: Int
    let packedItems: Int
    let totalCost: Int64
    let costPerPerson: Int64
    let progress: Double
}

enum ItemStatusFilter: String, CaseIterable {
    case all = "all"
    case needed = "NEEDED"
    case assigned = "ASSIGNED"
    case confirmed = "CONFIRMED"
    case packed = "PACKED"
    case cancelled = "CANCELLED"
    
    var label: String {
        switch self {
        case .all: return "Tous"
        case .needed: return "Requis"
        case .assigned: return "Assignés"
        case .confirmed: return "Confirmés"
        case .packed: return "Emballés"
        case .cancelled: return "Annulés"
        }
    }
    
    var icon: String {
        switch self {
        case .all: return "square.grid.2x2"
        case .needed: return "circle"
        case .assigned: return "person"
        case .confirmed: return "checkmark.circle"
        case .packed: return "checkmark.circle.fill"
        case .cancelled: return "xmark.circle"
        }
    }
}

// MARK: - Form Sheets

struct EquipmentItemFormSheet: View {
    let eventId: String
    let item: EquipmentItemModel?
    let participants: [ParticipantModel]
    let onSave: (EquipmentItemModel) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var name: String
    @State private var description: String
    @State private var category: EquipmentCategory
    @State private var quantity: String
    @State private var estimatedCost: String
    @State private var assignedTo: String?
    @State private var status: ItemStatus
    @State private var sharedItem: Bool
    
    init(eventId: String, item: EquipmentItemModel?, participants: [ParticipantModel], onSave: @escaping (EquipmentItemModel) -> Void) {
        self.eventId = eventId
        self.item = item
        self.participants = participants
        self.onSave = onSave
        
        _name = State(initialValue: item?.name ?? "")
        _description = State(initialValue: item?.description ?? "")
        _category = State(initialValue: item?.category ?? .other)
        _quantity = State(initialValue: item != nil ? "\(item!.quantity)" : "1")
        _estimatedCost = State(initialValue: item != nil ? "\(item!.estimatedCost / 100)" : "")
        _assignedTo = State(initialValue: item?.assignedTo)
        _status = State(initialValue: item?.status ?? .needed)
        _sharedItem = State(initialValue: item?.sharedItem ?? false)
    }
    
    var isValid: Bool {
        !name.isEmpty && Int(quantity) != nil && Int(quantity)! > 0
    }
    
    var body: some View {
        NavigationView {
            Form {
                Section("Informations générales") {
                    TextField("Nom *", text: $name)
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(3...5)
                }
                
                Section("Quantité et coût") {
                    HStack {
                        Text("Quantité")
                        Spacer()
                        TextField("1", text: $quantity)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 60)
                    }
                    
                    HStack {
                        Text("Coût estimé (€)")
                        Spacer()
                        TextField("0", text: $estimatedCost)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 80)
                    }
                }
                
                Section("Catégorie") {
                    Picker("Catégorie", selection: $category) {
                        ForEach(EquipmentCategory.allCases, id: \.self) { cat in
                            Text(cat.label).tag(cat)
                        }
                    }
                }
                
                Section("Statut") {
                    Picker("Statut", selection: $status) {
                        ForEach(ItemStatus.allCases, id: \.self) { st in
                            Text(st.label).tag(st)
                        }
                    }
                }
                
                Section("Attribution") {
                    Picker("Assigné à", selection: $assignedTo) {
                        Text("Non assigné").tag(nil as String?)
                        ForEach(participants) { participant in
                            Text(participant.name).tag(participant.id as String?)
                        }
                    }
                }
                
                Section {
                    Toggle("Équipement partagé", isOn: $sharedItem)
                }
            }
            .navigationTitle(item == nil ? "Ajouter" : "Modifier")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(item == nil ? "Ajouter" : "Modifier") {
                        saveItem()
                    }
                    .disabled(!isValid)
                    .fontWeight(.semibold)
                }
            }
        }
    }
    
    private func saveItem() {
        let costInCents = Int64((Double(estimatedCost) ?? 0) * 100)
        let now = ISO8601DateFormatter().string(from: Date())
        
        let updatedItem = EquipmentItemModel(
            id: item?.id ?? UUID().uuidString,
            eventId: eventId,
            name: name.trimmingCharacters(in: .whitespaces),
            description: description.trimmingCharacters(in: .whitespaces),
            category: category,
            quantity: Int(quantity) ?? 1,
            assignedTo: assignedTo,
            status: status,
            estimatedCost: costInCents,
            sharedItem: sharedItem,
            createdAt: item?.createdAt ?? now,
            updatedAt: now
        )
        
        onSave(updatedItem)
    }
}

struct AssignItemSheet: View {
    let item: EquipmentItemModel
    let participants: [ParticipantModel]
    let onAssign: (String?) -> Void
    
    @Environment(\.dismiss) private var dismiss
    @State private var selectedParticipant: String?
    
    init(item: EquipmentItemModel, participants: [ParticipantModel], onAssign: @escaping (String?) -> Void) {
        self.item = item
        self.participants = participants
        self.onAssign = onAssign
        _selectedParticipant = State(initialValue: item.assignedTo)
    }
    
    var body: some View {
        NavigationView {
            List {
                Section {
                    Button {
                        selectedParticipant = nil
                    } label: {
                        HStack {
                            Text("Non assigné")
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedParticipant == nil {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                }
                
                Section {
                    ForEach(participants) { participant in
                        Button {
                            selectedParticipant = participant.id
                        } label: {
                            HStack {
                                Text(participant.name)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedParticipant == participant.id {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Assigner")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Confirmer") {
                        onAssign(selectedParticipant)
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
}

struct AutoGenerateEquipmentSheet: View {
    let eventId: String
    let participantCount: Int
    let onGenerate: ([EquipmentItemModel]) -> Void
    
    @Environment(\.dismiss) private var dismiss
    @State private var selectedType: String = "camping"
    
    let eventTypes: [(key: String, label: String)] = [
        ("camping", "Camping 🏕️"),
        ("beach", "Plage 🏖️"),
        ("ski", "Ski / Montagne ⛷️"),
        ("hiking", "Randonnée 🥾"),
        ("city", "Ville / Urbain 🏙️"),
        ("bbq", "BBQ / Pique-nique 🍖"),
        ("road_trip", "Road Trip 🚗"),
        ("festival", "Festival 🎪")
    ]
    
    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Sélectionnez le type d'événement :")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
                    .padding(.top)
                
                List(eventTypes, id: \.key) { type in
                    Button {
                        selectedType = type.key
                    } label: {
                        HStack {
                            Text(type.label)
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedType == type.key {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.blue)
                            } else {
                                Image(systemName: "circle")
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Générer une liste")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Générer") {
                        generateItems()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
    
    private func generateItems() {
        // TODO: Call EquipmentManager from Kotlin
        onGenerate([])
    }
}

// MARK: - Main View

/**
 * Equipment Checklist View (iOS)
 * 
 * Features:
 * - List equipment items grouped by category
 * - Filter by status
 * - Assign items to participants
 * - Update item status with checkboxes
 * - Auto-generate checklist from event type
 * - Display statistics and progress
 * - Liquid Glass design system
 */
struct EquipmentChecklistView: View {
    let eventId: String
    let currentUserId: String
    let currentUserName: String
    @Environment(\.dismiss) private var dismiss
    
    @State private var equipmentItems: [EquipmentItemModel] = []
    @State private var participants: [ParticipantModel] = []
    @State private var isLoading = false
    
    // Filters
    @State private var selectedStatusFilter: ItemStatusFilter = .all
    
    // Sheets
    @State private var showAddItemSheet = false
    @State private var showAutoGenerateSheet = false
    @State private var showAssignSheet = false
    @State private var selectedItem: EquipmentItemModel?
    @State private var itemToAssign: EquipmentItemModel?
    
    // Alerts
    @State private var showDeleteAlert = false
    @State private var itemToDelete: EquipmentItemModel?
    
    // Comments state
    @State private var commentCount = 0
    @State private var showComments = false
    
    var filteredItems: [EquipmentItemModel] {
        if selectedStatusFilter == .all {
            return equipmentItems
        }
        return equipmentItems.filter { $0.status.rawValue == selectedStatusFilter.rawValue }
    }
    
    var itemsByCategory: [(category: EquipmentCategory, items: [EquipmentItemModel])] {
        let grouped = Dictionary(grouping: filteredItems, by: { $0.category })
        return grouped.sorted { $0.key.rawValue < $1.key.rawValue }
            .map { ($0.key, $0.value) }
    }
    
    var stats: EquipmentStats {
        calculateStats(items: equipmentItems)
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background gradient
                LinearGradient(
                    colors: [Color(uiColor: .systemBackground), Color(uiColor: .systemGray6)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
                
                if isLoading {
                    ProgressView()
                } else {
                    mainContent
                }
            }
            .navigationTitle("Équipement")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fermer") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 12) {
                        CommentButton(commentCount: commentCount) {
                            showComments = true
                        }
                        
                        Menu {
                            Button {
                                selectedItem = nil
                                showAddItemSheet = true
                            } label: {
                                Label("Ajouter un équipement", systemImage: "plus.circle")
                            }
                            
                            Button {
                                showAutoGenerateSheet = true
                            } label: {
                                Label("Générer automatiquement", systemImage: "wand.and.stars")
                            }
                            
                            Button {
                                balanceAssignments()
                            } label: {
                                Label("Rééquilibrer", systemImage: "scale.3d")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle.fill")
                                .font(.title2)
                        }
                    }
                }
            }
            .sheet(isPresented: $showAddItemSheet) {
                EquipmentItemFormSheet(
                    eventId: eventId,
                    item: selectedItem,
                    participants: participants,
                    onSave: { item in
                        if let index = equipmentItems.firstIndex(where: { $0.id == item.id }) {
                            equipmentItems[index] = item
                        } else {
                            equipmentItems.append(item)
                        }
                        showAddItemSheet = false
                        selectedItem = nil
                    }
                )
            }
            .sheet(isPresented: $showAutoGenerateSheet) {
                AutoGenerateEquipmentSheet(
                    eventId: eventId,
                    participantCount: participants.count,
                    onGenerate: { items in
                        equipmentItems.append(contentsOf: items)
                        showAutoGenerateSheet = false
                    }
                )
            }
            .sheet(item: $itemToAssign) { item in
                AssignItemSheet(
                    item: item,
                    participants: participants,
                    onAssign: { participantId in
                        if let index = equipmentItems.firstIndex(where: { $0.id == item.id }) {
                            var updated = equipmentItems[index]
                            updated.assignedTo = participantId
                            updated.status = participantId != nil ? .assigned : .needed
                            equipmentItems[index] = updated
                        }
                        itemToAssign = nil
                    }
                )
            }
            .sheet(isPresented: $showComments) {
                NavigationView {
                    CommentsView(
                        eventId: eventId,
                        section: .EQUIPMENT,
                        sectionItemId: nil,
                        currentUserId: currentUserId,
                        currentUserName: currentUserName
                    )
                }
            }
            .alert("Supprimer l'équipement", isPresented: $showDeleteAlert, presenting: itemToDelete) { item in
                Button("Supprimer", role: .destructive) {
                    equipmentItems.removeAll { $0.id == item.id }
                    itemToDelete = nil
                }
                Button("Annuler", role: .cancel) {
                    itemToDelete = nil
                }
            } message: { item in
                Text("Voulez-vous vraiment supprimer « \(item.name) » ?")
            }
            .onAppear {
                loadData()
                loadCommentCount()
            }
        }
    }
    
    @ViewBuilder
    private var mainContent: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Statistics Card
                statsCard
                
                // Filter Chips
                filterChips
                
                // Equipment List by Category
                if filteredItems.isEmpty {
                    emptyStateCard
                } else {
                    ForEach(itemsByCategory, id: \.category) { categoryGroup in
                        categorySection(category: categoryGroup.category, items: categoryGroup.items)
                    }
                }
            }
            .padding()
        }
    }
    
    @ViewBuilder
    private var statsCard: some View {
        VStack(spacing: 12) {
            Text("Progression")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            // Progress Bar
            VStack(alignment: .leading, spacing: 4) {
                ProgressView(value: stats.progress)
                    .tint(Color.blue)
                
                Text("\(stats.packedItems) / \(stats.totalItems) équipements emballés (\(Int(stats.progress * 100))%)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            // Status Summary
            HStack(spacing: 20) {
                VStack {
                    Text("\(stats.neededItems)")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("Requis")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                VStack {
                    Text("\(stats.assignedItems)")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                    Text("Assignés")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                VStack {
                    Text("\(stats.confirmedItems)")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.purple)
                    Text("Confirmés")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity)
            
            Divider()
            
            // Cost Summary
            HStack {
                VStack(alignment: .leading) {
                    Text("Coût total")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(stats.totalCost / 100)€")
                        .font(.title3)
                        .fontWeight(.bold)
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    Text("Par personne")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(stats.costPerPerson / 100)€")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                }
            }
        }
        .padding()
        .glassCard()
    }
    
     @ViewBuilder
     private var filterChips: some View {
         ScrollView(.horizontal, showsIndicators: false) {
             HStack(spacing: 8) {
                 ForEach(ItemStatusFilter.allCases, id: \.self) { filter in
                     filterChipButton(for: filter)
                 }
             }
         }
     }
    
    @ViewBuilder
    private func filterChipButton(for filter: ItemStatusFilter) -> some View {
        let isSelected = selectedStatusFilter == filter
        let backgroundColor = isSelected ? Color.blue.opacity(0.15) : Color.clear
        let foregroundColor = isSelected ? Color.blue : Color.primary
        let shadowOpacity = isSelected ? 0.08 : 0.03
        let shadowRadius: CGFloat = isSelected ? 4 : 2
        
        Button {
            selectedStatusFilter = filter
        } label: {
            HStack(spacing: 4) {
                Image(systemName: filter.icon)
                    .font(.caption)
                Text(filter.label)
                    .font(.caption)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(backgroundColor)
            .background(.ultraThinMaterial)
            .foregroundColor(foregroundColor)
            .continuousCornerRadius(12)
            .shadow(
                color: .black.opacity(shadowOpacity),
                radius: shadowRadius,
                x: 0,
                y: 2
            )
        }
    }
    
     @ViewBuilder
     private func categorySection(category: EquipmentCategory, items: [EquipmentItemModel]) -> some View {
         VStack(alignment: .leading, spacing: 12) {
             HStack {
                 Text(category.label)
                     .font(.headline)
                 
                 Spacer()
                 
                 GlassBadge(
                     text: "\(items.count)",
                     icon: nil,
                     color: .blue,
                     style: .filled
                 )
             }
            
            ForEach(items) { item in
                itemRow(item: item)
                
                if item.id != items.last?.id {
                    Divider()
                }
            }
        }
        .padding()
        .glassCard()
    }
    
    @ViewBuilder
    private func itemRow(item: EquipmentItemModel) -> some View {
        HStack(alignment: .top, spacing: 12) {
            // Status Checkbox
            Button {
                togglePacked(item: item)
            } label: {
                Image(systemName: item.status == .packed ? "checkmark.square.fill" : "square")
                    .font(.title2)
                    .foregroundColor(item.status == .packed ? .green : .secondary)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(item.name)
                        .font(.body)
                        .fontWeight(.medium)
                    
                    if item.quantity > 1 {
                        Text("× \(item.quantity)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                if !item.description.isEmpty {
                    Text(item.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                 HStack(spacing: 8) {
                     // Status Badge
                     GlassBadge(
                         text: item.status.label,
                         icon: nil,
                         color: item.status.color,
                         style: .filled
                     )
                     
                     // Assigned Person
                     if let assignedId = item.assignedTo,
                        let participant = participants.first(where: { $0.id == assignedId }) {
                         Button {
                             itemToAssign = item
                         } label: {
                             GlassBadge(
                                 text: participant.name,
                                 icon: "person.fill",
                                 color: .blue,
                                 style: .filled
                             )
                         }
                     } else {
                         Button {
                             itemToAssign = item
                         } label: {
                             GlassBadge(
                                 text: "Assigner",
                                 icon: "person.badge.plus",
                                 color: .secondary,
                                 style: .glass
                             )
                         }
                     }
                    
                    // Cost
                    if item.estimatedCost > 0 {
                        Text("\(item.estimatedCost / 100)€")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                }
                
                 // Edit and Delete buttons
                 HStack(spacing: 8) {
                     Button {
                         selectedItem = item
                         showAddItemSheet = true
                     } label: {
                         GlassBadge(
                             text: "Modifier",
                             icon: "pencil",
                             color: .blue,
                             style: .glass
                         )
                     }
                     
                     Button {
                         itemToDelete = item
                         showDeleteAlert = true
                     } label: {
                         GlassBadge(
                             text: "Supprimer",
                             icon: "trash",
                             color: .red,
                             style: .filled
                         )
                     }
                 }
            }
            
            Spacer()
        }
    }
    
    @ViewBuilder
    private var emptyStateCard: some View {
        VStack(spacing: 16) {
            Image(systemName: "shippingbox.fill")
                .font(.system(size: 64))
                .foregroundColor(.secondary)
            
            Text(equipmentItems.isEmpty ? "Aucun équipement ajouté" : "Aucun équipement avec ce statut")
                .font(.body)
                .foregroundColor(.secondary)
            
            if equipmentItems.isEmpty {
                Button("Générer une liste") {
                    showAutoGenerateSheet = true
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(32)
        .glassCard()
    }
    
    // MARK: - Helper Methods
    
    private func loadData() {
        // TODO: Load from repository
        isLoading = true
        // Simulate loading
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isLoading = false
        }
    }
    
    // MARK: - Comments
    
    private func loadCommentCount() {
        // TODO: Integrate with CommentRepository
        // For now, placeholder - should fetch count for section .EQUIPMENT and sectionItemId = nil
        commentCount = 0
    }
    
    private func togglePacked(item: EquipmentItemModel) {
        if let index = equipmentItems.firstIndex(where: { $0.id == item.id }) {
            var updated = equipmentItems[index]
            if updated.status == .packed {
                updated.status = updated.assignedTo != nil ? .confirmed : .needed
            } else {
                updated.status = .packed
            }
            equipmentItems[index] = updated
        }
    }
    
    private func balanceAssignments() {
        // TODO: Implement balancing logic
    }
    
    private func calculateStats(items: [EquipmentItemModel]) -> EquipmentStats {
        let total = items.count
        let needed = items.filter { $0.status == .needed }.count
        let assigned = items.filter { $0.status == .assigned }.count
        let confirmed = items.filter { $0.status == .confirmed }.count
        let packed = items.filter { $0.status == .packed }.count
        let totalCost = items.reduce(0) { $0 + $1.estimatedCost }
        let costPerPerson = participants.isEmpty ? 0 : totalCost / Int64(participants.count)
        let progress = total > 0 ? Double(packed) / Double(total) : 0.0
        
        return EquipmentStats(
            totalItems: total,
            neededItems: needed,
            assignedItems: assigned,
            confirmedItems: confirmed,
            packedItems: packed,
            totalCost: totalCost,
            costPerPerson: costPerPerson,
            progress: progress
        )
    }
}

// MARK: - Preview

struct EquipmentChecklistView_Previews: PreviewProvider {
    static var previews: some View {
        EquipmentChecklistView(
            eventId: "event-1",
            currentUserId: "user-1",
            currentUserName: "Test User"
        )
    }
}
