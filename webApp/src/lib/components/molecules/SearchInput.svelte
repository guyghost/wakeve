<script lang="ts">
  interface Props {
    value: string
    placeholder?: string
    onsearch: (query: string) => void
  }

  const { value, placeholder = 'Rechercher…', onsearch }: Props = $props()

  let inputValue = $state(value)
  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  $effect(() => {
    inputValue = value
  })

  function handleInput(e: Event & { currentTarget: HTMLInputElement }) {
    inputValue = e.currentTarget.value
    if (debounceTimer !== null) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      onsearch(inputValue)
    }, 300)
  }

  function handleClear() {
    inputValue = ''
    if (debounceTimer !== null) clearTimeout(debounceTimer)
    onsearch('')
  }

  const hasValue = $derived(inputValue.length > 0)
</script>

<div class="relative flex items-center">
  <!-- Magnifier icon -->
  <div class="pointer-events-none absolute left-3 text-gray-400" aria-hidden="true">
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 20 20"
      fill="currentColor"
      class="h-4 w-4"
    >
      <path
        fill-rule="evenodd"
        d="M9 3.5a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11ZM2 9a7 7 0 1 1 12.452 4.391l3.328 3.329a.75.75 0 1 1-1.06 1.06l-3.329-3.328A7 7 0 0 1 2 9Z"
        clip-rule="evenodd"
      />
    </svg>
  </div>

  <input
    type="search"
    value={inputValue}
    {placeholder}
    oninput={handleInput}
    class="w-full rounded-btn border border-border bg-white py-2 pl-9 pr-9 text-sm text-gray-900
      placeholder-gray-400 transition-default
      focus-visible:outline-wakeve-500"
    aria-label={placeholder}
  />

  <!-- Clear button -->
  {#if hasValue}
    <button
      type="button"
      onclick={handleClear}
      aria-label="Effacer la recherche"
      class="absolute right-2.5 flex items-center justify-center rounded-full p-0.5
        text-gray-400 hover:text-gray-600 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 20 20"
        fill="currentColor"
        class="h-4 w-4"
        aria-hidden="true"
      >
        <path
          d="M6.28 5.22a.75.75 0 0 0-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 1 0 1.06 1.06L10 11.06l3.72 3.72a.75.75 0 1 0 1.06-1.06L11.06 10l3.72-3.72a.75.75 0 0 0-1.06-1.06L10 8.94 6.28 5.22Z"
        />
      </svg>
    </button>
  {/if}
</div>
