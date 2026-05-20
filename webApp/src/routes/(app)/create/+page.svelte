<script lang="ts">
  import { createActor } from 'xstate'
  import { goto } from '$app/navigation'
  import { onDestroy } from 'svelte'
  import { eventWizardMachine } from '$lib/machines/eventWizard.machine'
  import CreateWizard from '$lib/components/organisms/CreateWizard.svelte'

  const actor = createActor(eventWizardMachine)
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => { sub.unsubscribe(); actor.stop() })

  $effect(() => {
    if (snapshot.value === 'success' && snapshot.context.createdEventId) {
      goto(`/events/${snapshot.context.createdEventId}`)
    }
  })
</script>

<div class="max-w-2xl mx-auto">
  <div class="flex items-center gap-3 mb-6">
    <a
      href="/"
      class="rounded-btn p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
      aria-label="Retour aux événements"
    >
      <svg viewBox="0 0 20 20" fill="currentColor" class="h-5 w-5" aria-hidden="true">
        <path fill-rule="evenodd" d="M17 10a.75.75 0 0 1-.75.75H5.612l4.158 3.96a.75.75 0 1 1-1.04 1.08l-5.5-5.25a.75.75 0 0 1 0-1.08l5.5-5.25a.75.75 0 1 1 1.04 1.08L5.612 9.25H16.25A.75.75 0 0 1 17 10Z" clip-rule="evenodd" />
      </svg>
    </a>
    <h1 class="text-2xl font-bold text-gray-900">Créer un événement</h1>
  </div>

  <div class="bg-white rounded-card shadow-card p-6">
    <CreateWizard {actor} />
  </div>
</div>
