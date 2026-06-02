<script lang="ts">
  import type { Actor, SnapshotFrom } from 'xstate'
  import { eventWizardMachine } from '$lib/machines/eventWizard.machine'
  import type { EventType, TimeOfDay } from '$lib/types/api'
  import type { WizardFields, WizardSlot } from '$lib/machines/eventWizard.machine'
  import StepIndicator from '$lib/components/molecules/StepIndicator.svelte'
  import TimeSlotEditor from '$lib/components/molecules/TimeSlotEditor.svelte'
  import Input from '$lib/components/atoms/Input.svelte'
  import Textarea from '$lib/components/atoms/Textarea.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import Button from '$lib/components/atoms/Button.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'

  type WizardActor = Actor<typeof eventWizardMachine>
  type WizardSnapshot = SnapshotFrom<typeof eventWizardMachine>

  interface Props {
    actor: WizardActor
  }

  const { actor }: Props = $props()

  let snapshot = $state<WizardSnapshot | undefined>()
  $effect(() => {
    snapshot = actor.getSnapshot()
    const sub = actor.subscribe((s) => { snapshot = s })
    return () => sub.unsubscribe()
  })

  const activeSnapshot = $derived(snapshot ?? actor.getSnapshot())
  const ctx = $derived(activeSnapshot.context)
  const stateValue = $derived(activeSnapshot.value as string)
  const isSubmitting = $derived(stateValue === 'submitting')

  const STEPS = ['Informations', 'Créneaux', 'Lieu', 'Participants']
  const stepMap: Record<string, number> = {
    step1: 1, step2: 2, step3: 3, step4: 4, submitting: 4, success: 4
  }
  const currentStep = $derived(stepMap[stateValue] ?? 1)

  // ── Event type options ────────────────────────────────────────────────────
  const eventTypeOptions: { value: EventType; label: string }[] = [
    { value: 'BIRTHDAY', label: '🎂 Anniversaire' },
    { value: 'WEDDING', label: '💍 Mariage' },
    { value: 'CORPORATE', label: '🏢 Corporate' },
    { value: 'TEAM_BUILDING', label: '🤝 Team Building' },
    { value: 'CONCERT', label: '🎵 Concert' },
    { value: 'SPORTS_EVENT', label: '⚽ Sport' },
    { value: 'FAMILY_REUNION', label: '👨‍👩‍👧‍👦 Réunion famille' },
    { value: 'GRADUATION', label: '🎓 Remise de diplômes' },
    { value: 'HOLIDAY_PARTY', label: '🎉 Fête de fin d\'année' },
    { value: 'NETWORKING', label: '🌐 Networking' },
    { value: 'CONFERENCE', label: '🎤 Conférence' },
    { value: 'WORKSHOP', label: '🔧 Atelier' },
    { value: 'DINNER_PARTY', label: '🍽️ Dîner' },
    { value: 'OUTDOOR_ADVENTURE', label: '🏔️ Aventure outdoor' },
    { value: 'CULTURAL_EVENT', label: '🎭 Événement culturel' },
    { value: 'OTHER', label: '📅 Autre' }
  ]

  // ── Helpers ───────────────────────────────────────────────────────────────
  function send(event: Parameters<WizardActor['send']>[0]) {
    actor.send(event)
  }

  function updateField(field: keyof WizardFields, value: string) {
    send({ type: 'UPDATE_FIELD', field, value })
  }

  // ── Step 4: invite email local state ─────────────────────────────────────
  let inviteEmail = $state('')

  function addInvite() {
    const e = inviteEmail.trim().toLowerCase()
    if (!e || !e.includes('@')) return
    send({ type: 'ADD_INVITE', email: e })
    inviteEmail = ''
  }

  function handleInviteKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter') { e.preventDefault(); addInvite() }
  }
</script>

