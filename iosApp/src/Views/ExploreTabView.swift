import SwiftUI

// Import the Wakev color extensions

/// Explore Tab View with suggestions, ideas, and new features
struct ExploreTabView: View {
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    DailySuggestionSection()
                    EventIdeasSection()
                    NewFeaturesSection()
                }
                .padding()
            }
            .navigationTitle("Explorer")
        }
    }
}

// MARK: - Liquid Glass Card (Simplified version for local use)
struct ExploreLiquidGlassCard<Content: View>: View {
    let content: Content
    let cornerRadius: CGFloat
    
    init(
        cornerRadius: CGFloat = 20,
        @ViewBuilder content: () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.content = content()
    }
    
    var body: some View {
        if #available(iOS 26.0, *) {
            content
                .padding()
                .glassEffect()
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        } else {
            // Fallback for iOS < 26
            content
                .padding()
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
        }
    }
}

// MARK: - Liquid Glass Button (Simplified version for local use)
struct ExploreLiquidGlassButton: View {
    let title: String
    let icon: String?
    let action: () -> Void
    
    init(
        _ title: String,
        icon: String? = nil,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon = icon {
                    Image(systemName: icon)
                }
                Text(title)
            }
            .font(.headline)
            .foregroundColor(.primary)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .exploreLiquidGlassButtonStyle()
    }
}

// MARK: - Button Style with Liquid Glass
struct ExploreLiquidGlassButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            configuration.label
                .glassEffect(.regular.interactive())
                .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
                .animation(.spring(response: 0.2, dampingFraction: 0.7), value: configuration.isPressed)
        } else {
            // Fallback for iOS < 26
            configuration.label
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
                .animation(.spring(response: 0.2, dampingFraction: 0.7), value: configuration.isPressed)
        }
    }
}

extension View {
    func exploreLiquidGlassButtonStyle() -> some View {
        self.buttonStyle(ExploreLiquidGlassButtonStyle())
    }
}

// MARK: - Daily Suggestion Section

struct DailySuggestionSection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Suggestion du jour")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            ExploreLiquidGlassCard {
                VStack(alignment: .leading, spacing: 16) {
                    // Large icon for the featured suggestion
Image(systemName: "sparkles")
                        .font(.system(size: 48))
                        .foregroundColor(.wakevAccent)
                        .frame(maxWidth: .infinity, alignment: .center)
                    
                    // Description
                    Text("Découvrez de nouvelles expériences")
                        .font(.title3)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.center)
                    
                    Text("Trouvez l'inspiration pour vos prochains événements grâce à nos suggestions personnalisées.")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(3)
                    
                    // Call to action button
                    ExploreLiquidGlassButton("Créer ce type d'événement") {
                        // Action to create this type of event
                        print("Creating featured event type")
                    }
                    .frame(maxWidth: .infinity)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}

// MARK: - Event Ideas Section

struct EventIdeasSection: View {
    let eventIdeas = [
        EventIdea(title: "Week-end entre amis", 
                  description: "Planifiez un week-end mémorable avec vos amis les plus proches", 
                  icon: "sun.max.fill", 
                  action: "Créer"),
        EventIdea(title: "Team building", 
                  description: "Organisez une journée de cohésion d'équipe inoubliable", 
                  icon: "person.3.fill", 
                  action: "Créer"),
        EventIdea(title: "Anniversaire", 
                  description: "Célébrez un anniversaire spécial avec style", 
                  icon: "cake.fill", 
                  action: "Créer"),
        EventIdea(title: "Soirée", 
                  description: "Organisez une soirée réussie avec vos proches", 
                  icon: "party.popper.fill", 
                  action: "Créer")
    ]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Idées d'événements")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            ForEach(eventIdeas, id: \.title) { idea in
                EventIdeaCard(idea: idea)
            }
        }
    }
}

// MARK: - Event Idea Data Model

struct EventIdea: Identifiable {
    let id = UUID()
    let title: String
    let description: String
    let icon: String
    let action: String
    
    static let empty = EventIdea(title: "", description: "", icon: "questionmark", action: "")
}

// MARK: - Event Idea Card

struct EventIdeaCard: View {
    let idea: EventIdea
    
    var body: some View {
        ExploreLiquidGlassCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: idea.icon)
                        .font(.title2)
                        .foregroundColor(.blue)
                        .frame(width: 30)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(idea.title)
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        Text(idea.description)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }
                    
                    Spacer()
                }
                
                ExploreLiquidGlassButton(idea.action) {
                    // Action to create this event idea
                    print("Creating \(idea.title)")
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

// MARK: - New Features Section

struct NewFeaturesSection: View {
    let features = [
        Feature(title: "Design Liquid Glass", 
                description: "Découvrez l'interface moderne avec des effets de verre translucide", 
                icon: "sparkles"),
        Feature(title: "Navigation par tabs", 
                description: "Naviguez facilement entre les différentes sections de l'application", 
                icon: "rectangle.3.group.fill"),
        Feature(title: "Cartes interactives", 
                description: "Explorez les suggestions avec des cartes interactives et animées", 
                icon: "rectangle.3.offgrid.bubble.left.fill")
    ]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Nouvelles fonctionnalités")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            ForEach(features, id: \.title) { feature in
                FeatureCard(feature: feature)
            }
        }
    }
}

// MARK: - Feature Data Model

struct Feature: Identifiable {
    let id = UUID()
    let title: String
    let description: String
    let icon: String
}

// MARK: - Feature Card

struct FeatureCard: View {
    let feature: Feature
    
    var body: some View {
        ExploreLiquidGlassCard {
            HStack(spacing: 12) {
                Image(systemName: feature.icon)
                    .font(.title3)
                    .foregroundColor(.purple)
                    .frame(width: 25)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(feature.title)
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    Text(feature.description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                
                Spacer()
            }
        }
    }
}

// MARK: - Previews

#Preview("ExploreTabView Light") {
    ExploreTabView()
        .preferredColorScheme(.light)
}

#Preview("ExploreTabView Dark") {
    ExploreTabView()
        .preferredColorScheme(.dark)
}

#Preview("Daily Suggestion Section") {
    DailySuggestionSection()
}

#Preview("Event Ideas Section") {
    ScrollView {
        EventIdeasSection()
    }
}