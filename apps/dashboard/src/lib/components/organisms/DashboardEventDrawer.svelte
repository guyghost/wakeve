<script lang="ts">
  import type {
    DashboardEventItem,
    EventDetailedAnalyticsResponse
  } from '$lib/types/api'
  import type { DashboardErrorKind } from '$lib/machines/dashboard.machine'
  import Badge from '$lib/components/atoms/Badge.svelte'
  import ProgressBar from '$lib/components/atoms/ProgressBar.svelte'
  import VoteBar from '$lib/components/molecules/VoteBar.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import StatePanel from '$lib/components/ui/StatePanel.svelte'
  import { formatDate, formatDateTime } from '$lib/utils/date'

  interface Props {
    open: boolean
    event: DashboardEventItem | null
    analytics: EventDetailedAnalyticsResponse | null
    loading: boolean
    error: string | null
    errorKind: DashboardErrorKind | null
    copied: boolean
    onclose: () => void
    oncopy: (id: string) => void
  }

  const {
    open,
    event,
    analytics,
    loading,
    error,
    errorKind,
    copied,
    onclose,
    oncopy
  }: Props = $props()

  let dialogEl: HTMLDialogElement | undefined = $state()

  $effect(() => {
    if (!dialogEl) return
    if (open && !dialogEl.open) dialogEl.showModal()
    if (!open && dialogEl.open) dialogEl.close()
  })

  const stateTone = $derived(
    errorKind === 'offline'
      ? 'offline'
      : errorKind === 'permission' || errorKind === 'auth'
        ? 'permission'
        : 'error'
  )

  const stateTitle = $derived(
    errorKind === 'offline'
      ? 'Connexion indisponible'
      : errorKind === 'permission' || errorKind === 'auth'
        ? 'Accès non autorisé'
        : 'Impossible de charger les détails'
  )

  const maxTimelineCount = $derived.by(() => {
    const counts = [
      ...(analytics?.voteTimeline ?? []).map((entry) => entry.count),
      ...(analytics?.participantTimeline ?? []).map((entry) => entry.count)
    ]
    return Math.max(1, ...counts)
  })

  function handleBackdropClick(e: MouseEvent) {
    if (e.target === dialogEl) onclose()
  }
</script>

<dialog
  bind:this={dialogEl}
  onclick={handleBackdropClick}
  onclose={onclose}
  class="m-0 ml-auto h-full max-h-none w-full max-w-xl rounded-none p-0 shadow-2xl backdrop:bg-slate-950/30"
  aria-modal="true"
  aria-labelledby="dashboard-drawer-title"
