<script lang="ts">
  import Spinner from './Spinner.svelte'

  interface Props {
    variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
    size?: 'sm' | 'md' | 'lg'
    disabled?: boolean
    loading?: boolean
    type?: 'button' | 'submit' | 'reset'
    onclick?: (e: MouseEvent) => void
    class?: string
    children: import('svelte').Snippet
  }

  const {
    variant = 'primary',
    size = 'md',
    disabled = false,
    loading = false,
    type = 'button',
    onclick,
    class: extraClass = '',
    children
  }: Props = $props()

  const baseClasses =
    'inline-flex items-center justify-center gap-2 font-medium rounded-btn transition-default focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-50 disabled:cursor-not-allowed'

  const variantClasses = $derived(
    {
      primary:
        'bg-wakeve-600 text-white hover:bg-wakeve-700 focus-visible:outline-wakeve-600',
      secondary:
        'bg-white border border-wakeve-200 text-wakeve-700 hover:bg-wakeve-50 focus-visible:outline-wakeve-500',
      ghost:
        'text-wakeve-600 hover:bg-wakeve-50 focus-visible:outline-wakeve-500',
      danger:
        'bg-red-600 text-white hover:bg-red-700 focus-visible:outline-red-600'
    }[variant]
  )

  const sizeClasses = $derived(
    {
      sm: 'px-3 py-1.5 text-sm',
      md: 'px-4 py-2 text-sm',
      lg: 'px-6 py-3 text-base'
    }[size]
  )
</script>

<button
  {type}
  disabled={disabled || loading}
  {onclick}
  class="{baseClasses} {variantClasses} {sizeClasses} {extraClass}"
>
  {#if loading}
    <Spinner size="sm" color="currentColor" />
  {/if}
  {@render children()}
</button>
