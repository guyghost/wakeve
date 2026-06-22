<script lang="ts">
  interface Props {
    label: string
    value: string | number
    helper?: string
    tone?: 'neutral' | 'attention' | 'success' | 'risk'
  }

  const { label, value, helper, tone = 'neutral' }: Props = $props()

  const toneClasses = $derived(
    {
      neutral: 'border-slate-200 bg-white',
      attention: 'border-amber-200 bg-amber-50',
      success: 'border-emerald-200 bg-emerald-50',
      risk: 'border-red-200 bg-red-50'
    }[tone]
  )
</script>

<div class="rounded-lg border {toneClasses} p-4">
  <div class="flex items-start justify-between gap-4">
    <p class="text-sm font-medium text-slate-600">{label}</p>
    <p class="shrink-0 text-right text-2xl font-semibold tabular-nums text-slate-950">{value}</p>
  </div>
  {#if helper}
    <p class="mt-2 text-xs leading-5 text-slate-500">{helper}</p>
  {/if}
</div>
