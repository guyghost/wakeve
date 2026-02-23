import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';

// ==================== Types ====================

interface DashboardOverview {
  totalEvents: number;
  totalParticipants: number;
  averageParticipants: number;
  totalVotes: number;
  totalComments: number;
  eventsByStatus: Record<string, number>;
}

interface DashboardEvent {
  eventId: string;
  title: string;
  status: string;
  eventType?: string;
  participantCount: number;
  voteCount: number;
  commentCount: number;
  responseRate: number;
}

interface TimeSlotVotes {
  label: string;
  yesVotes: number;
  maybeVotes: number;
  noVotes: number;
  totalVotes: number;
}

interface EventAnalytics {
  voteTimeline: { date: string; count: number }[];
  participantTimeline: { date: string; count: number }[];
  popularTimeSlots: TimeSlotVotes[];
  pollCompletionRate: number;
  totalParticipants: number;
  votedParticipants: number;
}

// ==================== Mock Data ====================

const mockOverview: DashboardOverview = {
  totalEvents: 12,
  totalParticipants: 87,
  averageParticipants: 7.25,
  totalVotes: 234,
  totalComments: 56,
  eventsByStatus: {
    DRAFT: 3,
    POLLING: 2,
    CONFIRMED: 4,
    FINALIZED: 3,
  },
};

const mockEvents: DashboardEvent[] = [
  { eventId: '1', title: 'Anniversaire Marie', status: 'CONFIRMED', eventType: 'BIRTHDAY', participantCount: 15, voteCount: 42, commentCount: 8, responseRate: 87.5 },
  { eventId: '2', title: 'Team Building Q4', status: 'POLLING', eventType: 'TEAM_BUILDING', participantCount: 22, voteCount: 38, commentCount: 12, responseRate: 65.0 },
  { eventId: '3', title: 'Soiree de Noel', status: 'DRAFT', eventType: 'PARTY', participantCount: 8, voteCount: 0, commentCount: 3, responseRate: 0.0 },
  { eventId: '4', title: 'Reunion Projet Alpha', status: 'FINALIZED', eventType: 'CONFERENCE', participantCount: 10, voteCount: 28, commentCount: 5, responseRate: 93.0 },
  { eventId: '5', title: 'Brunch Dominical', status: 'CONFIRMED', eventType: 'FOOD_TASTING', participantCount: 6, voteCount: 18, commentCount: 4, responseRate: 100.0 },
];

const mockAnalytics: EventAnalytics = {
  voteTimeline: [
    { date: '01/10', count: 3 },
    { date: '02/10', count: 7 },
    { date: '03/10', count: 12 },
    { date: '04/10', count: 8 },
    { date: '05/10', count: 15 },
    { date: '06/10', count: 5 },
    { date: '07/10', count: 2 },
  ],
  participantTimeline: [
    { date: '01/10', count: 2 },
    { date: '02/10', count: 4 },
    { date: '03/10', count: 3 },
    { date: '04/10', count: 5 },
    { date: '05/10', count: 1 },
  ],
  popularTimeSlots: [
    { label: 'AFTERNOON', yesVotes: 12, maybeVotes: 3, noVotes: 2, totalVotes: 17 },
    { label: 'MORNING', yesVotes: 8, maybeVotes: 5, noVotes: 4, totalVotes: 17 },
    { label: 'EVENING', yesVotes: 6, maybeVotes: 7, noVotes: 4, totalVotes: 17 },
  ],
  pollCompletionRate: 85.0,
  totalParticipants: 15,
  votedParticipants: 13,
};

// ==================== Helpers ====================

const STATUS_KEYS: Record<string, string> = {
  DRAFT: 'events.statusDraft',
  POLLING: 'events.statusPolling',
  COMPARING: 'events.statusComparing',
  CONFIRMED: 'events.statusConfirmed',
  ORGANIZING: 'events.statusOrganizing',
  FINALIZED: 'events.statusFinalized',
};

const TIME_SLOT_KEYS: Record<string, string> = {
  AFTERNOON: 'events.timeAfternoon',
  MORNING: 'events.timeMorning',
  EVENING: 'events.timeEvening',
  ALL_DAY: 'events.timeAllDay',
};

const statusColors: Record<string, string> = {
  DRAFT: '#6B7280',
  POLLING: '#2563EB',
  COMPARING: '#4F46E5',
  CONFIRMED: '#16A34A',
  ORGANIZING: '#EA580C',
  FINALIZED: '#9333EA',
};

// ==================== Components ====================

function SummaryCard({ icon, title, value, color }: { icon: string; title: string; value: string; color: string }) {
  return (
    <div
      className="flex flex-col items-center p-4 rounded-xl"
      style={{ backgroundColor: `${color}15` }}
    >
      <span className="text-2xl mb-1" role="img" aria-hidden="true">{icon}</span>
      <span className="text-xl font-bold text-gray-900">{value}</span>
      <span className="text-xs text-gray-500 mt-0.5">{title}</span>
    </div>
  );
}

