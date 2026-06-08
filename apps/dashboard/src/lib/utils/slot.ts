import type { CreateTimeSlotRequest, TimeOfDay, TimeSlotResponse } from '$lib/types/api'

export interface WizardSlot {
  id: string
  timeOfDay: TimeOfDay
  date: string
  startTime: string
  endTime: string
  errors: string[]
}

/**
 * Validate a single wizard slot and return an array of error messages.
 * An empty array means the slot is valid.
 */
export function validateSlot(slot: WizardSlot): string[] {
  const errors: string[] = []

  if (!slot.date) {
    errors.push('La date est requise')
  }

  if (slot.timeOfDay === 'SPECIFIC') {
    if (!slot.startTime) {
      errors.push("L'heure de début est requise")
    }
    if (!slot.endTime) {
      errors.push("L'heure de fin est requise")
    }
    if (slot.startTime && slot.endTime && slot.endTime <= slot.startTime) {
      errors.push('La fin doit être après le début')
    }
  }

  return errors
}

/**
 * Convert a WizardSlot into the CreateTimeSlotRequest shape expected by the API.
 * For non-SPECIFIC time-of-day slots the start/end are null.
 */
export function buildSlotRequest(slot: WizardSlot): CreateTimeSlotRequest {
  if (slot.timeOfDay !== 'SPECIFIC' || !slot.date) {
    return { start: null, end: null, timeOfDay: slot.timeOfDay }
  }

  return {
    start:
      slot.date && slot.startTime
        ? new Date(`${slot.date}T${slot.startTime}`).toISOString()
        : null,
    end:
      slot.date && slot.endTime
        ? new Date(`${slot.date}T${slot.endTime}`).toISOString()
        : null,
    timeOfDay: slot.timeOfDay
  }
}

const TIME_OF_DAY_LABELS: Record<TimeOfDay, string> = {
  ALL_DAY: 'Toute la journée',
  MORNING: 'Matin',
  AFTERNOON: 'Après-midi',
  EVENING: 'Soirée',
  SPECIFIC: 'Horaire précis'
}

/**
 * Build a human-readable label for a TimeSlotResponse.
 * Prefers the slot's own label field, falls back to time-of-day description,
 * and for SPECIFIC slots shows the formatted start→end times.
 */
export function formatSlotLabel(slot: TimeSlotResponse): string {
  if (slot.label) return slot.label

  if (slot.timeOfDay !== 'SPECIFIC') {
    return TIME_OF_DAY_LABELS[slot.timeOfDay] ?? slot.timeOfDay
  }

  if (slot.startTime) {
    const start = new Date(slot.startTime).toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    })
    const end = slot.endTime
      ? ` → ${new Date(slot.endTime).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`
      : ''
    return `${start}${end}`
  }

  return 'Créneau'
}
