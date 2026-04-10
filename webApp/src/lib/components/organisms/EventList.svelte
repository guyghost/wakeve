<script lang="ts">
  import type { EventResponse } from '$lib/types/api'
  import EventCard from '$lib/components/molecules/EventCard.svelte'
  import SearchInput from '$lib/components/molecules/SearchInput.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'

  interface Props {
    events: EventResponse[]
    loading: boolean
    error: string | null
    searchQuery: string
    statusFilter: string
    onsearch: (q: string) => void
    onfilter: (s: string) => void
    onreload: () => void
  }

  const {
    events,
    loading,
    error,
    searchQuery,
    statusFilter,
    onsearch,
    onfilter,
    onreload
  }: Props = $props()

  const statusOptions = [
    { value: '', label: 'Tous les statuts' },
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'POLLING', label: 'Vote en cours' },
    { value: 'COMPARING', label: 'Comparaison' },
    { value: 'CONFIRMED', label: 'Confirmé' },
    { value: 'ORGANIZING', label: 'Organisation' },
    { value: 'FINALIZED', label: 'Finalisé' },
    { value: 'EXPIRED', label: 'Expiré' }
  ]

  function handleStatusChange(e: Event & { currentTarget: HTMLSelectElement }) {
    onfilter(e.currentTarget.value)
  }
</script>

<div class="flex flex-col gap-4">
  <!-- Toolbar -->
  <div class="flex flex-col sm:flex-row gap-3">
    <div class="flex-1">
      <SearchInput
        value={searchQuery}
        placeholder="Rechercher un événement…"
        onsearch={onsearch}
      />
    </div>
    <div class="sm:w-48">
      <Select
        id="status-filter"
        value={statusFilter}
        options={statusOptions}
        onchange={handleStatusChange}
      />
    </div>
  </div>

  <!-- Loading skeletons -->
  {#if loading}
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each { length: 6 } as _, i (i)}
        <div class="bg-white rounded-card shadow-card p-4 flex flex-col gap-3">
          <SkeletonBlock height="h-5" width="w-3/4" rounded="rounded" />
          <SkeletonBlock height="h-3" rounded="rounded" />
          <SkeletonBlock height="h-3" width="w-5/6" rounded="rounded" />
          <div class="flex gap-2 mt-1">
            <SkeletonBlock height="h-5" width="w-16" rounded="rounded-full" />
            <SkeletonBlock height="h-5" width="w-20" rounded="rounded" />
          </div>
        </div>
      {/each}
    </div>

  <!-- Error -->
  {:else if error}
    <ErrorBanner message={error} onretry={onreload} />

  <!-- Empty state -->
  {:else if events.length === 0}
    <div class="flex flex-col items-center justify-center py-20 text-center gap-4">
      <span class="text-6xl" aria-hidden="true">📅</span>
      <div class="flex flex-col gap-1">
        <p class="text-lg font-semibold text-gray-800">Aucun événement</p>
        <p class="text-sm text-gray-500 max-w-xs text-balance">
          {#if searchQuery || statusFilter}
            Aucun événement ne correspond à vos filtres.
          {:else}
            Créez votre premier événement pour commencer la planification collaborative.
          {/if}
        </p>
      </div>
      {#if !searchQuery && !statusFilter}
        <a
          href="/create"
          class="inline-flex items-center gap-2 rounded-btn bg-wakeve-600 px-4 py-2 text-sm font-medium text-white
            hover:bg-wakeve-700 transition-default
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
        >
          <span aria-hidden="true">+</span>
          Créer un événement
        </a>
      {/if}
    </div>

  <!-- Grid -->
  {:else}
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each events as event (event.id)}
        <EventCard {event} />
      {/each}
    </div>
  {/if}
</div>