function StatusBar({ status, count, total, label }: { status: string; count: number; total: number; label: string }) {
  const pct = total > 0 ? (count / total) * 100 : 0;
  const color = statusColors[status] || '#6B7280';

  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-gray-700 w-28 shrink-0">{label}</span>
      <div className="flex-1 h-5 bg-gray-100 rounded overflow-hidden">
        <div
          className="h-full rounded transition-all duration-500"
          style={{ width: `${pct}%`, backgroundColor: color }}
        />
      </div>
      <span className="text-sm font-semibold w-8 text-right" style={{ color }}>{count}</span>
    </div>
  );
}

function BarChart({ entries, color }: { entries: { date: string; count: number }[]; color: string }) {
  const maxCount = Math.max(...entries.map((e) => e.count), 1);

  return (
    <div className="flex items-end gap-1.5 h-24">
      {entries.map((entry) => (
        <div key={entry.date} className="flex flex-col items-center flex-1 gap-0.5">
          <span className="text-[10px] text-gray-400">{entry.count}</span>
          <div
            className="w-full rounded-t transition-all duration-500"
            style={{
              height: `${Math.max((entry.count / maxCount) * 70, 3)}px`,
              backgroundColor: color,
              opacity: 0.85,
            }}
          />
          <span className="text-[9px] text-gray-400">{entry.date}</span>
        </div>
      ))}
    </div>
  );
}

function CircularProgress({ percentage }: { percentage: number }) {
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (percentage / 100) * circumference;
  const color = percentage > 75 ? '#16A34A' : percentage > 50 ? '#EA580C' : '#DC2626';

  return (
    <div className="relative w-24 h-24">
      <svg className="w-24 h-24 -rotate-90" viewBox="0 0 96 96">
        <circle cx="48" cy="48" r={radius} fill="none" stroke="#E5E7EB" strokeWidth="8" />
        <circle
          cx="48"
          cy="48"
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth="8"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className="transition-all duration-700"
        />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <span className="text-lg font-bold text-gray-900">{Math.round(percentage)}%</span>
      </div>
    </div>
  );
}

function StackedBar({ yes, maybe, no, total }: { yes: number; maybe: number; no: number; total: number }) {
  const t = Math.max(total, 1);
  return (
    <div className="flex gap-0.5 h-3.5 rounded overflow-hidden">
      <div className="rounded" style={{ width: `${(yes / t) * 100}%`, backgroundColor: '#16A34A' }} />
      <div className="rounded" style={{ width: `${(maybe / t) * 100}%`, backgroundColor: '#EA580C' }} />
      <div className="rounded" style={{ width: `${(no / t) * 100}%`, backgroundColor: '#DC2626' }} />
    </div>
  );
}

// ==================== Event Detail Modal ====================

