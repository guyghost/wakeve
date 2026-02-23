import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { eventsApi } from '../services/api';
import { useApi } from '../hooks/useApi';
import { StatusBadge } from '../components/StatusBadge';
import type { EventResponse } from '../types/api';

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
    });
  } catch {
    return iso;
  }
}

export function ExplorePage() {
  const { data, error, isLoading } = useApi(() => eventsApi.list(), []);
  const [search, setSearch] = useState('');

  // Show all events for exploration (in a real app, this would be a public API)
  const filteredEvents = useMemo(() => {
    if (!data?.events) return [];
    const q = search.toLowerCase();
    if (!q) return data.events;
    return data.events.filter(
      (e) =>
        e.title.toLowerCase().includes(q) ||
        e.description.toLowerCase().includes(q),
    );
  }, [data, search]);

  // Group by status
  const polling = filteredEvents.filter((e) => e.status === 'POLLING');
  const upcoming = filteredEvents.filter((e) => e.status === 'CONFIRMED' || e.status === 'ORGANIZING' || e.status === 'FINALIZED');
  const drafts = filteredEvents.filter((e) => e.status === 'DRAFT' || e.status === 'COMPARING');

  const renderSection = (title: string, events: EventResponse[]) => {
    if (events.length === 0) return null;
    return (
      <section className="mb-8">
        <h2 className="text-lg font-semibold text-gray-900 mb-3">{title}</h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {events.map((event) => (
            <Link
              key={event.id}
              to={`/events/${event.id}`}
              className="bg-white rounded-xl border border-gray-200 hover:border-wakeve-300 hover:shadow-md transition-all p-4"
              aria-label={`Voir ${event.title}`}
            >
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-medium text-gray-900 truncate text-sm">{event.title}</h3>
                <StatusBadge status={event.status} />
              </div>
              {event.description && (
                <p className="mt-1.5 text-xs text-gray-500 line-clamp-2">{event.description}</p>
              )}
              <div className="mt-3 flex items-center gap-3 text-xs text-gray-400">
                <span>{event.participants.length} participant{event.participants.length !== 1 ? 's' : ''}</span>
                <span>{event.proposedSlots.length} creneau{event.proposedSlots.length !== 1 ? 'x' : ''}</span>
                <span>Limite : {formatDate(event.deadline)}</span>
              </div>
            </Link>
          ))}
        </div>
      </section>
    );
  };

  return (
    <div className="pb-20 md:pb-0">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Explorer</h1>

      <div className="relative mb-6">
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
          placeholder="Rechercher un evenement..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none"
          aria-label="Rechercher"
        />
      </div>

      {isLoading && (
        <div className="flex justify-center py-12" role="status" aria-label="Chargement">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-wakeve-600" />
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700" role="alert">
          {error}
        </div>
      )}

      {!isLoading && !error && (
        <>
          {renderSection('Sondages en cours', polling)}
          {renderSection('Evenements a venir', upcoming)}
          {renderSection('En preparation', drafts)}

          {filteredEvents.length === 0 && (
            <div className="text-center py-12 text-gray-500">
              <p className="text-sm">
                {search ? 'Aucun evenement ne correspond a votre recherche.' : 'Aucun evenement disponible pour le moment.'}
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
