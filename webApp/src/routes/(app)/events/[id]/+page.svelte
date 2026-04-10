<script lang="ts">
  import { page } from '$app/stores'
  import { createActor } from 'xstate'
  import { onDestroy } from 'svelte'
  import { eventDetailMachine } from '$lib/machines/eventDetail.machine'
  import type { DetailTab } from '$lib/machines/eventDetail.machine'
  import type { CommentSection, VoteValue } from '$lib/types/api'
  import { useAuth } from '$lib/actors/auth.actor.svelte'
  import Badge from '$lib/components/atoms/Badge.svelte'
  import Spinner from '$lib/components/atoms/Spinner.svelte'
  import PollPanel from '$lib/components/organisms/PollPanel.svelte'
  import CommentPanel from '$lib/components/organisms/CommentPanel.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import { EVENT_TYPE_ICONS, EVENT_TYPE_LABELS } from '$lib/utils/event-type'
  import { formatDate, formatDateTime } from '$lib/utils/date'
  import { formatSlotLabel } from '$lib/utils/slot'

  const { snapshot: authSnapshot } = useAuth()
  const eventId = $page.params.id

  const actor = createActor(eventDetailMachine, { input: { eventId } })
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => { sub.unsubscribe(); actor.stop() })

  const currentUserId = $derived(authSnapshot.context.user?.id ?? '')
  const ctx = $derived(snapshot.context)
  const stateValue = $derived(snapshot.value as string)

  const tabs: { key: DetailTab; label: string }[] = [
    { key: 'info', label: 'Infos' },
    { key: 'poll', label: 'Sondage' },
    { key: 'comments', label: 'Commentaires' }
  ]

  function switchTab(tab: DetailTab) {
    actor.send({ type: 'SWITCH_TAB', tab })
  }

  function handleVote(slotId: string, value: VoteValue) {
    if (!currentUserId) return
    actor.send({ type: 'VOTE', slotId, value, participantId: currentUserId })
  }

  function handleAddComment(content: string, section: CommentSection) {
    actor.send({ type: 'ADD_COMMENT', content, section })
  }
</script>

