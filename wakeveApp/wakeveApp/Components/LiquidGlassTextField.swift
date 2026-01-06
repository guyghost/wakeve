import SwiftUI

/// Liquid Glass Text Field Component
///
/// A reusable text input component following Apple's Liquid Glass guidelines.
/// Supports focus states, validation, and custom keyboard types.
///
/// ## Features
/// - Native iOS focus states with glass materials
/// - Title and placeholder labels
/// - Error states with visual feedback
/// - Secure text option for passwords
/// - Custom keyboard types
/// - Left and right accessory views
///
/// ## Usage Examples
/// ```swift
/// // Basic text field
/// LiquidGlassTextField(
///     title: "Email",
///     placeholder: "your@email.com",
///     text: $email
///     keyboardType: .emailAddress
/// )
///
/// // Text field with error
/// LiquidGlassTextField(
///     title: "Password",
///     placeholder: "Enter your password",
///     text: $password,
///     isSecure: true,
///     errorMessage: "Password must be at least 8 characters"
/// )
///
/// // Text field with left icon
/// LiquidGlassTextField(
///     title: "Search",
///     placeholder: "Search events...",
///     text: $searchText,
///     leftIcon: "magnifyingglass",
///     leftIconAction: { /* perform search */ }
/// )
///
/// // Disabled text field
/// LiquidGlassTextField(
///     title: "Username",
///     placeholder: "Enter your username",
///     text: $username,
///     isDisabled: true
/// )
/// ```

struct LiquidGlassTextField: View {
    let title: String?
    let placeholder: String
    @Binding var text: String
    let isSecure: Bool?
    let isDisabled: Bool?
    let keyboardType: UIKeyboardType?
    let errorMessage: String?
    let leftIcon: String?
    let rightIcon: String?
    let leftIconAction: (() -> Void)?
    let rightIconAction: (() -> Void)?
    
    @FocusState private var isFocused: Bool
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Title
            if let title = title {
                Text(title)
                    .font(.caption.weight(.medium))
                    .foregroundColor(.secondary)
            }
            
            // Text field container
            HStack(spacing: 0) {
                // Left icon
                if let leftIcon = leftIcon {
                    Button(action: leftIconAction ?? {}) {
                        Image(systemName: leftIcon)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(isDisabled == true ? .secondary : .primary)
                    }
                    .disabled(leftIconAction == nil)
                    .buttonStyle(.plain)
                }
                
                // Text field
                ZStack(alignment: .leading) {
                    if text.isEmpty {
                        Text(placeholder)
                            .font(.body)
                            .foregroundColor(isDisabled == true ? .secondary : .tertiary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 12)
                    }
                    
                    if !text.isEmpty {
                        if isSecure == true {
                            SecureField("", text: $text)
                                .font(.body)
                                .foregroundColor(isDisabled == true ? .secondary : .primary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 12)
                                .disabled(isDisabled == true)
                                .autocapitalization(.none)
                        } else {
                            TextField("", text: $text)
                                .font(.body)
                                .foregroundColor(isDisabled == true ? .secondary : .primary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 12)
                                .disabled(isDisabled == true)
                                .autocapitalization(.none)
                                .keyboardType(keyboardType ?? .default)
                        }
                    }
                    
                    // Focus indicator
                    if isFocused {
                        RoundedRectangle(cornerRadius: 2)
                            .frame(width: 3, height: 3)
                            .padding(2)
                    }
                }
                .frame(minHeight: 56)
                .background(isFocused ? .thinMaterial : .ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(
                            isFocused ? Color.wakevPrimary : (errorMessage != nil ? Color.secondary : .clear),
                            lineWidth: 1.5
                        )
                )
                .shadow(
                    color: isFocused ? Color.wakevPrimary.opacity(0.2) : .clear,
                    radius: isFocused ? 6 : 0,
                    x: 0,
                    y: isFocused ? 2 : 0
                )
                
                // Right icon
                if let rightIcon = rightIcon {
                    Button(action: rightIconAction ?? {}) {
                        Image(systemName: rightIcon)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(isDisabled == true ? .secondary : .primary)
                    }
                    .disabled(rightIconAction == nil)
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            
            // Error message
            if let errorMessage = errorMessage, !errorMessage.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.red)
                    
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                        .lineLimit(2)
                }
                .padding(.top, 4)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(title ?? placeholder)
        .accessibilityHint(isSecure == true ? "Password field" : "Text field")
    }
    
    // MARK: - Focus State Management
    
    private var backgroundColor: Material {
        isFocused ? .thinMaterial : .ultraThinMaterial
    }
    
    // MARK: - Validation Helper
    
    private var borderColor: Color {
        if let errorMessage = errorMessage, !errorMessage.isEmpty {
            return .red
        } else if isFocused {
            return .wakevPrimary
        } else {
            return .secondary
        }
    }
}

// MARK: - Preview

struct LiquidGlassTextField_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Basic Text Fields")
                    .font(.headline)
                
                VStack(spacing: 16) {
                    LiquidGlassTextField(
                        title: "Email",
                        placeholder: "your@email.com",
                        text: .constant(""),
                        keyboardType: .emailAddress
                    )
                    
                    LiquidGlassTextField(
                        title: "Password",
                        placeholder: "Enter your password",
                        text: .constant(""),
                        isSecure: true
                    )
                    
                    LiquidGlassTextField(
                        title: "Username",
                        placeholder: "Enter your username",
                        text: .constant(""),
                        isDisabled: true
                    )
                }
                
                Divider()
                    .padding(.vertical)
                
                Text("Text Fields with Icons")
                    .font(.headline)
                
                VStack(spacing: 16) {
                    LiquidGlassTextField(
                        title: "Search",
                        placeholder: "Search events...",
                        text: .constant(""),
                        leftIcon: "magnifyingglass",
                        leftIconAction: {}
                    )
                    
                    LiquidGlassTextField(
                        title: "Confirm Email",
                        placeholder: "Confirm your email",
                        text: .constant(""),
                        rightIcon: "checkmark.circle.fill",
                        rightIconAction: {}
                    )
                }
                
                Divider()
                    .padding(.vertical)
                
                Text("Text Fields with Errors")
                    .font(.headline)
                
                VStack(spacing: 16) {
                    LiquidGlassTextField(
                        title: "Email",
                        placeholder: "Enter your email",
                        text: .constant(""),
                        errorMessage: "Please enter a valid email address"
                    )
                    
                    LiquidGlassTextField(
                        title: "Password",
                        placeholder: "Enter your password",
                        text: .constant(""),
                        isSecure: true,
                        errorMessage: "Password must be at least 8 characters"
                    )
                }
            }
            .padding()
        }
    }
}
