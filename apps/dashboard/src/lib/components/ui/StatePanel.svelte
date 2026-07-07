<script lang="ts">
  type StateTone = 'empty' | 'error' | 'offline' | 'permission' | 'info' | 'success' | 'archived'

  interface Props {
    tone?: StateTone
    title: string
    description: string
    actionLabel?: string
    actionHref?: string
    onaction?: () => void
    onretry?: () => void
    compact?: boolean
  }

  const {
    tone = 'info',
    title,
    description,
    actionLabel,
    actionHref,
    onaction,
    onretry,
    compact = false
  }: Props = $props()

  const toneClasses = $derived(
    {
      empty: 'border-slate-200 bg-white text-slate-700',
      error: 'border-red-200 bg-red-50 text-red-800',
      offline: 'border-amber-200 bg-amber-50 text-amber-900',
      permission: 'border-slate-300 bg-slate-100 text-slate-800',
      info: 'border-sky-200 bg-sky-50 text-sky-900',
      success: 'border-emerald-200 bg-emerald-50 text-emerald-900',
      archived: 'border-slate-200 bg-slate-50 text-slate-700'
    }[tone]
  )
</script>

<div
  role={tone === 'error' || tone === 'offline' || tone === 'permission' ? 'alert' : 'status'}
  class="rounded-lg border {toneClasses} {compact ? 'px-4 py-3' : 'px-5 py-5'}"
>
  <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
    <div class="min-w-0">
      <p class="text-sm font-semibold">{title}</p>
      <p class="mt-1 text-sm opacity-80">{description}</p>
    </div>

    {#if actionLabel && (actionHref || onaction || onretry)}
      {#if actionHref}
        <a
          href={actionHref}
          class="inline-flex shrink-0 items-center justify-center rounded-btn bg-white px-3 py-1.5 text-sm font-medium text-slate-900 shadow-sm ring-1 ring-inset ring-slate-200 transition-default hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
        >
          {actionLabel}
        </a>
      {:else}
        <button
          type="button"
          onclick={onaction ?? onretry}
          class="inline-flex shrink-0 items-center justify-center rounded-btn bg-white px-3 py-1.5 text-sm font-medium text-slate-900 shadow-sm ring-1 ring-inset ring-slate-200 transition-default hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
        >
          {actionLabel}
        </button>
      {/if}
    {/if}
  </div>
</div>
