<script lang="ts">
  import type { WizardSlot } from '$lib/utils/slot'
  import type { TimeOfDay } from '$lib/types/api'
  import Select from '$lib/components/atoms/Select.svelte'
  import Input from '$lib/components/atoms/Input.svelte'

  interface Props {
    slot: WizardSlot
    index: number
    onupdate: (slot: WizardSlot) => void
    onremove: () => void
  }

  const { slot, index, onupdate, onremove }: Props = $props()

  const timeOfDayOptions: { value: TimeOfDay; label: string }[] = [
    { value: 'ALL_DAY', label: 'Toute la journée' },
    { value: 'MORNING', label: 'Matin' },
    { value: 'AFTERNOON', label: 'Après-midi' },
    { value: 'EVENING', label: 'Soirée' },
    { value: 'SPECIFIC', label: 'Horaire précis' }
  ]

  function update(patch: Partial<WizardSlot>) {
    onupdate({ ...slot, ...patch })
  }

  function handleTimeOfDay(e: Event & { currentTarget: HTMLSelectElement }) {
    update({ timeOfDay: e.currentTarget.value as TimeOfDay })
  }

  function handleDate(e: Event & { currentTarget: HTMLInputElement }) {
    update({ date: e.currentTarget.value })
  }

  function handleStartTime(e: Event & { currentTarget: HTMLInputElement }) {
    update({ startTime: e.currentTarget.value })
  }

  function handleEndTime(e: Event & { currentTarget: HTMLInputElement }) {
    update({ endTime: e.currentTarget.value })
  }

  const isSpecific = $derived(slot.timeOfDay === 'SPECIFIC')
</script>

<div class="rounded-card border border-border bg-surface p-4 flex flex-col gap-3">
  <!-- Header -->
  <div class="flex items-center justify-between">
    <span class="text-sm font-medium text-gray-700">Créneau {index + 1}</span>
    <button
      type="button"
      onclick={onremove}
      class="text-xs text-red-500 hover:text-red-700 transition-default focus-visible:outline-red-500 rounded"
      aria-label="Supprimer le créneau {index + 1}"
    >
      Supprimer
    </button>
  </div>

  <!-- Time of day -->
  <Select
    id="tod-{slot.id}"
    label="Moment de la journée"
    value={slot.timeOfDay}
    options={timeOfDayOptions}
    onchange={handleTimeOfDay}
  />

  <!-- Date (always shown) -->
  <Input
    id="date-{slot.id}"
    type="date"
    label="Date *"
    value={slot.date}
    oninput={handleDate}
  />

  <!-- Start / End time only for SPECIFIC -->
  {#if isSpecific}
    <div class="grid grid-cols-2 gap-3">
      <Input
        id="start-{slot.id}"
        type="time"
        label="Heure de début"
        value={slot.startTime}
        oninput={handleStartTime}
      />
      <Input
        id="end-{slot.id}"
        type="time"
        label="Heure de fin"
        value={slot.endTime}
        oninput={handleEndTime}
      />
    </div>
  {/if}

  <!-- Validation errors -->
  {#if slot.errors.length > 0}
    <ul class="flex flex-col gap-0.5" role="alert" aria-label="Erreurs du créneau {index + 1}">
      {#each slot.errors as err (err)}
        <li class="text-xs text-red-600">{err}</li>
      {/each}
    </ul>
  {/if}
</div>
