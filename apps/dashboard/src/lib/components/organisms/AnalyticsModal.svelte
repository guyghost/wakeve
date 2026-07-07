<script lang="ts">
  import type { EventDetailedAnalyticsResponse } from '$lib/types/api'
  import Modal from '$lib/components/ui/Modal.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import VoteBar from '$lib/components/molecules/VoteBar.svelte'
  import ProgressBar from '$lib/components/atoms/ProgressBar.svelte'

  interface Props {
    open: boolean
    analytics: EventDetailedAnalyticsResponse | null
    loading: boolean
    error: string | null
    onclose: () => void
  }

  const { open, analytics, loading, error, onclose }: Props = $props()

  const responseRatePct = $derived(
    analytics ? Math.round(analytics.responseRate * 100) : 0
  )

  // Sort comment sections by count desc
  const commentSections = $derived(
    analytics
      ? Object.entries(analytics.commentsBySection)
          .filter(([, count]) => count > 0)
          .sort((a, b) => b[1] - a[1])
      : []
  )

  const maxCommentCount = $derived(
    commentSections.length > 0 ? commentSections[0][1] : 1
  )
</script>

<Modal {open} title={analytics?.title ?? 'Analytiques'} {onclose}>
  {#snippet children()}
    {#if loading}
      <!-- Skeleton -->
      <div class="flex flex-col gap-4">
        <div class="grid grid-cols-3 gap-3">
          {#each { length: 3 } as _, i (i)}
            <div class="flex flex-col gap-2 rounded-card border border-border p-3">
              <SkeletonBlock height="h-6" width="w-1/2" rounded="rounded" />
              <SkeletonBlock height="h-3" width="w-3/4" rounded="rounded" />
            </div>
          {/each}
        </div>
        {#each { length: 3 } as _, i (i)}
          <SkeletonBlock height="h-14" rounded="rounded-card" />
        {/each}
      </div>

    {:else if error}
      <ErrorBanner message={error} />

    {:else if analytics}
      <!-- Stats row -->
      <div class="grid grid-cols-3 gap-3 mb-6">
        <div class="rounded-card border border-border bg-wakeve-50 p-3 text-center">
          <p class="text-2xl font-bold text-wakeve-700">{responseRatePct}%</p>
          <p class="text-xs text-gray-600 mt-0.5">Taux de réponse</p>
        </div>
        <div class="rounded-card border border-border bg-blue-50 p-3 text-center">
          <p class="text-2xl font-bold text-blue-700">{analytics.totalParticipants}</p>
          <p class="text-xs text-gray-600 mt-0.5">Participants</p>
        </div>
        <div class="rounded-card border border-border bg-green-50 p-3 text-center">
          <p class="text-2xl font-bold text-green-700">{analytics.totalVotes}</p>
          <p class="text-xs text-gray-600 mt-0.5">Votes</p>
        </div>
      </div>

      <!-- Top time slots -->
      {#if analytics.popularTimeSlots.length > 0}
        <section class="mb-6">
          <h3 class="text-sm font-semibold text-gray-800 mb-3">Meilleurs créneaux</h3>
          <ul class="flex flex-col gap-3" role="list">
            {#each analytics.popularTimeSlots as slot (slot.slotId)}
              <li class="flex flex-col gap-1.5">
                <span class="text-sm text-gray-700 font-medium">{slot.label}</span>
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
      {/if}

      <!-- Comments by section -->
      {#if commentSections.length > 0}
        <section>
          <h3 class="text-sm font-semibold text-gray-800 mb-3">Commentaires par section</h3>
          <ul class="flex flex-col gap-2" role="list">
            {#each commentSections as [sectionKey, count] (sectionKey)}
              <li class="flex flex-col gap-1">
                <div class="flex items-center justify-between text-sm">
                  <span class="text-gray-700">{sectionKey}</span>
                  <span class="font-medium text-gray-900">{count}</span>
                </div>
                <ProgressBar
                  value={Math.round((count / maxCommentCount) * 100)}
                  color="bg-wakeve-500"
                  height="sm"
                />
              </li>
            {/each}
          </ul>
        </section>
      {/if}
    {/if}
  {/snippet}
</Modal>
