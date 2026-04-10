<script lang="ts">
  interface Props {
    message: string
    type?: 'success' | 'error' | 'info'
    duration?: number
    ondismiss: () => void
  }

  const { message, type = 'info', duration = 3000, ondismiss }: Props = $props()

  let visible = $state(false)

  // Slide-in on mount, auto-dismiss after duration
  $effect(() => {
    // Trigger animation on next tick
    const showTimer = setTimeout(() => { visible = true }, 10)
    const hideTimer = setTimeout(() => {
      visible = false
      // Allow slide-out animation before removing
      setTimeout(ondismiss, 300)
    }, duration)

    return () => {
      clearTimeout(showTimer)
      clearTimeout(hideTimer)
    }
  })

  const config = $derived(
    {
      success: {
        bg: 'bg-green-50 border-green-200',
        icon: '✅',
        text: 'text-green-800'
      },
      error: {
        bg: 'bg-red-50 border-red-200',
        icon: '❌',
        text: 'text-red-800'
      },
      info: {
        bg: 'bg-blue-50 border-blue-200',
        icon: 'ℹ️',
        text: 'text-blue-800'
      }
    }[type]
  )
</script>

<div
  role="status"
  aria-live="polite"
  aria-atomic="true"
  class="fixed top-4 right-4 z-50 flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg
    max-w-sm w-full transition-all duration-300
    {config.bg} {config.text}
    {visible ? 'translate-x-0 opacity-100' : 'translate-x-full opacity-0'}"
>
  <span class="text-base shrink-0 mt-0.5" aria-hidden="true">{config.icon}</span>

  <p class="flex-1 text-sm font-medium leading-snug">{message}</p>

  <button
    type="button"
    onclick={() => {
      visible = false
      setTimeout(ondismiss, 300)
    }}
    class="shrink-0 rounded-md p-0.5 opacity-60 hover:opacity-100 transition-default
      focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-current"
    aria-label="Fermer la notification"
  >
    <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
      <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>
  </button>
</div>
