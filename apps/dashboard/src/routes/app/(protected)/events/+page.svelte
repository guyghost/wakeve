<script lang="ts">
  import { createActor } from 'xstate'
  import { onDestroy } from 'svelte'
  import { eventListMachine, deriveFilteredEvents } from '$lib/machines/eventList.machine'
  import type { EventResponse } from '$lib/types/api'
  import EventList from '$lib/components/organisms/EventList.svelte'
  import { t } from '$lib/i18n'

  const actor = createActor(eventListMachine)
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => {
    sub.unsubscribe()
    actor.stop()
  })

  // Archives filter: past events = FINALIZED/ARCHIVED status or finalDate in
  // the past. Off by default so the standard list keeps its original behavior.
  let archivedOnly = $state(false)

  function isPastEvent(event: EventResponse): boolean {
    if (event.status === 'FINALIZED' || event.status === 'ARCHIVED') return true
    if (event.finalDate) return new Date(event.finalDate).getTime() < Date.now()
    return false
  }

  const filteredEvents = $derived.by(() => {
    const base = deriveFilteredEvents(snapshot.context.events, snapshot.context.searchQuery)
    return archivedOnly ? base.filter(isPastEvent) : base
  })
</script>

<div class="flex flex-col gap-6">
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-bold text-gray-900">Mes événements</h1>
    <a
      href="/app/create"
      class="inline-flex items-center gap-1.5 rounded-btn bg-wakeve-600 px-4 py-2 text-sm font-medium text-white
        hover:bg-wakeve-700 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
    >
      <span aria-hidden="true">+</span> Créer
    </a>
  </div>

  <!-- Archives / past events filter -->
  <label
    class="flex w-fit cursor-pointer items-center gap-2 text-sm text-gray-600"
  >
    <input
      type="checkbox"
      bind:checked={archivedOnly}
      class="h-4 w-4 rounded border-gray-300 accent-wakeve-600"
    />
    {t('events.archives.filter')}
  </label>

  {#if archivedOnly && !snapshot.context.error && snapshot.value !== 'loading' && filteredEvents.length === 0}
    <!-- Dedicated empty state for the archives filter -->
    <div class="flex flex-col items-center justify-center py-20 text-center gap-2">
      <span class="text-6xl" aria-hidden="true">🗂️</span>
      <p class="text-lg font-semibold text-gray-800">{t('events.archives.emptyTitle')}</p>
      <p class="text-sm text-gray-500 max-w-xs text-balance">{t('events.archives.emptyDesc')}</p>
    </div>
  {:else}
    <EventList
      events={filteredEvents}
      loading={snapshot.value === 'loading'}
      error={snapshot.context.error}
      searchQuery={snapshot.context.searchQuery}
      statusFilter={snapshot.context.statusFilter}
      onsearch={(q) => actor.send({ type: 'SEARCH', query: q })}
      onfilter={(s) => actor.send({ type: 'FILTER', status: s })}
      onreload={() => actor.send({ type: 'RELOAD' })}
    />
  {/if}
</div>