<div class="flex flex-col gap-6 max-w-2xl mx-auto">
  <!-- Step indicator -->
  <StepIndicator steps={STEPS} {currentStep} />

  <!-- Error banner -->
  {#if ctx.formError}
    <ErrorBanner message={ctx.formError} />
  {/if}

  <!-- ── Step 1: Basic info ────────────────────────────────────────────────── -->
  {#if stateValue === 'step1'}
    <section class="flex flex-col gap-4">
      <h2 class="text-base font-semibold text-gray-800">Informations générales</h2>

      <Input
        id="wizard-title"
        label="Titre de l'événement *"
        placeholder="Ex : Soirée d'anniversaire de Marie"
        value={ctx.fields.title}
        required
        oninput={(e) => updateField('title', e.currentTarget.value)}
      />

      <Textarea
        id="wizard-description"
        label="Description"
        placeholder="Décrivez votre événement…"
        value={ctx.fields.description}
        rows={3}
        oninput={(e) => updateField('description', e.currentTarget.value)}
      />

      <Select
        id="wizard-type"
        label="Type d'événement"
        value={ctx.fields.type}
        options={eventTypeOptions}
        onchange={(e) => updateField('type', e.currentTarget.value)}
      />

      <Input
        id="wizard-participants"
        type="number"
        label="Nombre de participants prévu"
        placeholder="Ex : 12"
        value={ctx.fields.expectedParticipants}
        oninput={(e) => updateField('expectedParticipants', e.currentTarget.value)}
      />

      <Input
        id="wizard-deadline"
        type="date"
        label="Date limite de réponse *"
        value={ctx.fields.deadline}
        required
        oninput={(e) => updateField('deadline', e.currentTarget.value)}
      />
    </section>

  <!-- ── Step 2: Slots ─────────────────────────────────────────────────────── -->
  {:else if stateValue === 'step2'}
    <section class="flex flex-col gap-4">
      <div class="flex flex-col gap-0.5">
        <h2 class="text-base font-semibold text-gray-800">Créneaux proposés</h2>
        <p class="text-xs text-gray-500">Proposez plusieurs créneaux pour que les participants votent.</p>
      </div>

      {#each ctx.slots as slot, i (slot.id)}
        <TimeSlotEditor
          {slot}
          index={i}
          onupdate={(updated) => send({ type: 'UPDATE_SLOT', id: updated.id, field: 'timeOfDay', value: updated.timeOfDay })}
          onremove={() => send({ type: 'REMOVE_SLOT', id: slot.id })}
        />
      {/each}

      {#if ctx.slots.length < 10}
        <button
          type="button"
          onclick={() => send({ type: 'ADD_SLOT' })}
          class="flex items-center gap-2 rounded-btn border border-dashed border-wakeve-300
            px-4 py-3 text-sm font-medium text-wakeve-600
            hover:border-wakeve-500 hover:bg-wakeve-50 transition-default
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
        >
          <span aria-hidden="true">+</span>
          Ajouter un créneau
        </button>
      {/if}
    </section>

  <!-- ── Step 3: Location ──────────────────────────────────────────────────── -->
  {:else if stateValue === 'step3'}
    <section class="flex flex-col items-center justify-center gap-4 py-12 text-center">
      <span class="text-5xl" aria-hidden="true">🗺️</span>
      <div class="flex flex-col gap-1">
        <h2 class="text-base font-semibold text-gray-800">Lieu (bientôt disponible)</h2>
        <p class="text-sm text-gray-500 max-w-xs text-balance">
          La sélection de lieu sera disponible prochainement.
          Passez à l'étape suivante pour inviter des participants.
        </p>
      </div>
    </section>

  <!-- ── Step 4: Participants ──────────────────────────────────────────────── -->
  {:else if stateValue === 'step4' || stateValue === 'submitting'}
    <section class="flex flex-col gap-4">
      <div class="flex flex-col gap-0.5">
        <h2 class="text-base font-semibold text-gray-800">Inviter des participants</h2>
        <p class="text-xs text-gray-500">Ajoutez les emails des personnes à inviter (optionnel).</p>
      </div>

      <!-- Email input row -->
      <div class="flex gap-2">
        <Input
          id="wizard-invite-email"
          type="email"
          placeholder="email@exemple.com"
          value={inviteEmail}
          class="flex-1"
          oninput={(e) => { inviteEmail = e.currentTarget.value }}
        />
        <Button
          variant="secondary"
          size="md"
          onclick={addInvite}
          disabled={!inviteEmail.includes('@')}
        >
          Ajouter
        </Button>
      </div>

      <!-- Added emails list -->
      {#if ctx.invites.length === 0}
        <p class="text-xs text-gray-400 text-center py-4">Aucun participant ajouté</p>
      {:else}
        <ul class="flex flex-col gap-2" role="list">
          {#each ctx.invites as email (email)}
            <li class="flex items-center justify-between rounded-btn border border-border bg-white px-3 py-2">
              <span class="text-sm text-gray-800">{email}</span>
              <button
                type="button"
                onclick={() => send({ type: 'REMOVE_INVITE', email })}
                class="text-xs text-red-500 hover:text-red-700 transition-default
                  focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-500 rounded"
                aria-label="Retirer {email}"
              >
                Retirer
              </button>
            </li>
          {/each}
        </ul>
      {/if}
    </section>
  {/if}

  <!-- ── Navigation buttons ─────────────────────────────────────────────────── -->
  {#if stateValue !== 'success'}
    <div class="flex items-center justify-between pt-2 border-t border-border">
      <div>
        {#if stateValue === 'step2' || stateValue === 'step3' || stateValue === 'step4' || stateValue === 'submitting'}
          <Button
            variant="ghost"
            size="md"
            disabled={isSubmitting}
            onclick={() => send({ type: 'PREV' })}
          >
            ← Précédent
          </Button>
        {/if}
      </div>

      <div>
        {#if stateValue === 'step1' || stateValue === 'step2' || stateValue === 'step3'}
          <Button
            variant="primary"
            size="md"
            onclick={() => send({ type: 'NEXT' })}
          >
            Suivant →
          </Button>
        {:else if stateValue === 'step4' || stateValue === 'submitting'}
          <Button
            variant="primary"
            size="md"
            loading={isSubmitting}
            disabled={isSubmitting}
            onclick={() => send({ type: 'SUBMIT' })}
          >
            Créer l'événement
          </Button>
        {/if}
      </div>
    </div>
  {/if}
</div>
