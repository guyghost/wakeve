<script lang="ts">
  import type { DashboardEventItem, DashboardLifecycleStage } from '$lib/types/api'
  import DashboardEventCard from '$lib/components/molecules/DashboardEventCard.svelte'
  import StatePanel from '$lib/components/ui/StatePanel.svelte'

  interface Props {
    events: DashboardEventItem[]
    copiedEventId: string | null
    onselectevent: (id: string) => void
    oncopyevent: (id: string) => void
  }

  const { events, copiedEventId, onselectevent, oncopyevent }: Props = $props()

  const groups: { stage: DashboardLifecycleStage; title: string; description: string }[] = [
    { stage: 'prepare', title: 'À préparer', description: 'Brouillons et informations à compléter.' },
    { stage: 'decide', title: 'Vote à décider', description: 'Sondages, échéances et choix à verrouiller.' },
    { stage: 'organize', title: 'Organisation', description: 'Événements confirmés avec logistique à suivre.' },
    { stage: 'done', title: 'Terminé / archivé', description: 'Récapitulatifs et événements clôturés.' }
  ]

  function eventsFor(stage: DashboardLifecycleStage): DashboardEventItem[] {
    return events.filter((event) => event.lifecycleStage === stage)
  }
</script>

<section class="flex flex-col gap-4" aria-labelledby="dashboard-board-title">
  <div class="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
    <div>
      <h2 id="dashboard-board-title" class="text-base font-semibold text-slate-950">Événements par cycle de vie</h2>
      <p class="text-sm text-slate-500">Chaque groupe montre le format le plus utile pour décider vite.</p>
    </div>
    <a
      href="/app/events"
      class="text-sm font-medium text-wakeve-700 transition-default hover:text-wakeve-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
    >
      Voir tous
    </a>
  </div>

  <div class="grid grid-cols-1 gap-4 xl:grid-cols-4">
    {#each groups as group (group.stage)}
      {@const groupedEvents = eventsFor(group.stage)}
      <section class="rounded-lg border border-slate-200 bg-slate-50/70 p-3" aria-labelledby="group-{group.stage}">
        <div class="mb-3 flex items-start justify-between gap-3">
          <div>
            <h3 id="group-{group.stage}" class="text-sm font-semibold text-slate-900">{group.title}</h3>
            <p class="mt-0.5 text-xs leading-5 text-slate-500">{group.description}</p>
          </div>
          <span class="rounded-full bg-white px-2 py-0.5 text-right text-xs font-semibold tabular-nums text-slate-700 ring-1 ring-inset ring-slate-200">
            {groupedEvents.length}
          </span>
        </div>

        {#if groupedEvents.length === 0}
          <StatePanel
            tone="empty"
            title="Rien ici"
            description="Aucun événement dans cette étape pour le moment."
            compact
          />
        {:else}
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-1">
            {#each groupedEvents as event (event.id)}
              <DashboardEventCard
                {event}
                copied={copiedEventId === event.id}
                ondetails={onselectevent}
                oncopy={oncopyevent}
              />
            {/each}
          </div>
        {/if}
      </section>
    {/each}
  </div>
</section>
