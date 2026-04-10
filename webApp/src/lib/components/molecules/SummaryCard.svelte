<script lang="ts">
  interface Props {
    icon: string
    label: string
    value: string | number
    color?: string
  }

  const { icon, label, value, color = 'bg-blue-50' }: Props = $props()
</script>

<div class="relative rounded-card shadow-card p-4 {color} overflow-hidden">
  <!-- Icon: top-right corner -->
  <div
    class="absolute top-3 right-3 text-2xl leading-none select-none"
    aria-hidden="true"
  >
    {icon}
  </div>

  <!-- Value -->
  <p class="text-2xl font-bold text-gray-900 leading-none mb-1">
    {value}
  </p>

  <!-- Label -->
  <p class="text-sm text-gray-600 leading-snug pr-8">
    {label}
  </p>
</div>
