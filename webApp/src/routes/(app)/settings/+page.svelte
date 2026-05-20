<script lang="ts">
  import { goto } from '$app/navigation'
  import { useAuth } from '$lib/actors/auth.actor.svelte'
  import Modal from '$lib/components/ui/Modal.svelte'
  import Button from '$lib/components/atoms/Button.svelte'
  import { setLocale, getLocale, SUPPORTED_LOCALES } from '$lib/i18n'

  const { snapshot, actor } = useAuth()
  const user = $derived(snapshot.context.user)
  const currentLocale = $derived(getLocale())

  // ── Notification toggles (UI only) ─────────────────────────────────────────
  let notifEvents = $state(true)
  let notifVotes = $state(true)
  let notifComments = $state(false)
  let notifReminders = $state(true)

  // ── Notification toggles definition (derived) ────────────────────────────────
  const toggles = $derived<{ label: string; description: string; value: boolean; key: string }[]>([
    { label: 'Nouveaux événements', description: 'Quand vous êtes invité à un événement', value: notifEvents, key: 'events' },
    { label: 'Votes reçus', description: 'Quand un participant vote sur vos créneaux', value: notifVotes, key: 'votes' },
    { label: 'Commentaires', description: 'Quand quelqu\'un commente votre événement', value: notifComments, key: 'comments' },
    { label: 'Rappels', description: 'Rappels avant la date limite de vote', value: notifReminders, key: 'reminders' }
  ])

  // ── Delete account modal ────────────────────────────────────────────────────
  let deleteModalOpen = $state(false)
  let deleteConfirmText = $state('')

  const canConfirmDelete = $derived(deleteConfirmText.trim() === 'SUPPRIMER')

  function handleLogout() {
    actor.send({ type: 'LOGOUT' })
    goto('/login')
  }

  function handleDeleteAccount() {
    // In a real app this would call a DELETE /api/auth/account endpoint
    actor.send({ type: 'LOGOUT' })
    goto('/login')
  }
</script>

