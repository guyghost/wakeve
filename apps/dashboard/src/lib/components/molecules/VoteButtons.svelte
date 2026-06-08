<script lang="ts">
  import type { VoteValue } from '$lib/types/api'

  interface Props {
    slotId: string
    currentVote: VoteValue | null
    onvote: (slotId: string, value: VoteValue) => void
    disabled?: boolean
  }

  const { slotId, currentVote, onvote, disabled = false }: Props = $props()

  interface VoteOption {
    value: VoteValue
    emoji: string
    label: string
    activeClass: string
    inactiveClass: string
  }

  const options: VoteOption[] = [
    {
      value: 'YES',
      emoji: '✅',
      label: 'Oui',
      activeClass: 'bg-green-600 text-white ring-2 ring-green-400 ring-offset-1',
      inactiveClass: 'border border-green-300 text-green-700 hover:bg-green-50'
    },
    {
      value: 'MAYBE',
      emoji: '🤔',
      label: 'Peut-être',
      activeClass: 'bg-amber-500 text-white ring-2 ring-amber-300 ring-offset-1',
      inactiveClass: 'border border-amber-300 text-amber-700 hover:bg-amber-50'
    },
    {
      value: 'NO',
      emoji: '❌',
      label: 'Non',
      activeClass: 'bg-red-600 text-white ring-2 ring-red-400 ring-offset-1',
      inactiveClass: 'border border-red-300 text-red-700 hover:bg-red-50'
    }
  ]
</script>

<div class="flex items-center gap-2" role="group" aria-label="Voter pour ce créneau">
  {#each options as opt (opt.value)}
    {@const isActive = currentVote === opt.value}
    <button
      type="button"
      {disabled}
      aria-pressed={isActive}
      aria-label="{opt.label}{isActive ? ' (sélectionné)' : ''}"
      onclick={() => onvote(slotId, opt.value)}
      class="inline-flex items-center gap-1 rounded-btn px-2.5 py-1 text-xs font-medium transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
        disabled:opacity-50 disabled:cursor-not-allowed
        {isActive ? opt.activeClass : opt.inactiveClass}"
    >
      <span aria-hidden="true">{opt.emoji}</span>
      <span>{opt.label}</span>
    </button>
  {/each}
</div>
