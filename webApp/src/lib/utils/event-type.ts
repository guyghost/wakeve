import type { EventType, EventStatus } from '$lib/types/api'

export const EVENT_TYPE_LABELS: Record<EventType, string> = {
  BIRTHDAY: 'Anniversaire',
  WEDDING: 'Mariage',
  CORPORATE: 'Corporate',
  TEAM_BUILDING: 'Team Building',
  CONCERT: 'Concert',
  SPORTS_EVENT: 'Sport',
  SPORT_EVENT: 'Sport',
  FAMILY_REUNION: 'Réunion de famille',
  GRADUATION: 'Remise de diplômes',
  HOLIDAY_PARTY: "Fête de fin d'année",
  NETWORKING: 'Networking',
  CONFERENCE: 'Conférence',
  WORKSHOP: 'Atelier',
  DINNER_PARTY: 'Dîner',
  OUTDOOR_ADVENTURE: 'Aventure outdoor',
  CULTURAL_EVENT: 'Événement culturel',
  OTHER: 'Autre'
}

export const EVENT_TYPE_ICONS: Record<EventType, string> = {
  BIRTHDAY: '🎂',
  WEDDING: '💍',
  CORPORATE: '🏢',
  TEAM_BUILDING: '🤝',
  CONCERT: '🎵',
  SPORTS_EVENT: '⚽',
  SPORT_EVENT: '⚽',
  FAMILY_REUNION: '👨‍👩‍👧‍👦',
  GRADUATION: '🎓',
  HOLIDAY_PARTY: '🎉',
  NETWORKING: '🌐',
  CONFERENCE: '🎤',
  WORKSHOP: '🔧',
  DINNER_PARTY: '🍽️',
  OUTDOOR_ADVENTURE: '🏔️',
  CULTURAL_EVENT: '🎭',
  OTHER: '📅'
}

export const STATUS_COLORS: Record<EventStatus, { bg: string; text: string; ring: string }> = {
  DRAFT: { bg: 'bg-gray-100', text: 'text-gray-700', ring: 'ring-gray-200' },
  POLLING: { bg: 'bg-blue-100', text: 'text-blue-700', ring: 'ring-blue-200' },
  COMPARING: { bg: 'bg-purple-100', text: 'text-purple-700', ring: 'ring-purple-200' },
  CONFIRMED: { bg: 'bg-green-100', text: 'text-green-700', ring: 'ring-green-200' },
  ORGANIZING: { bg: 'bg-orange-100', text: 'text-orange-700', ring: 'ring-orange-200' },
  FINALIZED: { bg: 'bg-emerald-100', text: 'text-emerald-700', ring: 'ring-emerald-200' },
  EXPIRED: { bg: 'bg-red-100', text: 'text-red-700', ring: 'ring-red-200' },
  DELETED: { bg: 'bg-red-100', text: 'text-red-700', ring: 'ring-red-200' }
}

export const STATUS_LABELS: Record<EventStatus, string> = {
  DRAFT: 'Brouillon',
  POLLING: 'Vote en cours',
  COMPARING: 'Comparaison',
  CONFIRMED: 'Confirmé',
  ORGANIZING: 'Organisation',
  FINALIZED: 'Finalisé',
  EXPIRED: 'Expiré',
  DELETED: 'Supprimé'
}