<div class="max-w-2xl mx-auto flex flex-col gap-6">
  <h1 class="text-2xl font-bold text-gray-900">Paramètres</h1>

  <!-- ── Language & Region ──────────────────────────────────────────────────── -->
  <section class="bg-white rounded-card shadow-card p-5 flex flex-col gap-4">
    <div class="flex flex-col gap-0.5">
      <h2 class="text-base font-semibold text-gray-800">Langue et région</h2>
      <p class="text-xs text-gray-500">Choisissez la langue d'affichage de l'interface.</p>
    </div>

    <div class="flex gap-2" role="group" aria-label="Choisir la langue">
      {#each SUPPORTED_LOCALES as locale (locale)}
        <button
          type="button"
          onclick={() => setLocale(locale)}
          class="flex items-center gap-2 rounded-btn border px-4 py-2.5 text-sm font-medium transition-default
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
            {currentLocale === locale
              ? 'border-wakeve-600 bg-wakeve-50 text-wakeve-700 shadow-sm'
              : 'border-border bg-white text-gray-600 hover:bg-gray-50'}"
          aria-pressed={currentLocale === locale}
        >
          <span aria-hidden="true">{locale === 'fr' ? '🇫🇷' : '🇬🇧'}</span>
          <span>{locale === 'fr' ? 'Français' : 'English'}</span>
          {#if currentLocale === locale}
            <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4 text-wakeve-600" aria-hidden="true">
              <path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z" clip-rule="evenodd" />
            </svg>
          {/if}
        </button>
      {/each}
    </div>
  </section>

  <!-- ── Notifications ───────────────────────────────────────────────────────── -->
  <section class="bg-white rounded-card shadow-card p-5 flex flex-col gap-4">
    <div class="flex flex-col gap-0.5">
      <h2 class="text-base font-semibold text-gray-800">Notifications</h2>
      <p class="text-xs text-gray-500">Gérez les notifications que vous souhaitez recevoir.</p>
    </div>

    <ul class="flex flex-col divide-y divide-gray-100" role="list">
      {#each toggles as toggle (toggle.key)}
        <li class="flex items-center justify-between gap-4 py-3.5">
          <div class="flex flex-col gap-0.5">
            <span class="text-sm font-medium text-gray-800">{toggle.label}</span>
            <span class="text-xs text-gray-500">{toggle.description}</span>
          </div>
          <!-- Toggle switch -->
          <button
            type="button"
            role="switch"
            aria-checked={toggle.value}
            aria-label={toggle.label}
            onclick={() => {
              if (toggle.key === 'events') notifEvents = !notifEvents
              else if (toggle.key === 'votes') notifVotes = !notifVotes
              else if (toggle.key === 'comments') notifComments = !notifComments
              else if (toggle.key === 'reminders') notifReminders = !notifReminders
            }}
            class="relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-default
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
              {toggle.value ? 'bg-wakeve-600' : 'bg-gray-200'}"
          >
            <span
              class="pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow ring-0 transition-transform duration-200
                {toggle.value ? 'translate-x-5' : 'translate-x-0'}"
            ></span>
          </button>
        </li>
      {/each}
    </ul>
  </section>

  <!-- ── Account ─────────────────────────────────────────────────────────────── -->
  {#if user}
    <section class="bg-white rounded-card shadow-card p-5 flex flex-col gap-4">
      <h2 class="text-base font-semibold text-gray-800">Compte</h2>

      <dl class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
        <div class="flex flex-col gap-0.5">
          <dt class="text-xs font-medium text-gray-400 uppercase tracking-wide">Nom d'affichage</dt>
          <dd class="text-gray-800">{user.displayName}</dd>
        </div>
        {#if user.email}
          <div class="flex flex-col gap-0.5">
            <dt class="text-xs font-medium text-gray-400 uppercase tracking-wide">Email</dt>
            <dd class="text-gray-800 truncate">{user.email}</dd>
          </div>
        {/if}
        <div class="flex flex-col gap-0.5">
          <dt class="text-xs font-medium text-gray-400 uppercase tracking-wide">Type</dt>
          <dd class="text-gray-800">
            {user.accountType === 'REGISTERED' ? 'Compte complet' : 'Invité'}
          </dd>
        </div>
      </dl>

      <Button variant="secondary" size="md" onclick={handleLogout}>
        Se déconnecter
      </Button>
    </section>
  {/if}

  <!-- ── Danger Zone ──────────────────────────────────────────────────────────── -->
  <section class="rounded-card border-2 border-red-200 bg-red-50 p-5 flex flex-col gap-4">
    <div class="flex flex-col gap-0.5">
      <h2 class="text-base font-semibold text-red-800">Zone dangereuse</h2>
      <p class="text-xs text-red-600">Ces actions sont irréversibles. Procédez avec précaution.</p>
    </div>

    <div class="flex items-center justify-between gap-4 flex-wrap">
      <div class="flex flex-col gap-0.5">
        <p class="text-sm font-medium text-red-800">Supprimer mon compte</p>
        <p class="text-xs text-red-600">Toutes vos données seront définitivement supprimées.</p>
      </div>
      <Button
        variant="danger"
        size="sm"
        onclick={() => { deleteModalOpen = true; deleteConfirmText = '' }}
      >
        Supprimer mon compte
      </Button>
    </div>
  </section>
</div>

<!-- ── Delete confirmation modal ─────────────────────────────────────────────── -->
<Modal
  open={deleteModalOpen}
  title="Supprimer mon compte"
  onclose={() => { deleteModalOpen = false; deleteConfirmText = '' }}
>
  {#snippet children()}
    <div class="flex flex-col gap-4">
      <div
        role="alert"
        class="flex items-start gap-3 rounded-xl bg-red-50 border border-red-200 px-4 py-3"
      >
        <svg viewBox="0 0 20 20" fill="currentColor" class="h-5 w-5 text-red-500 shrink-0 mt-0.5" aria-hidden="true">
          <path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495ZM10 5a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 10 5Zm0 9a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z" clip-rule="evenodd" />
        </svg>
        <div class="text-sm text-red-700">
          <p class="font-semibold mb-1">Cette action est irréversible.</p>
          <p>Tous vos événements, votes et commentaires seront définitivement supprimés.</p>
        </div>
      </div>

      <div class="flex flex-col gap-1.5">
        <label for="delete-confirm" class="text-sm font-medium text-gray-700">
          Tapez <span class="font-mono font-bold text-red-600">SUPPRIMER</span> pour confirmer
        </label>
        <input
          id="delete-confirm"
          type="text"
          bind:value={deleteConfirmText}
          placeholder="SUPPRIMER"
          class="w-full rounded-btn border border-border px-3 py-2 text-sm text-gray-900 placeholder-gray-300
            focus-visible:outline-red-500 transition-default"
          autocomplete="off"
          autocorrect="off"
          spellcheck={false}
        />
      </div>
    </div>
  {/snippet}

  {#snippet footer()}
    <div class="flex items-center justify-end gap-3">
      <Button
        variant="ghost"
        size="md"
        onclick={() => { deleteModalOpen = false; deleteConfirmText = '' }}
      >
        Annuler
      </Button>
      <Button
        variant="danger"
        size="md"
        disabled={!canConfirmDelete}
        onclick={handleDeleteAccount}
      >
        Supprimer définitivement
      </Button>
    </div>
  {/snippet}
</Modal>
