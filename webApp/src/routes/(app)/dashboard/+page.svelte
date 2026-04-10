<script lang="ts">
  import { createActor } from 'xstate'
  import { onDestroy } from 'svelte'
  import { dashboardMachine } from '$lib/machines/dashboard.machine'
  import DashboardOverview from '$lib/components/organisms/DashboardOverview.svelte'
  import DashboardEventTable from '$lib/components/organisms/DashboardEventTable.svelte'
  import AnalyticsModal from '$lib/components/organisms/AnalyticsModal.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'

  const actor = createActor(dashboardMachine)
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => { sub.unsubscribe(); actor.stop() })

  const stateValue = $derived(snapshot.value as string)
  const ctx = $derived(snapshot.context)

  const analyticsOpen = $derived(
    stateValue === 'loadingAnalytics' || stateValue === 'showingAnalytics'
  )
  const analyticsLoading = $derived(stateValue === 'loadingAnalytics')
</script>

<div class="flex flex-col gap-8">
  <!-- Page heading -->
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-bold text-gray-900">Tableau de bord</h1>
    <button
      type="button"
      onclick={() => actor.send({ type: 'RELOAD' })}
      disabled={stateValue === 'loading'}
      class="rounded-btn border border-border bg-white px-3 py-1.5 text-sm text-gray-600
        hover:bg-gray-50 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
        disabled:opacity-50 disabled:cursor-not-allowed"
    >
      ↻ Actualiser
    </button>
  </div>

  <!-- Loading state -->
  {#if stateValue === 'loading'}
    <section aria-label="Chargement du tableau de bord">
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-8">
        {#each { length: 6 } as _, i (i)}
          <div class="bg-white rounded-card shadow-card p-4 flex flex-col gap-2">
            <SkeletonBlock height="h-8" width="w-1/2" rounded="rounded" />
            <SkeletonBlock height="h-3" width="w-3/4" rounded="rounded" />
          </div>
        {/each}
      </div>
      <SkeletonBlock height="h-64" rounded="rounded-card" />
    </section>

  <!-- Error state -->
  {:else if stateValue === 'error'}
    <ErrorBanner
      message={ctx.error ?? 'Impossible de charger le tableau de bord'}
      onretry={() => actor.send({ type: 'RELOAD' })}
    />

  <!-- Ready / analytics states -->
  {:else if ctx.overview}
    <!-- Overview cards -->
    <section aria-label="Vue d'ensemble">
      <DashboardOverview overview={ctx.overview} />
    </section>

    <!-- Events table -->
    <section aria-label="Événements récents">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-semibold text-gray-800">Événements récents</h2>
        <a
          href="/"
          class="text-sm text-wakeve-600 hover:underline
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
        >
          Voir tous →
        </a>
      </div>
      <DashboardEventTable
        events={ctx.events}
        onselectevent={(id) => actor.send({ type: 'SELECT_EVENT', id })}
      />
    </section>
  {/if}

  <!-- Analytics modal -->
  <AnalyticsModal
    open={analyticsOpen}
    analytics={ctx.analytics}
    loading={analyticsLoading}
    error={ctx.analyticsError}
    onclose={() => actor.send({ type: 'CLOSE_ANALYTICS' })}
  />
</div>
