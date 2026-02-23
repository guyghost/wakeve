import type { EventStatus } from '../types/api';

const STATUS_CONFIG: Record<EventStatus, { label: string; className: string }> = {
  DRAFT: { label: 'Brouillon', className: 'bg-gray-100 text-gray-700' },
  POLLING: { label: 'Sondage', className: 'bg-blue-100 text-blue-700' },
  COMPARING: { label: 'Comparaison', className: 'bg-purple-100 text-purple-700' },
  CONFIRMED: { label: 'Confirme', className: 'bg-green-100 text-green-700' },
  ORGANIZING: { label: 'Organisation', className: 'bg-orange-100 text-orange-700' },
  FINALIZED: { label: 'Finalise', className: 'bg-emerald-100 text-emerald-800' },
};

export function StatusBadge({ status }: { status: EventStatus }) {
  const config = STATUS_CONFIG[status] || STATUS_CONFIG.DRAFT;
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}
      role="status"
    >
      {config.label}
    </span>
  );
}
