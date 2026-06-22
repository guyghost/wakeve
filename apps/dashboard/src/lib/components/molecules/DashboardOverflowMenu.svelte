<script lang="ts">
  interface Props {
    copied: boolean
    ondetails: () => void
    oncopy: () => void
  }

  const { copied, ondetails, oncopy }: Props = $props()
</script>

<details class="relative">
  <summary
    class="flex h-8 w-8 cursor-pointer list-none items-center justify-center rounded-btn text-slate-500 transition-default hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
    aria-label="Actions secondaires"
    title="Actions secondaires"
  >
    <span aria-hidden="true">...</span>
  </summary>
  <div class="absolute right-0 top-9 z-10 w-44 overflow-hidden rounded-lg border border-slate-200 bg-white py-1 text-sm shadow-lg">
    <button
      type="button"
      onclick={ondetails}
      class="flex w-full items-center justify-between px-3 py-2 text-left text-slate-700 transition-default hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
    >
      Détails
    </button>
    <button
      type="button"
      onclick={oncopy}
      class="flex w-full items-center justify-between px-3 py-2 text-left text-slate-700 transition-default hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
    >
      Copier le lien
      {#if copied}
        <span class="text-xs font-medium text-emerald-700">Copié</span>
      {/if}
    </button>
  </div>
</details>
