<script lang="ts">
  import type { EventStatus } from '$lib/types/api'
  import { STATUS_COLORS, STATUS_LABELS } from '$lib/utils/event-type'

  interface Props {
    status: EventStatus
    size?: 'sm' | 'md'
  }

  const { status, size = 'sm' }: Props = $props()

  const colors = $derived(STATUS_COLORS[status])
  const label = $derived(STATUS_LABELS[status])
  const sizeClass = $derived(size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-sm')
</script>

<span
  role="status"
  class="inline-flex items-center font-medium rounded-full {sizeClass} {colors.bg} {colors.text}"
>
  {label}
</span>
