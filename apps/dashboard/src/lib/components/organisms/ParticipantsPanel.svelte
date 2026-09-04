<script lang="ts">
  import type {
    EventResponse,
    ParticipantRsvpResponse,
    RsvpAttendance,
    TimeSlotResponse
  } from '$lib/types/api'
  import * as participantsApi from '$lib/api/participants.api'
  import { t } from '$lib/i18n'
  import Avatar from '$lib/components/atoms/Avatar.svelte'
  import Button from '$lib/components/atoms/Button.svelte'
  import Input from '$lib/components/atoms/Input.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import { formatSlotLabel } from '$lib/utils/slot'

  interface Props {
    event: EventResponse
    currentUserId: string
  }

  const { event, currentUserId }: Props = $props()

  const isOrganizer = $derived(currentUserId !== '' && currentUserId === event.organizerId)

  // ── Participant list state ───────────────────────────────────────────────
  let participants = $state<string[]>([])
  let loading = $state(true)
  let loadError = $state<string | null>(null)

  async function load() {
    loading = true
    loadError = null
    try {
      const res = await participantsApi.list(event.id)
      participants = [...res.participants].sort((a, b) =>
        a.localeCompare(b)
      )
    } catch {
      loadError = t('participants.errors.load')
    } finally {
      loading = false
    }
  }

  load()

  // ── Add participant (organizer only, by user ID) ─────────────────────────
  let newParticipantId = $state('')
  let adding = $state(false)
  let addError = $state<string | null>(null)

  async function handleAdd(e: SubmitEvent) {
    e.preventDefault()
    const participantId = newParticipantId.trim()
    if (!participantId) return
    adding = true
    addError = null
    try {
      const res = await participantsApi.add(event.id, {
        eventId: event.id,
        participantId
      })
      participants = [...res.participants].sort((a, b) => a.localeCompare(b))
      newParticipantId = ''
    } catch {
      addError = t('participants.errors.add')
    } finally {
      adding = false
    }
  }

  // ── RSVP (requires a confirmed retained date) ────────────────────────────
  // The backend requires the slotId of the retained date. Best-effort match
  // against the event's proposed slots using finalDate.
  const retainedSlot = $derived.by(() => {
    if (!event.finalDate) return event.proposedSlots[0] ?? null
    const finalDay = event.finalDate.slice(0, 10)
    return (
      event.proposedSlots.find(
        (s: TimeSlotResponse) => (s.startTime ?? '').slice(0, 10) === finalDay
      ) ??
      event.proposedSlots[0] ??
      null
    )
  })
  let rsvpSlotId = $state('')
  // Effective slot used for RSVP: user override, else the retained slot guess.
  const effectiveSlotId = $derived(rsvpSlotId || retainedSlot?.id || '')

  const rsvpSlotOptions = $derived(
    event.proposedSlots.map((s: TimeSlotResponse) => ({
      value: s.id,
      label: formatSlotLabel(s)
    }))
  )

  let attendanceByUser = $state<Record<string, RsvpAttendance>>({})
  let rsvpResults = $state<Record<string, ParticipantRsvpResponse>>({})
  let rsvpBusyUserId = $state<string | null>(null)
  let rsvpError = $state<string | null>(null)

  function attendanceFor(userId: string): RsvpAttendance {
    return attendanceByUser[userId] ?? 'CONFIRMED'
  }

  function handleAttendanceChange(userId: string, attendance: RsvpAttendance) {
    attendanceByUser = { ...attendanceByUser, [userId]: attendance }
  }

  function canRespond(userId: string): boolean {
    return isOrganizer || userId === currentUserId
  }

  const canRespondHere = $derived(
    event.finalDate !== null &&
      event.finalDate !== undefined &&
      (isOrganizer || participants.includes(currentUserId))
  )

  async function applyRsvp(userId: string) {
    const slotId = effectiveSlotId
    if (!slotId) return
    rsvpBusyUserId = userId
    rsvpError = null
    try {
      const res = await participantsApi.rsvp(event.id, userId, {
        slotId,
        attendance: attendanceFor(userId)
      })
      rsvpResults = { ...rsvpResults, [userId]: res }
    } catch {
      rsvpError = t('participants.errors.rsvp')
    } finally {
      rsvpBusyUserId = null
    }
  }

  // ── Display helpers ──────────────────────────────────────────────────────
  function labelFor(userId: string): string {
    return userId === currentUserId ? t('participants.you') : userId
  }

  function rsvpStateFor(userId: string): string | null {
    const res = rsvpResults[userId]
    return res ? t(`participants.rsvpState.${res.rsvpState}`) : null
  }

  function rsvpStateClass(userId: string): string {
    const state = rsvpResults[userId]?.rsvpState
    if (state === 'ACCEPTED') return 'bg-green-50 text-green-700'
    if (state === 'DECLINED') return 'bg-red-50 text-red-700'
    if (state === 'PENDING') return 'bg-amber-50 text-amber-700'
    return 'bg-gray-100 text-gray-600'
  }
