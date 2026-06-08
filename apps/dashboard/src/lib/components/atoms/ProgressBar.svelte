<script lang="ts">
  interface Props {
    value: number
    color?: string
    height?: 'sm' | 'md'
    label?: string
  }

  const { value, color = 'bg-wakeve-600', height = 'sm', label = '' }: Props = $props()

  const clampedValue = $derived(Math.min(100, Math.max(0, value)))

  const heightClass = $derived(
    {
      sm: 'h-1.5',
      md: 'h-2.5'
    }[height]
  )
</script>

<div class="w-full">
  {#if label}
    <div class="flex justify-between mb-1">
      <span class="text-xs text-gray-600">{label}</span>
      <span class="text-xs font-medium text-gray-700">{clampedValue}%</span>
    </div>
  {/if}
  <div
    class="w-full bg-gray-200 rounded-full overflow-hidden {heightClass}"
    role="progressbar"
    aria-valuenow={clampedValue}
    aria-valuemin={0}
    aria-valuemax={100}
    aria-label={label || `${clampedValue}%`}
  >
    <div
      class="{heightClass} {color} rounded-full transition-all duration-500 ease-out"
      style="width: {clampedValue}%"
    ></div>
  </div>
</div>
