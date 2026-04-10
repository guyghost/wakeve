<script lang="ts">
  import { createActor } from 'xstate'
  import { onDestroy } from 'svelte'
  import { eventListMachine, deriveFilteredEvents } from '$lib/machines/eventList.machine'
  import EventList from '$lib/components/organisms/EventList.svelte'

  const actor = createActor(eventListMachine)
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => {
    sub.unsubscribe()
    actor.stop()
  })

  const filteredEvents = $derived(
    deriveFilteredEvents(snapshot.context.events, snapshot.context.searchQuery)
  )
</script>

<div class="flex flex-col gap-6">
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-bold text-gray-900">Mes événements</h1>
    <a
      href="/create"
      class="inline-flex items-center gap-1.5 rounded-btn bg-wakeve-600 px-4 py-2 text-sm font-medium text-white
        hover:bg-wakeve-700 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
    >
      <span aria-hidden="true">+</span> Créer
    </a>
  </div>

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
</div>
