<script lang="ts">
  import { createActor } from 'xstate'
  import { onDestroy } from 'svelte'
  import { eventListMachine, deriveFilteredEvents } from '$lib/machines/eventList.machine'
  import type { EventResponse, EventStatus } from '$lib/types/api'
  import EventCard from '$lib/components/molecules/EventCard.svelte'
  import SearchInput from '$lib/components/molecules/SearchInput.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'

  const actor = createActor(eventListMachine)
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => { sub.unsubscribe(); actor.stop() })

  let searchQuery = $state('')

  function handleSearch(q: string) {
    searchQuery = q
  }

  const allEvents = $derived(snapshot.context.events)

  function filterByStatus(events: EventResponse[], statuses: EventStatus[]): EventResponse[] {
    return events.filter((e) => statuses.includes(e.status))
  }

  const matchedEvents = $derived(
    deriveFilteredEvents(allEvents, searchQuery)
  )

  const sections: { key: string; label: string; statuses: EventStatus[] }[] = [
    { key: 'polling', label: '🗳️ Votes en cours', statuses: ['POLLING'] },
    { key: 'upcoming', label: '✅ À venir', statuses: ['CONFIRMED', 'ORGANIZING', 'FINALIZED'] },
    { key: 'draft', label: '📝 En préparation', statuses: ['DRAFT', 'COMPARING'] }
  ]
</script>

<div class="flex flex-col gap-6">
  <div class="flex flex-col gap-1">
    <h1 class="text-2xl font-bold text-gray-900">Explorer</h1>
    <p class="text-sm text-gray-500">Retrouvez tous les événements en un clin d'œil.</p>
  </div>

  <!-- Search bar -->
  <SearchInput
    value={searchQuery}
    placeholder="Rechercher un événement…"
    onsearch={handleSearch}
  />

  {#if snapshot.value === 'loading'}
    <!-- Skeletons for sections -->
    <div class="flex flex-col gap-8">
      {#each { length: 3 } as _, si (si)}
        <div class="flex flex-col gap-3">
          <SkeletonBlock height="h-6" width="w-40" rounded="rounded" />
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {#each { length: 3 } as _, ci (ci)}
              <div class="bg-white rounded-card shadow-card p-4 flex flex-col gap-3">
                <SkeletonBlock height="h-5" width="w-3/4" rounded="rounded" />
                <SkeletonBlock height="h-3" rounded="rounded" />
                <SkeletonBlock height="h-3" width="w-5/6" rounded="rounded" />
              </div>
            {/each}
          </div>
        </div>
      {/each}
    </div>

  {:else if snapshot.context.error}
    <ErrorBanner
      message={snapshot.context.error}
      onretry={() => actor.send({ type: 'RELOAD' })}
    />

  {:else if allEvents.length === 0}
    <div class="flex flex-col items-center justify-center py-20 text-center gap-4">
      <span class="text-6xl" aria-hidden="true">📅</span>
      <div class="flex flex-col gap-1">
        <p class="text-lg font-semibold text-gray-800">Aucun événement</p>
        <p class="text-sm text-gray-500">Créez votre premier événement !</p>
      </div>
      <a
        href="/create"
        class="inline-flex items-center gap-2 rounded-btn bg-wakeve-600 px-4 py-2 text-sm font-medium text-white
          hover:bg-wakeve-700 transition-default
          focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
      >
        + Créer
      </a>
    </div>

  {:else}
    <!-- Sections -->
    {#each sections as section (section.key)}
      {@const sectionEvents = filterByStatus(matchedEvents, section.statuses)}

      {#if sectionEvents.length > 0 || !searchQuery}
        <section aria-label={section.label}>
          <div class="flex items-center justify-between mb-3">
            <h2 class="text-base font-semibold text-gray-800">{section.label}</h2>
            {#if sectionEvents.length > 0}
              <span class="text-xs text-gray-400">{sectionEvents.length}</span>
            {/if}
          </div>

          {#if sectionEvents.length === 0}
            <p class="text-sm text-gray-400 py-4 text-center">
              Aucun événement dans cette catégorie.
            </p>
          {:else}
            <!-- Horizontal scroll on mobile, grid on desktop -->
            <div class="sm:hidden flex gap-3 overflow-x-auto pb-2 scrollbar-none -mx-4 px-4 snap-x snap-mandatory">
              {#each sectionEvents as event (event.id)}
                <div class="snap-start shrink-0 w-72">
                  <EventCard {event} />
                </div>
              {/each}
            </div>

            <div class="hidden sm:grid grid-cols-2 lg:grid-cols-3 gap-4">
              {#each sectionEvents as event (event.id)}
                <EventCard {event} />
              {/each}
            </div>
          {/if}
        </section>
      {/if}
    {/each}

    {#if searchQuery && matchedEvents.length === 0}
      <div class="flex flex-col items-center justify-center py-12 gap-3 text-center">
        <span class="text-4xl" aria-hidden="true">🔍</span>
        <p class="text-sm text-gray-500">Aucun événement ne correspond à « {searchQuery} »</p>
        <button
          type="button"
          onclick={() => { searchQuery = '' }}
          class="text-sm text-wakeve-600 hover:underline
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
        >
          Effacer la recherche
        </button>
      </div>
    {/if}
  {/if}
</div>
