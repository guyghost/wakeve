import SwiftUI

/// Typography tokens keep Wakeve native and readable by using SF Pro through SwiftUI Font styles.
public enum TypographyTokens {
    public static let screenTitle = Font.largeTitle.weight(.bold)
    public static let contentHero = Font.title.weight(.bold)
    public static let cardTitle = Font.title3.weight(.semibold)
    public static let headline = Font.headline
    public static let body = Font.body
    public static let bodyEmphasis = Font.body.weight(.semibold)
    public static let callout = Font.callout
    public static let metadata = Font.callout.weight(.medium)
    public static let caption = Font.caption
    public static let badge = Font.caption2.weight(.semibold)

    public static func lineLimitForCompactLabel(dynamicTypeSize: DynamicTypeSize) -> Int {
        dynamicTypeSize.isAccessibilitySize ? 2 : 1
    }

    public static func minimumScaleForCompactLabel(dynamicTypeSize: DynamicTypeSize) -> CGFloat {
        dynamicTypeSize.isAccessibilitySize ? 1 : 0.82
    }
}
