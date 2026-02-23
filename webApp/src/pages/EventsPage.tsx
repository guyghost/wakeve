import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { eventsApi } from '../services/api';
import { useApi } from '../hooks/useApi';
import { StatusBadge } from '../components/StatusBadge';
import type { EventResponse, EventStatus } from '../types/api';

const EVENT_TYPE_KEYS: Record<string, string> = {
  BIRTHDAY: 'events.typeBirthday',
  WEDDING: 'events.typeWedding',
  TEAM_BUILDING: 'events.typeTeamBuilding',
  CONFERENCE: 'events.typeConference',
  WORKSHOP: 'events.typeWorkshop',
  PARTY: 'events.typeParty',
  SPORTS_EVENT: 'events.typeSportsEvent',
  CULTURAL_EVENT: 'events.typeCulturalEvent',
  FAMILY_GATHERING: 'events.typeFamilyGathering',
  OUTDOOR_ACTIVITY: 'events.typeOutdoorActivity',
  FOOD_TASTING: 'events.typeFoodTasting',
  TECH_MEETUP: 'events.typeTechMeetup',
  WELLNESS_EVENT: 'events.typeWellnessEvent',
  CREATIVE_WORKSHOP: 'events.typeCreativeWorkshop',
  OTHER: 'events.typeOther',
  CUSTOM: 'events.typeCustom',
};

function formatDate(iso: string, lng: string): string {
  try {
    return new Date(iso).toLocaleDateString(lng, {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return iso;
  }
}

function EventCard({ event }: { event: EventResponse }) {
  const { t, i18n } = useTranslation();
  return (
    <Link
      to={`/events/${event.id}`}
      className="block bg-white rounded-xl border border-gray-200 hover:border-wakeve-300 hover:shadow-md transition-all p-5"
      aria-label={t('events.viewEvent', { title: event.title })}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <h3 className="font-semibold text-gray-900 truncate">{event.title}</h3>
          {event.description && (
            <p className="mt-1 text-sm text-gray-500 line-clamp-2">{event.description}</p>
          )}
        </div>
        <StatusBadge status={event.status} />
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-3 text-xs text-gray-500">
        {event.eventType && event.eventType !== 'OTHER' && (
          <span className="inline-flex items-center gap-1">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9.568 3H5.25A2.25 2.25 0 0 0 3 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 0 0 5.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 0 0 9.568 3Z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 6h.008v.008H6V6Z" />
            </svg>
            {EVENT_TYPE_KEYS[event.eventType] ? t(EVENT_TYPE_KEYS[event.eventType]) : event.eventType}
          </span>
        )}

        <span className="inline-flex items-center gap-1">
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
          </svg>
          {t('common.participant', { count: event.participants.length })}
        </span>

        <span className="inline-flex items-center gap-1">
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
          </svg>
          {t('events.deadlineValue', { date: formatDate(event.deadline, i18n.language) })}
        </span>

        {event.proposedSlots.length > 0 && (
          <span className="inline-flex items-center gap-1">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5" />
            </svg>
            {t('common.slot', { count: event.proposedSlots.length })}
          </span>
        )}
      </div>
    </Link>
  );
}

export function EventsPage() {
  const { t } = useTranslation();
  const { data, error, isLoading } = useApi(() => eventsApi.list(), []);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<EventStatus | 'ALL'>('ALL');

  const STATUS_FILTER_OPTIONS: { value: EventStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: t('events.statusAll') },
    { value: 'DRAFT', label: t('events.statusDraftPlural') },
    { value: 'POLLING', label: t('events.statusPollingPlural') },
    { value: 'COMPARING', label: t('events.statusComparing') },
    { value: 'CONFIRMED', label: t('events.statusConfirmedPlural') },
    { value: 'ORGANIZING', label: t('events.statusOrganizing') },
    { value: 'FINALIZED', label: t('events.statusFinalizedPlural') },
  ];

  const filteredEvents = useMemo(() => {
    if (!data?.events) return [];
    let events = data.events;

    if (statusFilter !== 'ALL') {
      events = events.filter((e) => e.status === statusFilter);
    }

    if (search.trim()) {
      const q = search.toLowerCase();
      events = events.filter(
        (e) =>
          e.title.toLowerCase().includes(q) ||
          e.description.toLowerCase().includes(q),
      );
    }

    return events;
  }, [data, search, statusFilter]);

  return (
    <div className="pb-20 md:pb-0">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 className="text-2xl font-bold text-gray-900">{t('events.myEvents')}</h1>
        <Link
          to="/create"
          className="hidden md:inline-flex items-center gap-2 px-4 py-2 bg-wakeve-600 text-white font-medium rounded-lg hover:bg-wakeve-700 focus:outline-none focus:ring-2 focus:ring-wakeve-500 focus:ring-offset-2 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          {t('events.newEvent')}
        </Link>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <div className="relative flex-1">
          <svg
            className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            aria-hidden="true"
          >
            <path strokeLinecap="round" strokeLinejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
          <input
            type="search"
            placeholder={t('events.searchPlaceholder')}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none"
            aria-label={t('events.searchAriaLabel')}
          />
        </div>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as EventStatus | 'ALL')}
          className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none bg-white"
          aria-label={t('events.filterByStatus')}
        >
          {STATUS_FILTER_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      {/* Content */}
      {isLoading && (
        <div className="flex justify-center py-12" role="status" aria-label={t('events.loadingEvents')}>
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-wakeve-600" />
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700" role="alert">
          <p className="font-medium">{t('events.loadError')}</p>
          <p className="text-sm mt-1">{error}</p>
        </div>
      )}

      {!isLoading && !error && filteredEvents.length === 0 && (
        <div className="text-center py-12">
          <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={1} stroke="currentColor" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5" />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-gray-900">{t('events.noEvents')}</h3>
          <p className="mt-1 text-sm text-gray-500">
            {search || statusFilter !== 'ALL'
              ? t('events.noEventsSearch')
              : t('events.noEventsCreate')}
          </p>
          {!search && statusFilter === 'ALL' && (
            <Link
              to="/create"
              className="inline-flex items-center gap-2 mt-4 px-4 py-2 bg-wakeve-600 text-white font-medium rounded-lg hover:bg-wakeve-700 transition-colors"
            >
              {t('events.createEvent')}
            </Link>
          )}
        </div>
      )}

      {!isLoading && !error && filteredEvents.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredEvents.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </div>
  );
}
