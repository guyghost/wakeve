<script lang="ts">
  interface Props {
    id: string
    name?: string
    value?: string
    placeholder?: string
    label?: string
    error?: string
    rows?: number
    disabled?: boolean
    oninput?: (e: Event & { currentTarget: HTMLTextAreaElement }) => void
    class?: string
  }

  const {
    id,
    name,
    value = '',
    placeholder = '',
    label = '',
    error = '',
    rows = 3,
    disabled = false,
    oninput,
    class: extraClass = ''
  }: Props = $props()
</script>

<div class="flex flex-col gap-1 {extraClass}">
  {#if label}
    <label for={id} class="text-sm font-medium text-gray-700">
      {label}
    </label>
  {/if}
  <textarea
    {id}
    {name}
    {rows}
    {placeholder}
    {disabled}
    {oninput}
    class="w-full rounded-btn border px-3 py-2 text-sm text-gray-900 placeholder-gray-400 transition-default resize-y
      {error
        ? 'border-red-400 focus-visible:outline-red-500'
        : 'border-border focus-visible:outline-wakeve-500'}
      disabled:bg-gray-50 disabled:text-gray-500"
    aria-invalid={!!error || undefined}
    aria-describedby={error ? `${id}-error` : undefined}
  >{value}</textarea>
  {#if error}
    <p id="{id}-error" class="text-xs text-red-600" role="alert">{error}</p>
  {/if}
</div>
