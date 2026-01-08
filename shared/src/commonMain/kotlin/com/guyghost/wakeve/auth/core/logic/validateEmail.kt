package com.guyghost.wakeve.auth.core.logic

import com.guyghost.wakeve.auth.core.models.AuthError

/**
 * Validates an email address format.
 * 
 * This function performs a RFC 5322-compliant email validation but is not
 * a full RFC validation - it checks the common email format patterns:
 * - Local part before @
 * - Domain part after @ with at least one dot
 * - No spaces or special characters in invalid positions
 * 
 * @param email The email address to validate
 * @return ValidationResult.success() if valid, ValidationResult.failure() with ValidationError if invalid
 * 
 * @example
 * ```
 * validateEmail("user@example.com") // success
 * validateEmail("user.name@company.co.uk") // success
 * validateEmail("invalid-email") // failure
 * validateEmail("@example.com") // failure
 * validateEmail("user@") // failure
 * ```
 */
fun validateEmail(email: String): ValidationResult {
    // Check for empty or null-like strings
    val trimmed = email.trim()
    if (trimmed.isEmpty()) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "L'adresse email est requise"
            )
        )
    }

    // Check minimum length (a@b.c = 5 chars)
    if (trimmed.length < 5) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "L'adresse email est trop courte"
            )
        )
    }

    // Check maximum length (RFC 5321: 254 chars)
    if (trimmed.length > 254) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "L'adresse email est trop longue"
            )
        )
    }

    // Split at the last @ to handle cases like "user@domain@domain.com"
    val atIndex = trimmed.lastIndexOf('@')
    if (atIndex <= 0) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "L'adresse email doit contenir un @"
            )
        )
    }

    if (atIndex == trimmed.length - 1) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "Le domaine est requis après @"
            )
        )
    }

    val localPart = trimmed.substring(0, atIndex)
    val domainPart = trimmed.substring(atIndex + 1)

    // Validate local part (part before @)
    if (localPart.length > 64) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "La partie locale de l'email est trop longue"
            )
        )
    }

    // Local part should not start or end with dot
    if (localPart.firstOrNull() == '.' || localPart.lastOrNull() == '.') {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "L'email ne peut pas commencer ou finir par un point"
            )
        )
    }

    // Validate domain part (part after @)
    if (domainPart.length > 253) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "Le domaine est trop long"
            )
        )
    }

    // Domain should contain at least one dot (except for single-label domains)
    // But we should have at least one dot for common domains
    if (!domainPart.contains('.')) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "Le domaine doit contenir un point (ex: example.com)"
            )
        )
    }

    // Check for invalid characters using regex
    // Allowed: alphanumerics, dots, hyphens, underscores, plus signs
    val validLocalChars = Regex("^[a-zA-Z0-9._+-]+$")
    if (!validLocalChars.matches(localPart)) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "L'adresse email contient des caractères invalides"
            )
        )
    }

    // Domain validation: alphanumerics, hyphens, dots
    val validDomainChars = Regex("^[a-zA-Z0-9.-]+$")
    if (!validDomainChars.matches(domainPart)) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "Le domaine contient des caractères invalides"
            )
        )
    }

    // Check for consecutive dots anywhere
    if (trimmed.contains("..")) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "L'email ne peut pas contenir de points consécutifs"
            )
        )
    }

    // Check for dot at start or end of domain
    if (domainPart.firstOrNull() == '.' || domainPart.lastOrNull() == '.') {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "Le domaine ne peut pas commencer ou finir par un point"
            )
        )
    }

    // Common disposable email domains (basic list)
    val disposableDomains = listOf(
        "tempmail.com", "throwaway.email", "mailinator.com",
        "guerrillamail.com", "10minutemail.com", "sharklasers.com"
    )
    val domainLower = domainPart.lowercase()
    if (disposableDomains.any { domainLower.endsWith(it) }) {
        return ValidationResult.failure(
            AuthError.ValidationError(
                field = "email",
                message = "Les emails temporaires ne sont pas autorisés"
            )
        )
    }

    return ValidationResult.success()
}

/**
 * Result type for validation functions.
 */
sealed class ValidationResult {
    data object Success : ValidationResult()
    
    data class Failure(
        val error: AuthError.ValidationError
    ) : ValidationResult()

    companion object {
        fun success(): ValidationResult = Success
        fun failure(error: AuthError.ValidationError): ValidationResult = Failure(error)
    }

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    val errorOrNull: AuthError.ValidationError?
        get() = (this as? Failure)?.error
}
