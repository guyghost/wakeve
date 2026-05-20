<script lang="ts">
  import { page } from '$app/stores'

  const currentPath = $derived($page.url.pathname)

  function isActive(href: string): boolean {
    if (href === '/') return currentPath === '/'
    return currentPath.startsWith(href)
  }
</script>

<nav
  class="fixed bottom-0 inset-x-0 bg-white border-t border-border z-40 sm:hidden"
  aria-label="Navigation principale"
  style="padding-bottom: env(safe-area-inset-bottom, 0px)"
>
  <div class="flex items-center justify-around h-14">

    <!-- Events -->
    <a
      href="/"
      class="flex flex-col items-center gap-0.5 px-3 py-1.5 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded-btn
        {isActive('/') ? 'text-wakeve-600' : 'text-gray-500 hover:text-gray-900'}"
      aria-current={isActive('/') ? 'page' : undefined}
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
        class="h-5 w-5" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round"
          d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5" />
      </svg>
      <span class="text-xs leading-none font-medium">Événements</span>
    </a>

    <!-- Explore -->
    <a
      href="/explore"
      class="flex flex-col items-center gap-0.5 px-3 py-1.5 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded-btn
        {isActive('/explore') ? 'text-wakeve-600' : 'text-gray-500 hover:text-gray-900'}"
      aria-current={isActive('/explore') ? 'page' : undefined}
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
        class="h-5 w-5" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round"
          d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
      </svg>
      <span class="text-xs leading-none font-medium">Explorer</span>
    </a>

    <!-- FAB: Create (center, elevated pill) -->
    <a
      href="/create"
      class="relative -top-4 flex h-12 w-12 items-center justify-center rounded-full
        bg-wakeve-600 text-white shadow-lg shadow-wakeve-600/30
        hover:bg-wakeve-700 active:scale-95 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
      aria-label="Créer un événement"
    >
      <svg viewBox="0 0 20 20" fill="currentColor" class="h-6 w-6" aria-hidden="true">
        <path d="M10.75 4.75a.75.75 0 0 0-1.5 0v4.5h-4.5a.75.75 0 0 0 0 1.5h4.5v4.5a.75.75 0 0 0 1.5 0v-4.5h4.5a.75.75 0 0 0 0-1.5h-4.5v-4.5Z" />
      </svg>
    </a>

    <!-- Dashboard -->
    <a
      href="/dashboard"
      class="flex flex-col items-center gap-0.5 px-3 py-1.5 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded-btn
        {isActive('/dashboard') ? 'text-wakeve-600' : 'text-gray-500 hover:text-gray-900'}"
      aria-current={isActive('/dashboard') ? 'page' : undefined}
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
        class="h-5 w-5" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round"
          d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" />
      </svg>
      <span class="text-xs leading-none font-medium">Tableau de bord</span>
    </a>

    <!-- Profile -->
    <a
      href="/profile"
      class="flex flex-col items-center gap-0.5 px-3 py-1.5 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded-btn
        {isActive('/profile') ? 'text-wakeve-600' : 'text-gray-500 hover:text-gray-900'}"
      aria-current={isActive('/profile') ? 'page' : undefined}
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
        class="h-5 w-5" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round"
          d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" />
      </svg>
      <span class="text-xs leading-none font-medium">Profil</span>
    </a>

  </div>
</nav>
