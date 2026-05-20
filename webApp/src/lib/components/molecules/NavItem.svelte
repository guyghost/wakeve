<script lang="ts">
  import type { Snippet } from 'svelte'

  interface Props {
    href: string
    label: string
    active?: boolean
    icon: Snippet
  }

  const { href, label, active = false, icon }: Props = $props()
</script>

<a
  {href}
  class="flex flex-col items-center gap-0.5 px-3 py-1.5 rounded-btn transition-default
    focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
    {active
      ? 'text-wakeve-600 font-medium'
      : 'text-gray-500 hover:text-gray-900'}"
  aria-current={active ? 'page' : undefined}
>
  <span class="h-5 w-5 flex items-center justify-center" aria-hidden="true">
    {@render icon()}
  </span>
  <span class="text-xs leading-none">{label}</span>
</a>
