<script lang="ts">
  import type { DashboardEventItem } from '$lib/types/api'
  import Badge from '$lib/components/atoms/Badge.svelte'
  import ProgressBar from '$lib/components/atoms/ProgressBar.svelte'
  import DashboardOverflowMenu from './DashboardOverflowMenu.svelte'
  import Tooltip from '$lib/components/ui/Tooltip.svelte'
  import { EVENT_TYPE_LABELS } from '$lib/utils/event-type'
  import { formatDate, timeAgo } from '$lib/utils/date'

  interface Props {
    event: DashboardEventItem
    copied: boolean
    ondetails: (id: string) => void
    oncopy: (id: string) => void
  }

  const { event, copied, ondetails, oncopy }: Props = $props()

  const deadlineLabel = $derived.by(() => {
    if (!event.deadline) return 'Sans limite'
    if (event.deadlineState === 'overdue') return `Expiré ${timeAgo(event.deadline)}`
    if (event.deadlineState === 'soon') return `Échéance ${timeAgo(event.deadline)}`
    return formatDate(event.deadline)
  })

  const deadlineTone = $derived(
    event.deadlineState === 'overdue'
      ? 'border-red-200 bg-red-50 text-red-700'
      : event.deadlineState === 'soon'
        ? 'border-amber-200 bg-amber-50 text-amber-800'
        : 'border-slate-200 bg-slate-50 text-slate-600'
  )

  const progressTone = $derived(
    event.isVoteClosed
      ? 'bg-slate-400'
      : event.isPollExpired
        ? 'bg-red-500'
        : event.responseRate >= 0.8
          ? 'bg-emerald-500'
          : 'bg-wakeve-500'
  )
</script>

<article
  class="group flex min-h-56 flex-col justify-between rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-default hover:border-slate-300 hover:shadow-md"
>
  <div class="flex flex-col gap-4">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <a
          href="/app/events/{event.id}"
          class="block truncate text-sm font-semibold text-slate-950 transition-default hover:text-wakeve-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
          title={event.title}
        >
          {event.title}
        </a>
        <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500">
          <span class="rounded-full bg-slate-100 px-2 py-0.5 text-slate-600">
            {EVENT_TYPE_LABELS[event.type] ?? event.type}
          </span>
          {#if event.commentCount > 0}
            <Tooltip label={`${event.commentCount} commentaire${event.commentCount > 1 ? 's' : ''}`}>
              <span class="rounded-full bg-slate-100 px-2 py-0.5 text-slate-600">
                {event.commentCount} note{event.commentCount > 1 ? 's' : ''}
              </span>
            </Tooltip>
          {/if}
        </div>
      </div>

      <DashboardOverflowMenu
        {copied}
        ondetails={() => ondetails(event.id)}
        oncopy={() => oncopy(event.id)}
      />
    </div>

    <div class="flex flex-wrap items-center gap-2">
      <Badge status={event.status} size="sm" />
      <span class="rounded-full border px-2 py-0.5 text-xs font-medium {deadlineTone}">
        {deadlineLabel}
      </span>
      {#if event.isPollExpired}
        <span class="rounded-full border border-red-200 bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700">
          Vote expiré
        </span>
      {:else if event.isVoteClosed}
        <span class="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs font-medium text-slate-600">
          RSVP fermé
        </span>
      {/if}
    </div>

    <div class="rounded-lg bg-slate-50 p-3">
      <div class="mb-2 flex items-center justify-between gap-3 text-xs">
        <span class="font-medium text-slate-600">Réponses</span>
        <span class="text-right font-semibold tabular-nums text-slate-950">{event.responseRatePct}%</span>
      </div>
      <ProgressBar value={event.responseRatePct} color={progressTone} height="sm" />
      <div class="mt-2 grid grid-cols-3 gap-2 text-xs text-slate-500">
        <span>Participants</span>
        <span class="text-right tabular-nums">{event.participantCount}</span>
        <span class="text-right tabular-nums">{event.pendingParticipants} en attente</span>
      </div>
    </div>
  </div>

  <div class="mt-4 flex items-center justify-between gap-3 border-t border-slate-100 pt-3">
    <a
      href={event.nextAction.href}
      class="inline-flex min-w-0 items-center rounded-btn bg-slate-950 px-3 py-1.5 text-sm font-medium text-white transition-default hover:bg-slate-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-950"
    >
      <span class="truncate">{event.nextAction.label}</span>
    </a>
    <button
      type="button"
      onclick={() => ondetails(event.id)}
      class="rounded-btn px-2 py-1.5 text-sm font-medium text-slate-600 opacity-100 transition-default hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 sm:opacity-0 sm:group-hover:opacity-100 sm:group-focus-within:opacity-100"
    >
      Aperçu
    </button>
  </div>
</article>
