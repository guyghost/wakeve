<script lang="ts">
  import type { Snippet } from 'svelte'

  interface Props {
    label: string
    class?: string
    children: Snippet
  }

  const { label, class: extraClass = '', children }: Props = $props()
</script>

<span class="group relative inline-flex {extraClass}" title={label}>
  {@render children()}
  <span
    role="tooltip"
    class="pointer-events-none absolute bottom-full left-1/2 z-20 mb-2 hidden -translate-x-1/2 whitespace-nowrap rounded-md bg-slate-900 px-2 py-1 text-xs font-medium text-white shadow-sm group-hover:block group-focus-within:block"
  >
    {label}
  </span>
</span>