>
  {#if event}
    <div class="flex h-full flex-col bg-white">
      <header class="border-b border-slate-200 px-5 py-4">
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0">
            <p class="text-xs font-medium uppercase tracking-wide text-slate-500">Détail événement</p>
            <h2 id="dashboard-drawer-title" class="mt-1 truncate text-lg font-semibold text-slate-950" title={event.title}>
              {event.title}
            </h2>
            <div class="mt-2 flex flex-wrap items-center gap-2">
              <Badge status={event.status} size="sm" />
              {#if event.isArchived}
                <span class="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs font-medium text-slate-600">
                  Événement archivé
                </span>
              {:else if event.isPollExpired}
                <span class="rounded-full border border-red-200 bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700">
                  Sondage expiré
                </span>
              {/if}
            </div>
          </div>

          <button
            type="button"
            onclick={onclose}
            class="rounded-btn p-2 text-slate-500 transition-default hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
            aria-label="Fermer le détail"
          >
            <span aria-hidden="true">×</span>
          </button>
        </div>
      </header>

      <div class="flex-1 overflow-y-auto px-5 py-5">
        <div class="mb-5 grid grid-cols-2 gap-3">
          <div class="rounded-lg border border-slate-200 p-3">
            <p class="text-xs text-slate-500">Participants</p>
            <p class="mt-1 text-right text-2xl font-semibold tabular-nums text-slate-950">{event.participantCount}</p>
          </div>
          <div class="rounded-lg border border-slate-200 p-3">
            <p class="text-xs text-slate-500">Réponses</p>
            <p class="mt-1 text-right text-2xl font-semibold tabular-nums text-slate-950">{event.responseRatePct}%</p>
          </div>
        </div>

        <div class="mb-5 rounded-lg border border-slate-200 p-4">
          <div class="mb-2 flex items-center justify-between text-sm">
            <span class="font-medium text-slate-700">Progression RSVP / vote</span>
            <span class="text-right font-semibold tabular-nums text-slate-950">{event.pendingParticipants} en attente</span>
          </div>
          <ProgressBar value={event.responseRatePct} color={event.isPollExpired ? 'bg-red-500' : 'bg-wakeve-500'} height="md" />
          <p class="mt-2 text-xs leading-5 text-slate-500">
            {#if event.isVoteClosed}
              Les réponses sont clôturées pour cet événement.
            {:else if event.pendingParticipants > 0}
              Des participants n’ont pas encore répondu. L’action principale reste visible sur la carte.
            {:else}
              Tous les participants attendus ont répondu selon les données dashboard.
            {/if}
          </p>
        </div>

        {#if loading}
          <div class="flex flex-col gap-3" aria-busy="true" aria-label="Chargement des détails">
            <SkeletonBlock height="h-20" rounded="rounded-lg" />
            <SkeletonBlock height="h-24" rounded="rounded-lg" />
            <SkeletonBlock height="h-32" rounded="rounded-lg" />
          </div>
        {:else if error}
          <StatePanel
            tone={stateTone}
            title={stateTitle}
            description={error}
            compact
          />
        {:else if analytics}
          <section class="mb-5 rounded-lg border border-slate-200 p-4">
            <h3 class="text-sm font-semibold text-slate-950">Synthèse du sondage</h3>
            <div class="mt-3 grid grid-cols-3 gap-3 text-sm">
              <div>
                <p class="text-xs text-slate-500">Ont répondu</p>
                <p class="mt-1 text-right font-semibold tabular-nums text-slate-950">{analytics.votedParticipants}</p>
              </div>
              <div>
                <p class="text-xs text-slate-500">En attente</p>
                <p class="mt-1 text-right font-semibold tabular-nums text-slate-950">{analytics.pendingParticipants}</p>
              </div>
              <div>
                <p class="text-xs text-slate-500">Votes</p>
                <p class="mt-1 text-right font-semibold tabular-nums text-slate-950">{analytics.totalVotes}</p>
              </div>
            </div>
          </section>

          {#if analytics.popularTimeSlots.length > 0}
            <section class="mb-5 rounded-lg border border-slate-200 p-4">
              <h3 class="text-sm font-semibold text-slate-950">Créneaux les plus lisibles</h3>
              <ul class="mt-3 flex flex-col gap-3" role="list">
                {#each analytics.popularTimeSlots as slot (slot.slotId)}
                  <li>
                    <div class="mb-1 flex items-center justify-between gap-3">
                      <span class="truncate text-sm font-medium text-slate-800" title={slot.label}>{slot.label}</span>
                      <span class="text-right text-xs tabular-nums text-slate-500">{slot.totalVotes} votes</span>
                    </div>
                    <VoteBar
                      yesCount={slot.yesCount}
                      maybeCount={slot.maybeCount}
                      noCount={slot.noCount}
                      totalParticipants={analytics.totalParticipants}
                    />
                  </li>
                {/each}
              </ul>
            </section>
          {:else}
            <StatePanel
              tone="empty"
              title="Aucun vote détaillé"
              description="Les créneaux n’ont pas encore assez de réponses pour afficher un détail utile."
              compact
            />
          {/if}

          <section class="mb-5 rounded-lg border border-slate-200 p-4">
            <h3 class="text-sm font-semibold text-slate-950">Activité récente</h3>
            <div class="mt-3 grid gap-3 sm:grid-cols-2">
              <div>
                <p class="mb-2 text-xs font-medium text-slate-500">Votes</p>
                {#if analytics.voteTimeline.length === 0}
                  <p class="text-sm text-slate-500">Aucune activité de vote.</p>
                {:else}
                  <ul class="flex flex-col gap-2" role="list">
                    {#each analytics.voteTimeline.slice(-4) as entry (`vote-${entry.date}`)}
                      <li class="grid grid-cols-[1fr_auto] items-center gap-2 text-xs">
                        <span class="truncate text-slate-600" title={entry.date}>{entry.date}</span>
                        <span class="text-right tabular-nums text-slate-900">{entry.count}</span>
                        <ProgressBar value={Math.round((entry.count / maxTimelineCount) * 100)} color="bg-slate-500" height="sm" />
                      </li>
                    {/each}
                  </ul>
                {/if}
              </div>
              <div>
                <p class="mb-2 text-xs font-medium text-slate-500">Participants</p>
                {#if analytics.participantTimeline.length === 0}
                  <p class="text-sm text-slate-500">Aucune arrivée récente.</p>
                {:else}
                  <ul class="flex flex-col gap-2" role="list">
                    {#each analytics.participantTimeline.slice(-4) as entry (`participant-${entry.date}`)}
                      <li class="grid grid-cols-[1fr_auto] items-center gap-2 text-xs">
                        <span class="truncate text-slate-600" title={entry.date}>{entry.date}</span>
                        <span class="text-right tabular-nums text-slate-900">{entry.count}</span>
                        <ProgressBar value={Math.round((entry.count / maxTimelineCount) * 100)} color="bg-emerald-500" height="sm" />
                      </li>
                    {/each}
                  </ul>
                {/if}
              </div>
            </div>
          </section>
        {/if}

        <section class="rounded-lg border border-slate-200 p-4">
          <h3 class="text-sm font-semibold text-slate-950">Données logistiques</h3>
          <p class="mt-1 text-sm leading-5 text-slate-500">
            Budget, transport, logement et tâches ne sont pas exposés par l’API dashboard actuelle. Ouvrez la fiche événement pour consulter ou compléter ces sections.
          </p>
          <div class="mt-3 flex flex-wrap gap-2">
            {#each ['Budget', 'Transport', 'Logement', 'Tâches'] as item (item)}
              <span class="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs font-medium text-slate-600">
                {item} · non disponible ici
              </span>
            {/each}
          </div>
        </section>
      </div>

      <footer class="border-t border-slate-200 px-5 py-4">
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <a
            href="/app/events/{event.id}"
            class="inline-flex items-center justify-center rounded-btn bg-slate-950 px-3 py-2 text-sm font-medium text-white transition-default hover:bg-slate-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-950"
          >
            Ouvrir l’événement
          </a>
          <button
            type="button"
            onclick={() => oncopy(event.id)}
            class="inline-flex items-center justify-center rounded-btn border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-800 transition-default hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
          >
            {copied ? 'Lien copié' : 'Copier le lien'}
          </button>
        </div>
      </footer>
    </div>
  {/if}
</dialog>
