<script lang="ts">
  import type { EventResponse } from '$lib/types/api'
  import Badge from '$lib/components/atoms/Badge.svelte'
  import { EVENT_TYPE_ICONS, EVENT_TYPE_LABELS } from '$lib/utils/event-type'
  import { formatDate } from '$lib/utils/date'

  interface Props {
    event: EventResponse
  }

  const { event }: Props = $props()

  const typeIcon = $derived(EVENT_TYPE_ICONS[event.type] ?? '📅')
  const typeLabel = $derived(EVENT_TYPE_LABELS[event.type] ?? event.type)
  const slotCount = $derived(event.proposedSlots?.length ?? 0)
  const deadlineFormatted = $derived(event.deadline ? formatDate(event.deadline) : null)
</script>

<a
  href="/events/{event.id}"
  class="group block bg-white rounded-card shadow-card p-4 transition-default
    hover:shadow-lg hover:-translate-y-0.5 hover:ring-1 hover:ring-wakeve-200
    focus-visible:outline-wakeve-500"
>
  <!-- Header row: title + badge -->
  <div class="flex items-start justify-between gap-2 mb-1">
    <h3 class="font-semibold text-gray-900 text-sm leading-snug line-clamp-1 flex-1">
      {event.title}
    </h3>
    <Badge status={event.status} size="sm" />
  </div>

  <!-- Description -->
  {#if event.description}
    <p class="text-gray-500 text-xs leading-relaxed line-clamp-2 mb-3">
      {event.description}
    </p>
  {:else}
    <div class="mb-3"></div>
  {/if}

  <!-- Type row -->
  <div class="flex items-center gap-1.5 text-xs text-gray-600 mb-3">
    <span aria-hidden="true">{typeIcon}</span>
    <span>{typeLabel}</span>
  </div>

  <!-- Footer -->
  <div class="flex items-center gap-3 text-xs text-gray-400 border-t border-gray-100 pt-3">
    <span class="flex items-center gap-1">
      <span aria-hidden="true">👥</span>
      <span>{event.participantCount}</span>
    </span>
    <span class="flex items-center gap-1">
      <span aria-hidden="true">📅</span>
      <span>{slotCount}</span>
    </span>
    {#if deadlineFormatted}
      <span class="ml-auto text-gray-400 truncate">
        Limite&nbsp;: {deadlineFormatted}
      </span>
    {/if}
  </div>
</a>
