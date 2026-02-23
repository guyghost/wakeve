import { useTranslation } from 'react-i18next';
import type { EventStatus } from '../types/api';

const STATUS_CLASSNAMES: Record<EventStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  POLLING: 'bg-blue-100 text-blue-700',
  COMPARING: 'bg-purple-100 text-purple-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  ORGANIZING: 'bg-orange-100 text-orange-700',
  FINALIZED: 'bg-emerald-100 text-emerald-800',
};

const STATUS_KEYS: Record<EventStatus, string> = {
  DRAFT: 'events.statusDraft',
  POLLING: 'events.statusPolling',
  COMPARING: 'events.statusComparing',
  CONFIRMED: 'events.statusConfirmed',
  ORGANIZING: 'events.statusOrganizing',
  FINALIZED: 'events.statusFinalized',
};

export function StatusBadge({ status }: { status: EventStatus }) {
  const { t } = useTranslation();
  const className = STATUS_CLASSNAMES[status] || STATUS_CLASSNAMES.DRAFT;
  const label = t(STATUS_KEYS[status] || STATUS_KEYS.DRAFT);
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${className}`}
      role="status"
    >
      {label}
    </span>
  );
}
