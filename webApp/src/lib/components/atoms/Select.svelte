<script lang="ts">
  interface Option {
    value: string
    label: string
  }

  interface Props {
    id: string
    name?: string
    value?: string
    label?: string
    error?: string
    options: Option[]
    required?: boolean
    disabled?: boolean
    onchange?: (e: Event & { currentTarget: HTMLSelectElement }) => void
    class?: string
  }

  const {
    id,
    name,
    value = '',
    label = '',
    error = '',
    options,
    required = false,
    disabled = false,
    onchange,
    class: extraClass = ''
  }: Props = $props()
</script>

<div class="flex flex-col gap-1 {extraClass}">
  {#if label}
    <label for={id} class="text-sm font-medium text-gray-700">
      {label}{#if required}<span class="text-red-500 ml-1" aria-hidden="true">*</span>{/if}
    </label>
  {/if}

  <div class="relative">
    <select
      {id}
      {name}
      {required}
      {disabled}
      {onchange}
      class="w-full appearance-none rounded-btn border px-3 py-2 pr-9 text-sm text-gray-900 transition-default bg-white
        {error
          ? 'border-red-400 focus-visible:outline-red-500'
          : 'border-border focus-visible:outline-wakeve-500'}
        disabled:bg-gray-50 disabled:text-gray-500"
      aria-invalid={!!error || undefined}
      aria-describedby={error ? `${id}-error` : undefined}
    >
      {#each options as option (option.value)}
        <option value={option.value} selected={option.value === value}>
          {option.label}
        </option>
      {/each}
    </select>

    <!-- Chevron icon -->
    <div
      class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2.5 text-gray-400"
      aria-hidden="true"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 20 20"
        fill="currentColor"
        class="h-4 w-4"
      >
        <path
          fill-rule="evenodd"
          d="M5.22 8.22a.75.75 0 0 1 1.06 0L10 11.94l3.72-3.72a.75.75 0 1 1 1.06 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L5.22 9.28a.75.75 0 0 1 0-1.06Z"
          clip-rule="evenodd"
        />
      </svg>
    </div>
  </div>

  {#if error}
    <p id="{id}-error" class="text-xs text-red-600" role="alert">{error}</p>
  {/if}
</div>