function EventDetailModal({ event, onClose }: { event: DashboardEvent; onClose: () => void }) {
  const { t } = useTranslation();
  const analytics = mockAnalytics;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-end sm:items-center justify-center z-50" onClick={onClose}>
      <div
        className="bg-white rounded-t-2xl sm:rounded-2xl w-full sm:max-w-lg max-h-[85vh] overflow-y-auto p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold text-gray-900">{event.title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none" aria-label={t('dashboard.close')}>&times;</button>
        </div>

        {/* Poll Completion */}
        <div className="bg-gray-50 rounded-xl p-4 mb-4 flex flex-col items-center gap-3">
          <h3 className="text-sm font-semibold text-gray-700">{t('dashboard.participationRate')}</h3>
          <CircularProgress percentage={analytics.pollCompletionRate} />
          <p className="text-sm text-gray-500">
            {t('dashboard.participantsVoted', { voted: analytics.votedParticipants, total: analytics.totalParticipants })}
          </p>
        </div>

        {/* Vote Timeline */}
        <div className="bg-gray-50 rounded-xl p-4 mb-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-3">{t('dashboard.voteTimeline')}</h3>
          <BarChart entries={analytics.voteTimeline} color="#2563EB" />
        </div>

        {/* Participant Timeline */}
        <div className="bg-gray-50 rounded-xl p-4 mb-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-3">{t('dashboard.participantTimeline')}</h3>
          <BarChart entries={analytics.participantTimeline} color="#16A34A" />
        </div>

        {/* Popular Time Slots */}
        <div className="bg-gray-50 rounded-xl p-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-3">{t('dashboard.popularSlots')}</h3>
          <div className="space-y-4">
            {analytics.popularTimeSlots.map((slot, idx) => (
              <div key={slot.label}>
                <div className="flex items-center gap-2 mb-1.5">
                  <span
                    className="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold text-white"
                    style={{ backgroundColor: idx === 0 ? '#9333EA' : '#6B7280' }}
                  >
                    #{idx + 1}
                  </span>
                  <span className="text-sm font-medium text-gray-700 flex-1">
                    {TIME_SLOT_KEYS[slot.label] ? t(TIME_SLOT_KEYS[slot.label]) : slot.label}
                  </span>
                  <span className="text-xs text-gray-400">{t('common.vote', { count: slot.totalVotes })}</span>
                </div>
                <StackedBar yes={slot.yesVotes} maybe={slot.maybeVotes} no={slot.noVotes} total={slot.totalVotes} />
                <div className="flex gap-4 mt-1">
                  <span className="flex items-center gap-1 text-[10px] text-gray-400">
                    <span className="w-2 h-2 rounded-full" style={{ backgroundColor: '#16A34A' }} />{t('common.yes')}: {slot.yesVotes}
                  </span>
                  <span className="flex items-center gap-1 text-[10px] text-gray-400">
                    <span className="w-2 h-2 rounded-full" style={{ backgroundColor: '#EA580C' }} />{t('common.maybe')}: {slot.maybeVotes}
                  </span>
                  <span className="flex items-center gap-1 text-[10px] text-gray-400">
                    <span className="w-2 h-2 rounded-full" style={{ backgroundColor: '#DC2626' }} />{t('common.no')}: {slot.noVotes}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ==================== Main Page ====================

export function DashboardPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [selectedEvent, setSelectedEvent] = useState<DashboardEvent | null>(null);

  const overview = mockOverview;
  const events = mockEvents;
  const total = Object.values(overview.eventsByStatus).reduce((a, b) => a + b, 0);

  return (
    <div className="pb-20 md:pb-0 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('dashboard.title')}</h1>

      {/* Summary Cards */}
      <section className="mb-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">{t('dashboard.overview')}</h2>
        <div className="grid grid-cols-3 gap-3">
          <SummaryCard icon="📅" title={t('dashboard.events')} value={`${overview.totalEvents}`} color="#2563EB" />
          <SummaryCard icon="👥" title={t('dashboard.participants')} value={`${overview.totalParticipants}`} color="#16A34A" />
          <SummaryCard icon="📊" title={t('dashboard.avgPerEvent')} value={overview.averageParticipants.toFixed(1)} color="#9333EA" />
          <SummaryCard icon="👍" title={t('dashboard.votes')} value={`${overview.totalVotes}`} color="#EA580C" />
          <SummaryCard icon="💬" title={t('dashboard.comments')} value={`${overview.totalComments}`} color="#0D9488" />
          <SummaryCard icon="📈" title={t('dashboard.responseRate')} value="78%" color="#DC2626" />
        </div>
      </section>

      {/* Status Breakdown */}
      <section className="mb-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">{t('dashboard.eventsByStatus')}</h2>
        <div className="bg-white rounded-xl border border-gray-200 p-4 space-y-3">
          {Object.entries(overview.eventsByStatus)
            .sort(([, a], [, b]) => b - a)
            .map(([status, count]) => (
              <StatusBar
                key={status}
                status={status}
                count={count}
                total={total}
                label={t(STATUS_KEYS[status] || 'events.statusDraft')}
              />
            ))}
        </div>
      </section>

      {/* Events List */}
      <section>
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">{t('dashboard.myEvents')}</h2>
        <div className="space-y-3">
          {events.map((event) => {
            const color = statusColors[event.status] || '#6B7280';
            const label = t(STATUS_KEYS[event.status] || 'events.statusDraft');

            return (
              <button
                key={event.eventId}
                onClick={() => setSelectedEvent(event)}
                className="w-full text-left bg-white rounded-xl border border-gray-200 p-4 hover:shadow-md transition-shadow"
              >
                <div className="flex justify-between items-start mb-3">
                  <h3 className="text-sm font-semibold text-gray-900">{event.title}</h3>
                  <span
                    className="text-[10px] font-medium px-2 py-0.5 rounded-full"
                    style={{ color, backgroundColor: `${color}20` }}
                  >
                    {label}
                  </span>
                </div>
                <div className="flex justify-between text-center">
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{event.participantCount}</div>
                    <div className="text-[10px] text-gray-400">{t('dashboard.participants')}</div>
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{event.voteCount}</div>
                    <div className="text-[10px] text-gray-400">{t('dashboard.votes')}</div>
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{event.commentCount}</div>
                    <div className="text-[10px] text-gray-400">{t('dashboard.comm')}</div>
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{Math.round(event.responseRate)}%</div>
                    <div className="text-[10px] text-gray-400">{t('dashboard.resp')}</div>
                  </div>
                </div>
                <div className="flex justify-end mt-2">
                  <svg className="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                  </svg>
                </div>
              </button>
            );
          })}
        </div>
      </section>

      {/* Event Detail Modal */}
      {selectedEvent && (
        <EventDetailModal event={selectedEvent} onClose={() => setSelectedEvent(null)} />
      )}
    </div>
  );
}
