<script lang="ts">
  import type { Snippet } from 'svelte'

  interface Props {
    open: boolean
    title: string
    onclose: () => void
    children: Snippet
    footer?: Snippet
  }

  const { open, title, onclose, children, footer }: Props = $props()

  let dialogEl: HTMLDialogElement | undefined = $state()

  $effect(() => {
    if (!dialogEl) return
    if (open) dialogEl.showModal()
    else dialogEl.close()
  })

  function handleBackdropClick(e: MouseEvent) {
    if (e.target === dialogEl) onclose()
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') {
      e.preventDefault()
      onclose()
    }
  }
</script>

<dialog
  bind:this={dialogEl}
  onclick={handleBackdropClick}
  onkeydown={handleKeydown}
  class="m-auto max-w-2xl w-full rounded-2xl p-0 shadow-2xl backdrop:bg-black/50 backdrop:backdrop-blur-sm open:animate-[dialog-in_200ms_ease-out]"
  aria-modal="true"
  aria-labelledby="modal-title"
>
  <div class="flex flex-col max-h-[90vh]">
    <!-- Header -->
    <div class="flex items-center justify-between px-6 py-4 border-b border-border">
      <h2 id="modal-title" class="text-lg font-semibold text-gray-900">{title}</h2>
      <button
        onclick={onclose}
        class="rounded-lg p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-default focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
        aria-label="Fermer"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Body -->
    <div class="flex-1 overflow-y-auto px-6 py-4">
      {@render children()}
    </div>

    <!-- Footer -->
    {#if footer}
      <div class="px-6 py-4 border-t border-border bg-gray-50 rounded-b-2xl">
        {@render footer()}
      </div>
    {/if}
  </div>
</dialog>

<style>
  @keyframes dialog-in {
    from { opacity: 0; transform: translateY(1rem) scale(0.97); }
    to   { opacity: 1; transform: translateY(0)    scale(1);    }
  }
  dialog::backdrop {
    animation: backdrop-in 200ms ease-out;
  }
  @keyframes backdrop-in {
    from { opacity: 0; }
    to   { opacity: 1; }
  }
</style>