</script>

<div class="flex flex-col gap-6">
  <!-- Loading skeleton -->
  {#if loading}
    <div class="flex flex-col gap-3">
      {#each { length: 3 } as _, i (i)}
        <SkeletonBlock height="h-14" rounded="rounded-card" />
      {/each}
    </div>

  <!-- Error -->
  {:else if loadError}
    <ErrorBanner message={loadError} onretry={load} />

  {:else}
    <!-- Add participant form (organizer only) -->
    {#if isOrganizer}
      <form
        onsubmit={handleAdd}
        class="flex flex-col gap-3 rounded-card border border-border bg-surface p-4"
      >
        <h3 class="text-sm font-semibold text-gray-800">{t('participants.addTitle')}</h3>
        <div class="flex items-end gap-3">
          <div class="flex-1">
            <Input
              id="participant-id"
              label={t('participants.addLabel')}
              placeholder={t('participants.addPlaceholder')}
              value={newParticipantId}
              disabled={adding}
              oninput={(e) => (newParticipantId = e.currentTarget.value)}
            />
          </div>
          <Button type="submit" disabled={!newParticipantId.trim()} loading={adding}>
            {t('participants.addSubmit')}
          </Button>
        </div>
        <p class="text-xs text-gray-500">{t('participants.addHelp')}</p>
        {#if addError}
          <ErrorBanner message={addError} />
        {/if}
      </form>
    {/if}

    <!-- Participant list -->
    <section class="flex flex-col gap-3" aria-label={t('participants.listTitle')}>
      <h3 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
        {t('participants.listTitle')}
        <span class="ml-1.5 text-xs font-normal text-gray-400 normal-case">
          ({participants.length})
        </span>
      </h3>

      {#if participants.length === 0}
        <div class="flex flex-col items-center justify-center gap-2 py-10 text-center">
          <span class="text-3xl" aria-hidden="true">👥</span>
          <p class="text-sm text-gray-500">{t('participants.empty')}</p>
        </div>
      {:else}
        <ul class="flex flex-col gap-2" role="list">
          {#each participants as userId (userId)}
            {@const rsvpState = rsvpStateFor(userId)}
            <li class="rounded-card border border-border bg-surface p-3">
              <div class="flex items-center gap-3">
                <Avatar name={userId} size="sm" />
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium text-gray-800">
                    {labelFor(userId)}
                  </p>
                  <div class="mt-0.5 flex flex-wrap items-center gap-1.5">
                    {#if userId === event.organizerId}
                      <span
                        class="rounded-full bg-wakeve-50 px-2 py-0.5 text-xs font-medium text-wakeve-700"
                      >
                        {t('participants.organizer')}
                      </span>
                    {/if}
                    {#if rsvpState}
                      <span class="rounded-full px-2 py-0.5 text-xs font-medium {rsvpStateClass(userId)}">
                        {rsvpState}
                      </span>
                    {/if}
                  </div>
                </div>
              </div>

              <!-- RSVP controls -->
              {#if event.finalDate && canRespond(userId)}
                <div class="mt-3 flex flex-col gap-2 border-t border-gray-100 pt-3 sm:flex-row sm:items-end">
                  <div class="w-full sm:w-40">
                    <Select
                      id="attendance-{userId}"
                      label={t('participants.rsvpAttendance')}
                      value={attendanceFor(userId)}
                      options={[
                        { value: 'CONFIRMED', label: t('participants.attendance.CONFIRMED') },
                        { value: 'TENTATIVE', label: t('participants.attendance.TENTATIVE') },
                        { value: 'DECLINED', label: t('participants.attendance.DECLINED') }
                      ]}
                      onchange={(e) =>
                        handleAttendanceChange(
                          userId,
                          e.currentTarget.value as RsvpAttendance
                        )}
                    />
                  </div>
                  <Button
                    size="sm"
                    loading={rsvpBusyUserId === userId}
                    disabled={!effectiveSlotId}
                    onclick={() => applyRsvp(userId)}
                  >
                    {t('participants.rsvpApply')}
                  </Button>
                </div>
              {/if}
            </li>
          {/each}
        </ul>
      {/if}
    </section>

    <!-- RSVP context: retained slot selector -->
    {#if canRespondHere}
      <section class="flex flex-col gap-3 rounded-card border border-border bg-surface p-4">
        <h3 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          {t('participants.rsvpTitle')}
        </h3>
        {#if rsvpError}
          <ErrorBanner message={rsvpError} />
        {/if}
        <div class="w-full sm:w-64">
          <Select
            id="rsvp-slot"
            label={t('participants.rsvpSlot')}
            value={effectiveSlotId}
            options={rsvpSlotOptions}
            onchange={(e) => (rsvpSlotId = e.currentTarget.value)}
          />
        </div>
      </section>
    {:else if !event.finalDate}
      <p class="text-xs text-gray-500">{t('participants.rsvpLocked')}</p>
    {/if}
  {/if}
</div>
