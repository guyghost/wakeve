import SwiftUI

// MARK: - Shared Reusable Components for Wakeve iOS

/// Info row with icon, text and color
struct InfoRow: View {
    let icon: String
    let text: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundColor(color)
                .frame(width: 20)
            
            Text(text)
                .font(.subheadline)
                .foregroundColor(.primary)
            
            Spacer()
        }
    }
}

/// Status badge with color coding
struct StatusBadge: View {
    let status: String
    
    var body: some View {
        Text(statusText)
            .font(.caption)
            .fontWeight(.medium)
            .foregroundColor(statusColor)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(statusColor.opacity(0.15))
            .continuousCornerRadius(12)
    }
    
    private var statusText: String {
        switch status {
        case "PLANNED": return "Planifié"
        case "ASSIGNED": return "Assigné"
        case "IN_PROGRESS": return "En cours"
        case "COMPLETED": return "Terminé"
        case "CANCELLED": return "Annulé"
        default: return status
        }
    }
    
    private var statusColor: Color {
        switch status {
        case "PLANNED": return .blue
        case "ASSIGNED": return .purple
        case "IN_PROGRESS": return .orange
        case "COMPLETED": return .green
        case "CANCELLED": return .red
        default: return .gray
        }
    }
}

/// Filter chip with icon and selection state
struct FilterChip: View {
    let text: String
    let icon: String
    let isSelected: Bool
    
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.caption)
            Text(text)
                .font(.subheadline)
                .fontWeight(.medium)
        }
        .foregroundColor(isSelected ? .white : .primary)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(isSelected ? Color.blue : Color(uiColor: .systemGray5))
        .continuousCornerRadius(20)
    }
}

/// Vote button for poll voting (Yes/Maybe/No)
struct VoteButton: View {
    let vote: PollVote
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(isSelected ? voteColor(for: vote) : Color.white.opacity(0.2))
                        .frame(width: 50, height: 50)
                        .overlay(
                            Circle()
                                .stroke(isSelected ? voteColor(for: vote) : Color.white.opacity(0.3), lineWidth: 2)
                        )
                    
                    Text(voteSymbol(for: vote))
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(isSelected ? .white : .white.opacity(0.8))
                }
                
                Text(voteLabel(for: vote))
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundColor(isSelected ? voteColor(for: vote) : .white.opacity(0.8))
            }
        }
    }
    
    private func voteColor(for vote: PollVote) -> Color {
        switch vote {
        case .yes: return .green
        case .maybe: return .orange
        case .no: return .red
        }
    }
    
    private func voteSymbol(for vote: PollVote) -> String {
        switch vote {
        case .yes: return "✓"
        case .maybe: return "~"
        case .no: return "✗"
        }
    }
    
    private func voteLabel(for vote: PollVote) -> String {
        switch vote {
        case .yes: return "Yes"
        case .maybe: return "Maybe"
        case .no: return "No"
        }
    }
}

/// Vote enum for poll responses
enum PollVote: String, Codable {
    case yes = "YES"
    case maybe = "MAYBE"
    case no = "NO"
}

// MARK: - Preview Support

struct SharedComponents_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            InfoRow(icon: "calendar", text: "Date: 25 Dec", color: .blue)
            
            StatusBadge(status: "PLANNED")
            StatusBadge(status: "COMPLETED")
            
            FilterChip(text: "Tous", icon: "square.grid.2x2", isSelected: true)
            FilterChip(text: "Actifs", icon: "circle", isSelected: false)
            
            HStack(spacing: 20) {
                VoteButton(vote: .yes, isSelected: true, action: {})
                VoteButton(vote: .maybe, isSelected: false, action: {})
                VoteButton(vote: .no, isSelected: false, action: {})
            }
        }
        .padding()
        .background(Color(uiColor: .systemBackground))
    }
}
