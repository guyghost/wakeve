<script lang="ts">
  import { goto } from '$app/navigation'
  import { useAuth } from '$lib/actors/auth.actor.svelte'
  import Avatar from '$lib/components/atoms/Avatar.svelte'
  import Button from '$lib/components/atoms/Button.svelte'
  import { setLocale, getLocale, SUPPORTED_LOCALES } from '$lib/i18n'

  const { snapshot, actor } = useAuth()

  const user = $derived(snapshot.context.user)
  const currentLocale = $derived(getLocale())

  function handleLogout() {
    actor.send({ type: 'LOGOUT' })
    goto('/login')
  }

  const authMethodLabels: Record<string, string> = {
    EMAIL_OTP: 'Email OTP',
    GOOGLE: 'Google',
    APPLE: 'Apple',
    GUEST: 'Invité'
  }

  const accountTypeLabels: Record<string, string> = {
    GUEST: 'Invité',
    REGISTERED: 'Compte complet'
  }
</script>

<div class="max-w-2xl mx-auto flex flex-col gap-6">
  <h1 class="text-2xl font-bold text-gray-900">Mon profil</h1>

  {#if user}
    <!-- Identity card -->
    <div class="bg-white rounded-card shadow-card p-6 flex flex-col gap-6">
      <!-- Avatar + name row -->
      <div class="flex items-center gap-4">
        <Avatar name={user.displayName} size="lg" />
        <div class="flex flex-col gap-0.5">
          <p class="text-xl font-bold text-gray-900">{user.displayName}</p>
          {#if user.email}
            <p class="text-sm text-gray-500">{user.email}</p>
          {/if}
        </div>
      </div>

      <hr class="border-border" />

      <!-- Details -->
      <dl class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
        <div class="flex flex-col gap-0.5">
          <dt class="text-xs font-medium text-gray-400 uppercase tracking-wide">Identifiant</dt>
          <dd class="font-mono text-xs text-gray-600 bg-gray-50 rounded px-2 py-1 truncate">
            {user.id}
          </dd>
        </div>

        <div class="flex flex-col gap-0.5">
          <dt class="text-xs font-medium text-gray-400 uppercase tracking-wide">Type de compte</dt>
          <dd class="flex items-center gap-1.5">
            <span
              class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium
                {user.accountType === 'REGISTERED'
                  ? 'bg-green-100 text-green-700'
                  : 'bg-gray-100 text-gray-600'}"
            >
              {accountTypeLabels[user.accountType] ?? user.accountType}
            </span>
          </dd>
        </div>

        <div class="flex flex-col gap-0.5">
          <dt class="text-xs font-medium text-gray-400 uppercase tracking-wide">Méthode de connexion</dt>
          <dd class="text-gray-700">{authMethodLabels[user.authMethod] ?? user.authMethod}</dd>
        </div>

        {#if user.email}
          <div class="flex flex-col gap-0.5">
            <dt class="text-xs font-medium text-gray-400 uppercase tracking-wide">Email</dt>
            <dd class="text-gray-700 truncate">{user.email}</dd>
          </div>
        {/if}
      </dl>
    </div>

    <!-- Language -->
    <div class="bg-white rounded-card shadow-card p-5 flex flex-col gap-3">
      <h2 class="text-sm font-semibold text-gray-800">Langue</h2>
      <div class="flex gap-2" role="group" aria-label="Choisir la langue">
        {#each SUPPORTED_LOCALES as locale (locale)}
          <button
            type="button"
            onclick={() => setLocale(locale)}
            class="rounded-btn border px-4 py-2 text-sm font-medium transition-default
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
              {currentLocale === locale
                ? 'border-wakeve-600 bg-wakeve-50 text-wakeve-700'
                : 'border-border bg-white text-gray-600 hover:bg-gray-50'}"
            aria-pressed={currentLocale === locale}
          >
            {locale === 'fr' ? '🇫🇷 Français' : '🇬🇧 English'}
          </button>
        {/each}
      </div>
    </div>

    <!-- Actions -->
    <div class="bg-white rounded-card shadow-card p-5 flex flex-col gap-3">
      <h2 class="text-sm font-semibold text-gray-800">Actions</h2>
      <div class="flex flex-col sm:flex-row gap-3">
        <a
          href="/dashboard"
          class="inline-flex items-center justify-center gap-2 rounded-btn border border-border bg-white px-4 py-2 text-sm font-medium text-gray-700
            hover:bg-gray-50 transition-default
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
        >
          📊 Tableau de bord
        </a>
        <Button variant="danger" size="md" onclick={handleLogout}>
          Se déconnecter
        </Button>
      </div>
    </div>

  {:else}
    <div class="flex flex-col items-center justify-center py-12 gap-4">
      <span class="text-5xl" aria-hidden="true">👤</span>
      <p class="text-sm text-gray-500">Aucun utilisateur connecté.</p>
      <a
        href="/login"
        class="rounded-btn bg-wakeve-600 px-4 py-2 text-sm font-medium text-white hover:bg-wakeve-700 transition-default
          focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600"
      >
        Se connecter
      </a>
    </div>
  {/if}
</div>
