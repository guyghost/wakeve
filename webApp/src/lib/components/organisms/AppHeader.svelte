<script lang="ts">
  import { page } from '$app/stores'
  import type { UserDTO } from '$lib/types/api'
  import Avatar from '$lib/components/atoms/Avatar.svelte'
  import Button from '$lib/components/atoms/Button.svelte'

  interface Props {
    user: UserDTO | null
    onlogout: () => void
  }

  const { user, onlogout }: Props = $props()

  let mobileMenuOpen = $state(false)
  const currentPath = $derived($page.url.pathname)

  const navLinks = [
    { href: '/', label: 'Événements' },
    { href: '/explore', label: 'Explorer' },
    { href: '/dashboard', label: 'Tableau de bord' }
  ]

  function isActive(href: string): boolean {
    if (href === '/') return currentPath === '/'
    return currentPath.startsWith(href)
  }
</script>

<header class="sticky top-0 z-40 bg-white border-b border-border shadow-sm">
  <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
    <div class="flex h-14 items-center justify-between gap-4">

      <!-- Logo -->
      <a
        href="/"
        class="flex items-center gap-2 font-bold text-wakeve-600 text-lg shrink-0
          focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
      >
        <!-- Wave icon -->
        <svg
          viewBox="0 0 32 32"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          class="h-7 w-7"
          aria-hidden="true"
        >
          <rect width="32" height="32" rx="8" fill="#4f46e5" />
          <path
            d="M4 18c2-4 4-6 6-6s4 4 6 4 4-6 6-6 4 2 6 2"
            stroke="white"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span>Wakeve</span>
      </a>

      <!-- Desktop nav -->
      <nav class="hidden sm:flex items-center gap-1" aria-label="Navigation principale">
        {#each navLinks as link (link.href)}
          <a
            href={link.href}
            class="px-3 py-1.5 rounded-btn text-sm transition-default
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
              {isActive(link.href)
                ? 'text-wakeve-600 font-semibold bg-wakeve-50'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'}"
            aria-current={isActive(link.href) ? 'page' : undefined}
          >
            {link.label}
          </a>
        {/each}
      </nav>

      <!-- Right side: create + user -->
      <div class="hidden sm:flex items-center gap-3">
        <a
          href="/create"
          class="inline-flex items-center gap-1.5 rounded-btn bg-wakeve-600 px-3 py-1.5 text-sm font-medium text-white
            hover:bg-wakeve-700 transition-default
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4" aria-hidden="true">
            <path d="M10.75 4.75a.75.75 0 0 0-1.5 0v4.5h-4.5a.75.75 0 0 0 0 1.5h4.5v4.5a.75.75 0 0 0 1.5 0v-4.5h4.5a.75.75 0 0 0 0-1.5h-4.5v-4.5Z" />
          </svg>
          Créer
        </a>

        {#if user}
          <div class="flex items-center gap-2">
            <a
              href="/profile"
              class="flex items-center gap-2 rounded-btn px-2 py-1 text-sm text-gray-700
                hover:bg-gray-100 transition-default
                focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
            >
              <Avatar name={user.displayName} size="sm" />
              <span class="max-w-[8rem] truncate">{user.displayName}</span>
            </a>
            <button
              type="button"
              onclick={onlogout}
              class="text-xs text-gray-500 hover:text-red-600 transition-default
                focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-500 rounded px-1.5 py-1"
            >
              Déconnexion
            </button>
          </div>
        {:else}
          <a
            href="/login"
            class="text-sm font-medium text-wakeve-600 hover:underline
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
          >
            Connexion
          </a>
        {/if}
      </div>

      <!-- Mobile hamburger -->
      <button
        type="button"
        class="sm:hidden p-2 rounded-btn text-gray-500 hover:text-gray-900 hover:bg-gray-100 transition-default
          focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
        aria-expanded={mobileMenuOpen}
        aria-controls="mobile-menu"
        aria-label="Ouvrir le menu"
        onclick={() => { mobileMenuOpen = !mobileMenuOpen }}
      >
        {#if mobileMenuOpen}
          <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        {:else}
          <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
          </svg>
        {/if}
      </button>
    </div>
  </div>

  <!-- Mobile menu -->
  {#if mobileMenuOpen}
    <div
      id="mobile-menu"
      class="sm:hidden border-t border-border bg-white px-4 py-3 flex flex-col gap-1"
    >
      {#each navLinks as link (link.href)}
        <a
          href={link.href}
          onclick={() => { mobileMenuOpen = false }}
          class="rounded-btn px-3 py-2 text-sm transition-default
            {isActive(link.href)
              ? 'text-wakeve-600 font-semibold bg-wakeve-50'
              : 'text-gray-700 hover:bg-gray-100'}"
          aria-current={isActive(link.href) ? 'page' : undefined}
        >
          {link.label}
        </a>
      {/each}

      <hr class="my-2 border-border" />

      <a
        href="/create"
        onclick={() => { mobileMenuOpen = false }}
        class="rounded-btn px-3 py-2 text-sm font-medium text-wakeve-600 hover:bg-wakeve-50 transition-default"
      >
        + Créer un événement
      </a>

      {#if user}
        <div class="flex items-center justify-between px-3 py-2">
          <div class="flex items-center gap-2">
            <Avatar name={user.displayName} size="sm" />
            <span class="text-sm text-gray-700 truncate max-w-[10rem]">{user.displayName}</span>
          </div>
          <button
            type="button"
            onclick={() => { mobileMenuOpen = false; onlogout() }}
            class="text-xs text-red-500 hover:text-red-700 transition-default"
          >
            Déconnexion
          </button>
        </div>
      {:else}
        <a
          href="/login"
          onclick={() => { mobileMenuOpen = false }}
          class="rounded-btn px-3 py-2 text-sm text-wakeve-600 font-medium hover:bg-wakeve-50 transition-default"
        >
          Connexion
        </a>
      {/if}
    </div>
  {/if}
</header>
