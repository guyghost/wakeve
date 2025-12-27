package com.guyghost.wakeve.payment

/**
 * Exceptions pour le service de paiement
 */

open class PaymentException(message: String) : Exception(message)

class EventNotFoundException(eventId: String) : PaymentException("Event not found: $eventId")

class NoActivePotException(eventId: String) : PaymentException("No active pot found for event: $eventId")

class PotNotFoundException(potId: String) : PaymentException("Pot not found: $potId")

class TricountNotConfiguredException : PaymentException("Tricount not configured for this pot")

class PaymentProviderException(message: String) : PaymentException("Payment provider error: $message")