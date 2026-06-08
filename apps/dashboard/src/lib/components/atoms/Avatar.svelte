<script lang="ts">
  interface Props {
    name: string
    size?: 'sm' | 'md' | 'lg'
  }
  const { name, size = 'md' }: Props = $props()

  const BG_COLORS = [
    'bg-violet-500', 'bg-blue-500', 'bg-green-500',
    'bg-orange-500', 'bg-pink-500', 'bg-teal-500'
  ]

  function computeInitials(n: string): string {
    const words = n.trim().split(/\s+/).filter(Boolean)
    if (words.length === 0) return '?'
    if (words.length === 1) return words[0][0].toUpperCase()
    return (words[0][0] + words[words.length - 1][0]).toUpperCase()
  }

  function computeBgColor(n: string): string {
    let hash = 0
    for (let i = 0; i < n.length; i++) {
      hash = (hash * 31 + n.charCodeAt(i)) | 0
    }
    return BG_COLORS[Math.abs(hash) % BG_COLORS.length]
  }

  const initials = $derived(computeInitials(name))
  const bgColor = $derived(computeBgColor(name))

  const sizeClasses = $derived(
    size === 'sm' ? 'h-6 w-6 text-xs'
    : size === 'lg' ? 'h-16 w-16 text-xl'
    : 'h-9 w-9 text-sm'
  )
</script>

<div
  class="inline-flex items-center justify-center rounded-full font-semibold text-white select-none flex-shrink-0 {bgColor} {sizeClasses}"
  aria-label={name}
  role="img"
>
  {initials}
</div>
