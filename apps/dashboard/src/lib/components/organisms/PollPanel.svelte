<script lang="ts">
  import type { PollResponse, VoteValue } from '$lib/types/api'
  import VoteButtons from '$lib/components/molecules/VoteButtons.svelte'
  import VoteBar from '$lib/components/molecules/VoteBar.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import { formatSlotLabel } from '$lib/utils/slot'
  import { formatDate } from '$lib/utils/date'

  interface Props {
    poll: PollResponse | null
    currentUserId: string
    onvote: (slotId: string, value: VoteValue) => void
    voteerror: string | null
    isvoting: boolean
  }

  const { poll, currentUserId, onvote, voteerror, isvoting }: Props = $props()

  function getVoteCounts(slotId: string): { yes: number; maybe: number; no: number } {
    if (!poll) return { yes: 0, maybe: 0, no: 0 }
    let yes = 0, maybe = 0, no = 0
    for (const userVotes of Object.values(poll.votes)) {
      const v = userVotes[slotId]
      if (v === 'YES') yes++
      else if (v === 'MAYBE') maybe++
      else if (v === 'NO') no++
    }
    return { yes, maybe, no }
  }

  function getCurrentVote(slotId: string): VoteValue | null {
    if (!poll || !currentUserId) return null
    const userVotes = poll.votes[currentUserId]
    if (!userVotes) return null
    return (userVotes[slotId] as VoteValue) ?? null
  }
</script>

<div class="flex flex-col gap-4">
  {#if voteerror}
    <ErrorBanner message={voteerror} />
  {/if}

  {#if !poll || poll.slots.length === 0}
    <div class="flex flex-col items-center justify-center py-12 text-center gap-3">
      <span class="text-4xl" aria-hidden="true">🗳️</span>
      <p class="text-sm text-gray-500">Aucun créneau proposé pour ce sondage.</p>
    </div>
  {:else}
    <p class="text-sm text-gray-600">
      {poll.participantCount} participant{poll.participantCount !== 1 ? 's' : ''} ·
      {poll.slots.length} créneau{poll.slots.length !== 1 ? 'x' : ''}
    </p>

    <ul class="flex flex-col gap-3" role="list">
      {#each poll.slots as slot (slot.id)}
        {@const counts = getVoteCounts(slot.id)}
        {@const currentVote = getCurrentVote(slot.id)}
        {@const label = formatSlotLabel(slot)}
        {@const dateLabel = slot.startTime ? formatDate(slot.startTime) : ''}

        <li class="rounded-card border border-border bg-white p-4 flex flex-col gap-3">
          <!-- Slot label -->
          <div class="flex flex-col gap-0.5">
            <span class="text-sm font-semibold text-gray-900">{label}</span>
            {#if dateLabel}
              <span class="text-xs text-gray-500">{dateLabel}</span>
            {/if}
          </div>

          <!-- Vote bar -->
          <VoteBar
            yesCount={counts.yes}
            maybeCount={counts.maybe}
            noCount={counts.no}
            totalParticipants={poll.participantCount}
          />

          <!-- Vote buttons -->
          <VoteButtons
            slotId={slot.id}
            {currentVote}
            onvote={onvote}
            disabled={isvoting}
          />
        </li>
      {/each}
    </ul>
  {/if}
</div>