{#if stateValue === 'loading'}
  <!-- Loading skeleton -->
  <div class="flex flex-col gap-6">
    <div class="flex items-start gap-4">
      <div class="flex-1 flex flex-col gap-2">
        <SkeletonBlock height="h-8" width="w-2/3" rounded="rounded-lg" />
        <SkeletonBlock height="h-4" width="w-1/3" rounded="rounded" />
      </div>
    </div>
    <div class="flex gap-2">
      {#each { length: 3 } as _, i (i)}
        <SkeletonBlock height="h-9" width="w-24" rounded="rounded-btn" />
      {/each}
    </div>
    <SkeletonBlock height="h-48" rounded="rounded-card" />
  </div>

{:else if stateValue === 'error'}
  <div class="flex flex-col items-center gap-4 py-12">
    <ErrorBanner message={ctx.error ?? 'Impossible de charger l\'événement'} onretry={() => actor.send({ type: 'RELOAD' })} />
  </div>

{:else if ctx.event}
  {@const event = ctx.event}

  <div class="flex flex-col gap-6">

    <!-- Page header -->
    <div class="flex flex-col gap-2">
      <div class="flex items-start justify-between gap-4 flex-wrap">
        <div class="flex flex-col gap-1 flex-1">
          <h1 class="text-2xl font-bold text-gray-900 text-balance">{event.title}</h1>
          <div class="flex items-center gap-2 flex-wrap">
            <Badge status={event.status} size="md" />
            <span class="flex items-center gap-1 text-sm text-gray-500">
              <span aria-hidden="true">{EVENT_TYPE_ICONS[event.type] ?? '📅'}</span>
              <span>{EVENT_TYPE_LABELS[event.type] ?? event.type}</span>
            </span>
          </div>
        </div>
        <button
          type="button"
          onclick={() => actor.send({ type: 'RELOAD' })}
          class="shrink-0 rounded-btn border border-border bg-white px-3 py-1.5 text-sm text-gray-600
            hover:bg-gray-50 transition-default
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
          aria-label="Actualiser"
        >
          ↻ Actualiser
        </button>
      </div>
    </div>

    <!-- Tab nav -->
    <div class="border-b border-border">
      <nav class="flex gap-0 -mb-px" aria-label="Onglets">
        {#each tabs as tab (tab.key)}
          {@const isActive = ctx.activeTab === tab.key}
          <button
            type="button"
            onclick={() => switchTab(tab.key)}
            class="px-5 py-2.5 text-sm font-medium border-b-2 transition-default
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded-t-sm
              {isActive
                ? 'border-wakeve-600 text-wakeve-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
            aria-current={isActive ? 'page' : undefined}
          >
            {tab.label}
            {#if tab.key === 'comments' && ctx.comments.length > 0}
              <span class="ml-1.5 rounded-full bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">
                {ctx.comments.length}
              </span>
            {/if}
          </button>
        {/each}
      </nav>
    </div>

    <!-- Tab panels -->
    <div class="min-h-[16rem]">

      <!-- Info tab -->
      {#if ctx.activeTab === 'info'}
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <!-- Details card -->
          <div class="bg-white rounded-card shadow-card p-5 flex flex-col gap-4">
            <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">Détails</h2>

            <dl class="flex flex-col gap-3 text-sm">
              {#if event.description}
                <div>
                  <dt class="text-xs text-gray-500 mb-0.5">Description</dt>
                  <dd class="text-gray-800 whitespace-pre-wrap">{event.description}</dd>
                </div>
              {/if}

              <div class="flex items-center justify-between">
                <dt class="text-gray-500">Statut</dt>
                <dd><Badge status={event.status} size="sm" /></dd>
              </div>

              <div class="flex items-center justify-between">
                <dt class="text-gray-500">Participants</dt>
                <dd class="font-medium text-gray-800">
                  <span aria-hidden="true">👥</span> {event.participantCount}
                </dd>
              </div>

              {#if event.deadline}
                <div class="flex items-center justify-between">
                  <dt class="text-gray-500">Date limite</dt>
                  <dd class="font-medium text-gray-800">{formatDate(event.deadline)}</dd>
                </div>
              {/if}

              {#if event.finalDate}
                <div class="flex items-center justify-between">
                  <dt class="text-gray-500">Date confirmée</dt>
                  <dd class="font-medium text-green-700">{formatDateTime(event.finalDate)}</dd>
                </div>
              {/if}

              <div class="flex items-center justify-between">
                <dt class="text-gray-500">Créé le</dt>
                <dd class="text-gray-700">{formatDate(event.createdAt)}</dd>
              </div>
            </dl>
          </div>

          <!-- Slots summary card -->
          <div class="bg-white rounded-card shadow-card p-5 flex flex-col gap-3">
            <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
              Créneaux proposés
              <span class="ml-1.5 text-xs font-normal text-gray-400 normal-case">
                ({event.proposedSlots.length})
              </span>
            </h2>

            {#if event.proposedSlots.length === 0}
              <p class="text-sm text-gray-500">Aucun créneau proposé.</p>
            {:else}
              <ul class="flex flex-col gap-2" role="list">
                {#each event.proposedSlots as slot (slot.id)}
                  <li class="flex items-center justify-between rounded-btn bg-gray-50 px-3 py-2 text-sm">
                    <span class="text-gray-800">{formatSlotLabel(slot)}</span>
                    {#if slot.voteCount != null}
                      <span class="text-xs text-gray-400">{slot.voteCount} vote{slot.voteCount !== 1 ? 's' : ''}</span>
                    {/if}
                  </li>
                {/each}
              </ul>
            {/if}
          </div>
        </div>

      <!-- Poll tab -->
      {:else if ctx.activeTab === 'poll'}
        <PollPanel
          poll={ctx.poll}
          {currentUserId}
          onvote={handleVote}
          voteerror={ctx.voteError}
          isvoting={stateValue === 'voting'}
        />

      <!-- Comments tab -->
      {:else if ctx.activeTab === 'comments'}
        <CommentPanel
          comments={ctx.comments}
          {currentUserId}
          onaddcomment={handleAddComment}
          commenterror={ctx.commentError}
          iscommenting={stateValue === 'addingComment'}
        />
      {/if}
    </div>

  </div>

{:else}
  <!-- No event loaded yet but not in error state -->
  <div class="flex items-center justify-center py-16">
    <Spinner size="lg" />
  </div>
{/if}
