<script lang="ts">
  interface Props {
    id: string
    name?: string
    type?: string
    value?: string
    placeholder?: string
    label?: string
    error?: string
    required?: boolean
    disabled?: boolean
    oninput?: (e: Event & { currentTarget: HTMLInputElement }) => void
    class?: string
  }

  const {
    id,
    name,
    type = 'text',
    value = '',
    placeholder = '',
    label = '',
    error = '',
    required = false,
    disabled = false,
    oninput,
    class: extraClass = ''
  }: Props = $props()
</script>

<div class="flex flex-col gap-1 {extraClass}">
  {#if label}
    <label for={id} class="text-sm font-medium text-gray-700">
      {label}{#if required}<span class="text-red-500 ml-1" aria-hidden="true">*</span>{/if}
    </label>
  {/if}
  <input
    {id}
    {name}
    {type}
    {value}
    {placeholder}
    {required}
    {disabled}
    {oninput}
    class="w-full rounded-btn border px-3 py-2 text-sm text-gray-900 placeholder-gray-400 transition-default
      {error
        ? 'border-red-400 focus-visible:outline-red-500'
        : 'border-border focus-visible:outline-wakeve-500'}
      disabled:bg-gray-50 disabled:text-gray-500"
    aria-invalid={!!error || undefined}
    aria-describedby={error ? `${id}-error` : undefined}
  />
  {#if error}
    <p id="{id}-error" class="text-xs text-red-600" role="alert">{error}</p>
  {/if}
</div>
