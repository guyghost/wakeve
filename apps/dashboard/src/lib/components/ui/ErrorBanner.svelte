<script lang="ts">
  interface Props {
    message: string
    onretry?: () => void
  }

  const { message, onretry }: Props = $props()
</script>

<div
  role="alert"
  aria-live="assertive"
  aria-atomic="true"
  class="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3"
>
  <!-- Exclamation icon -->
  <div class="shrink-0 mt-0.5" aria-hidden="true">
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 20 20"
      fill="currentColor"
      class="h-5 w-5 text-red-500"
    >
      <path
        fill-rule="evenodd"
        d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495ZM10 5a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 10 5Zm0 9a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"
        clip-rule="evenodd"
      />
    </svg>
  </div>

  <!-- Message + retry -->
  <div class="flex-1">
    <p class="text-sm font-medium text-red-800">{message}</p>
    {#if onretry}
      <button
        type="button"
        onclick={onretry}
        class="mt-1.5 text-xs font-semibold text-red-700 underline underline-offset-2
          hover:text-red-900 transition-default
          focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-600 rounded"
      >
        Réessayer
      </button>
    {/if}
  </div>
</div>
