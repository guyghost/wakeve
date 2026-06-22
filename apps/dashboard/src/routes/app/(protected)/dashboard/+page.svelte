<script lang="ts">
  import { createActor } from 'xstate'
  import { onDestroy, onMount } from 'svelte'
  import { dashboardMachine } from '$lib/machines/dashboard.machine'
  import type { DashboardErrorKind } from '$lib/machines/dashboard.machine'
  import MetricTile from '$lib/components/molecules/MetricTile.svelte'
  import DashboardEventBoard from '$lib/components/organisms/DashboardEventBoard.svelte'
  import DashboardEventDrawer from '$lib/components/organisms/DashboardEventDrawer.svelte'
  import NextActionQueue from '$lib/components/organisms/NextActionQueue.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import StatePanel from '$lib/components/ui/StatePanel.svelte'

  const actor = createActor(dashboardMachine)
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => { sub.unsubscribe(); actor.stop() })

  onMount(() => {
    function syncNetworkState() {
      actor.send({ type: 'NETWORK_CHANGED', isOffline: !navigator.onLine })
    }

    syncNetworkState()
    window.addEventListener('online', syncNetworkState)
    window.addEventListener('offline', syncNetworkState)
    return () => {
      window.removeEventListener('online', syncNetworkState)
      window.removeEventListener('offline', syncNetworkState)
    }
  })

  const stateValue = $derived(snapshot.value as string)
  const ctx = $derived(snapshot.context)
  const detailsOpen = $derived(stateValue === 'loadingAnalytics' || stateValue === 'showingDetails')
  const detailsLoading = $derived(stateValue === 'loadingAnalytics')
  const selectedEvent = $derived(
    ctx.events.find((event) => event.id === ctx.selectedEventId) ?? null
  )
  const responseRateDisplay = $derived(
    ctx.overview ? `${Math.round(ctx.overview.responseRate * 100)}%` : '0%'
  )

  function stateTone(kind: DashboardErrorKind | null): 'error' | 'offline' | 'permission' {
    if (kind === 'offline') return 'offline'
    if (kind === 'permission' || kind === 'auth') return 'permission'
    return 'error'
  }

  function stateTitle(kind: DashboardErrorKind | null): string {
    if (kind === 'offline') return 'Mode hors ligne'
    if (kind === 'permission' || kind === 'auth') return 'Accès au dashboard refusé'
    return 'Impossible de charger le tableau de bord'
  }

  function eventUrl(id: string): string {
    if (typeof window === 'undefined') return `/app/events/${id}`
    return `${window.location.origin}/app/events/${id}`
  }

  async function copyEventLink(id: string) {
    const url = eventUrl(id)
    try {
      await navigator.clipboard.writeText(url)
    } catch {
      const textarea = document.createElement('textarea')
      textarea.value = url
      textarea.setAttribute('readonly', 'true')
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    actor.send({ type: 'COPY_LINK_SUCCESS', id })
    window.setTimeout(() => actor.send({ type: 'CLEAR_COPY_FEEDBACK' }), 1800)
  }
</script>

<div class="flex flex-col gap-6">
  <header class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
    <div class="max-w-2xl">
      <p class="text-sm font-medium text-wakeve-700">Centre d’action</p>
      <h1 class="mt-1 text-2xl font-semibold tracking-normal text-slate-950">Tableau de bord</h1>
      <p class="mt-2 text-sm leading-6 text-slate-500">
        Priorisez les décisions qui débloquent vos événements, puis ouvrez le détail seulement quand il apporte du contexte.
      </p>
    </div>

    <div class="flex flex-wrap items-center gap-2">
      <button
        type="button"
        onclick={() => actor.send({ type: 'RELOAD' })}
        disabled={stateValue === 'loading'}
        class="rounded-btn border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition-default hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 disabled:cursor-not-allowed disabled:opacity-50"
      >
        Actualiser
      </button>
      <a
        href="/app/create"
        class="rounded-btn bg-wakeve-600 px-4 py-2 text-sm font-medium text-white transition-default hover:bg-wakeve-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
      >
        Créer un événement
      </a>
    </div>
  </header>

  {#if ctx.isOffline && stateValue !== 'loading'}
    <StatePanel
      tone="offline"
      title="Vous êtes hors ligne"
      description="Les données affichées peuvent être incomplètes. Les actions qui nécessitent le serveur seront à reprendre une fois reconnecté."
      compact
    />
  {/if}

  {#if stateValue === 'loading'}
    <section aria-label="Chargement du tableau de bord" aria-busy="true" class="flex flex-col gap-5">
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {#each { length: 4 } as _, i (i)}
          <div class="rounded-lg border border-slate-200 bg-white p-4">
            <SkeletonBlock height="h-4" width="w-1/2" rounded="rounded" />
            <SkeletonBlock height="h-8" width="w-1/3" rounded="rounded" />
          </div>
        {/each}
      </div>
      <SkeletonBlock height="h-40" rounded="rounded-lg" />
      <SkeletonBlock height="h-96" rounded="rounded-lg" />
    </section>
  {:else if stateValue === 'error'}
    <StatePanel
      tone={stateTone(ctx.errorKind)}
      title={stateTitle(ctx.errorKind)}
      description={ctx.error ?? 'Réessayez dans quelques instants.'}
      actionLabel="Réessayer"
      onretry={() => actor.send({ type: 'RELOAD' })}
    />
  {:else if ctx.overview}
    <section class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-label="Indicateurs dashboard">
      <MetricTile
        label="Événements actifs"
        value={ctx.overview.activeEvents}
        helper={`${ctx.overview.totalEvents} au total`}
      />
      <MetricTile
        label="Participants"
        value={ctx.overview.totalParticipants}
        helper={`${ctx.overview.averageParticipantsPerEvent.toFixed(1)} par événement en moyenne`}
      />
      <MetricTile
        label="Engagement vote"
        value={responseRateDisplay}
        helper={`${ctx.overview.totalVotes} votes reçus`}
        tone={ctx.overview.responseRate < 0.5 ? 'attention' : 'success'}
      />
      <MetricTile
        label="Notes"
        value={ctx.overview.totalComments}
        helper="Commentaires et signaux de coordination"
      />
    </section>

    {#if ctx.events.length === 0}
      <section class="grid grid-cols-1 gap-4 lg:grid-cols-[1.2fr_0.8fr]">
        <StatePanel
          tone="empty"
          title="Votre premier événement commence ici"
          description="Créez un événement, proposez quelques créneaux, puis laissez Wakeve guider les prochaines décisions sans vous submerger."
          actionLabel="Créer un événement"
          actionHref="/app/create"
        />
        <div class="rounded-lg border border-slate-200 bg-white p-5">
          <h2 class="text-sm font-semibold text-slate-950">Checklist de démarrage</h2>
          <ol class="mt-4 flex flex-col gap-3 text-sm text-slate-600">
            <li class="flex items-start gap-3">
              <span class="mt-0.5 h-5 w-5 shrink-0 rounded-full bg-slate-950 text-center text-xs leading-5 text-white">1</span>
              Définir le type d’événement et les participants attendus.
            </li>
            <li class="flex items-start gap-3">
              <span class="mt-0.5 h-5 w-5 shrink-0 rounded-full bg-slate-950 text-center text-xs leading-5 text-white">2</span>
              Proposer plusieurs dates ou moments de journée.
            </li>
            <li class="flex items-start gap-3">
              <span class="mt-0.5 h-5 w-5 shrink-0 rounded-full bg-slate-950 text-center text-xs leading-5 text-white">3</span>
              Suivre les réponses depuis les prochaines actions.
            </li>
          </ol>
        </div>
      </section>
    {:else}
      <div class="grid grid-cols-1 gap-5 2xl:grid-cols-[24rem_1fr]">
        <NextActionQueue
          events={ctx.events}
          onselectevent={(id) => actor.send({ type: 'SELECT_EVENT', id })}
        />
        <DashboardEventBoard
          events={ctx.events}
          copiedEventId={ctx.copiedEventId}
          onselectevent={(id) => actor.send({ type: 'SELECT_EVENT', id })}
          oncopyevent={copyEventLink}
        />
      </div>
    {/if}
  {/if}

  <DashboardEventDrawer
    open={detailsOpen}
    event={selectedEvent}
    analytics={ctx.analytics}
    loading={detailsLoading}
    error={ctx.analyticsError}
    errorKind={ctx.analyticsErrorKind}
    copied={!!selectedEvent && ctx.copiedEventId === selectedEvent.id}
    onclose={() => actor.send({ type: 'CLOSE_DETAILS' })}
    oncopy={copyEventLink}
  />
</div>
