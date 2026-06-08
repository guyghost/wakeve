<script lang="ts">
  interface Props {
    height?: string
    width?: string
    rounded?: string
  }

  const { height = 'h-4', width = 'w-full', rounded = 'rounded' }: Props = $props()
</script>

<div
  class="animate-pulse bg-gray-200 {height} {width} {rounded}"
  aria-hidden="true"
></div>
