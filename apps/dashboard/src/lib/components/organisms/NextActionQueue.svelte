<script lang="ts">
  import type { DashboardEventItem } from '$lib/types/api'
  import Badge from '$lib/components/atoms/Badge.svelte'
  import StatePanel from '$lib/components/ui/StatePanel.svelte'
  import { formatDate } from '$lib/utils/date'

  interface Props {
    events: DashboardEventItem[]
    onselectevent: (id: string) => void
  }

  const { events, onselectevent }: Props = $props()

  const priority = { high: 0, medium: 1, low: 2, done: 3 }
  const visibleActions = $derived(
    [...events]
      .filter((event) => event.nextAction.urgency !== 'done')
      .sort((a, b) => {
        const urgency = priority[a.nextAction.urgency] - priority[b.nextAction.urgency]
        if (urgency !== 0) return urgency
        return new Date(a.deadline ?? a.createdAt).getTime() - new Date(b.deadline ?? b.createdAt).getTime()
      })
      .slice(0, 5)
  )
</script>

<section class="rounded-lg border border-slate-200 bg-white p-4 shadow-sm" aria-labelledby="next-actions-title">
  <div class="mb-4 flex items-center justify-between gap-4">
    <div>
      <h2 id="next-actions-title" class="text-base font-semibold text-slate-950">Prochaines actions</h2>
      <p class="text-sm text-slate-500">Les décisions qui débloquent vos événements.</p>
    </div>
    <a
      href="/app/create"
      class="shrink-0 rounded-btn bg-wakeve-600 px-3 py-1.5 text-sm font-medium text-white transition-default hover:bg-wakeve-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
    >
      Créer
    </a>
  </div>

  {#if visibleActions.length === 0}
    <StatePanel
      tone="success"
      title="Aucune décision urgente"
      description="Vos événements actifs sont à jour. Vous pouvez créer un nouvel événement ou consulter les récapitulatifs."
      actionLabel="Créer un événement"
      actionHref="/app/create"
      compact
    />
  {:else}
    <ul class="divide-y divide-slate-100" role="list">
      {#each visibleActions as event (event.id)}
        <li class="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between">
          <button
            type="button"
            onclick={() => onselectevent(event.id)}
            class="min-w-0 text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
          >
            <span class="block truncate text-sm font-medium text-slate-950" title={event.title}>{event.title}</span>
            <span class="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500">
              <Badge status={event.status} size="sm" />
              {#if event.deadline}
                <span>{formatDate(event.deadline)}</span>
              {/if}
              {#if event.pendingParticipants > 0}
                <span>{event.pendingParticipants} participant{event.pendingParticipants > 1 ? 's' : ''} en attente</span>
              {/if}
            </span>
          </button>

          <a
            href={event.nextAction.href}
            class="inline-flex shrink-0 items-center justify-center rounded-btn border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 transition-default hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
          >
            {event.nextAction.label}
          </a>
        </li>
      {/each}
    </ul>
  {/if}
</section>
