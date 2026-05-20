<script lang="ts">
  import type { DashboardOverviewResponse, EventStatus } from '$lib/types/api'
  import SummaryCard from '$lib/components/molecules/SummaryCard.svelte'
  import ProgressBar from '$lib/components/atoms/ProgressBar.svelte'
  import { STATUS_LABELS } from '$lib/utils/event-type'

  interface Props {
    overview: DashboardOverviewResponse
  }

  const { overview }: Props = $props()

  const responseRateDisplay = $derived(
    `${Math.round(overview.responseRate * 100)}%`
  )

  const avgDisplay = $derived(overview.averageParticipantsPerEvent.toFixed(1))

  // Only show statuses with at least 1 event
  const statusEntries = $derived(
    (Object.entries(overview.eventsByStatus) as [EventStatus, number][])
      .filter(([, count]) => count > 0)
      .sort((a, b) => b[1] - a[1])
  )

  const statusBarColors: Record<EventStatus, string> = {
    DRAFT: 'bg-gray-400',
    POLLING: 'bg-blue-500',
    COMPARING: 'bg-purple-500',
    CONFIRMED: 'bg-green-500',
    ORGANIZING: 'bg-orange-500',
    FINALIZED: 'bg-emerald-500',
    EXPIRED: 'bg-red-400',
    DELETED: 'bg-red-300'
  }
</script>

<div class="flex flex-col gap-8">
  <!-- Stats grid -->
  <section aria-label="Vue d'ensemble">
    <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">
      <SummaryCard
        icon="📅"
        label="Événements"
        value={overview.totalEvents}
        color="bg-wakeve-50"
      />
      <SummaryCard
        icon="👥"
        label="Participants"
        value={overview.totalParticipants}
        color="bg-blue-50"
      />
      <SummaryCard
        icon="📊"
        label="Moy. / événement"
        value={avgDisplay}
        color="bg-purple-50"
      />
      <SummaryCard
        icon="🗳️"
        label="Votes totaux"
        value={overview.totalVotes}
        color="bg-green-50"
      />
      <SummaryCard
        icon="💬"
        label="Commentaires"
        value={overview.totalComments}
        color="bg-orange-50"
      />
      <SummaryCard
        icon="✅"
        label="Taux de réponse"
        value={responseRateDisplay}
        color="bg-emerald-50"
      />
    </div>
  </section>

  <!-- Status breakdown -->
  {#if statusEntries.length > 0}
    <section aria-label="Répartition par statut">
      <h2 class="text-base font-semibold text-gray-800 mb-4">Répartition par statut</h2>
      <ul class="flex flex-col gap-3" role="list">
        {#each statusEntries as [status, count] (status)}
          {@const pct = overview.totalEvents > 0
            ? Math.round((count / overview.totalEvents) * 100)
            : 0}
          <li class="flex flex-col gap-1.5">
            <div class="flex items-center justify-between text-sm">
              <span class="text-gray-700">{STATUS_LABELS[status]}</span>
              <span class="font-medium text-gray-900">{count}</span>
            </div>
            <ProgressBar
              value={pct}
              color={statusBarColors[status]}
              height="sm"
            />
          </li>
        {/each}
      </ul>
    </section>
  {/if}
</div>
