<script lang="ts">
  import { createActor } from 'xstate'
  import { onDestroy } from 'svelte'
  import type { EventStatus, MeetingDTO, MeetingStatus } from '$lib/types/api'
  import { meetingsMachine } from '$lib/machines/meetings.machine'
  import { t, currentLocale } from '$lib/i18n'
  import Button from '$lib/components/atoms/Button.svelte'
  import Input from '$lib/components/atoms/Input.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import Modal from '$lib/components/ui/Modal.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import StatePanel from '$lib/components/ui/StatePanel.svelte'
  import { formatDateTime, toISOLocal } from '$lib/utils/date'

  interface Props {
    eventId: string
    /** When provided, gates creation on the ORGANIZING status (consultation stays available). */
    eventStatus?: EventStatus | null
    /** Only the organizer may create/cancel meetings (enforced server-side too). */
    isOrganizer?: boolean
    /** Used as the default title when creating a meeting. */
    eventTitle?: string
  }

  const {
    eventId,
    eventStatus = null,
    isOrganizer = false,
    eventTitle = ''
  }: Props = $props()

  // ─── Meetings list machine ───────────────────────────────────────────────────

  // The panel is instantiated per event-detail page; capturing the initial
  // eventId is intentional (the route param never changes for this tab).
  // svelte-ignore state_referenced_locally
  const actor = createActor(meetingsMachine, { input: { eventId } })
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => { sub.unsubscribe(); actor.stop() })

  const meetings = $derived(snapshot.context.meetings)
  const loadError = $derived(snapshot.context.error)
  const isCreating = $derived(snapshot.context.isCreating)
  const createError = $derived(snapshot.context.createError)
  const cancelError = $derived(snapshot.context.cancelError)
  const stateValue = $derived(snapshot.value as string)

  const canCreate = $derived(isOrganizer && (eventStatus === null || eventStatus === 'ORGANIZING'))
  const isLockedByStatus = $derived(
    isOrganizer && eventStatus !== null && eventStatus !== 'ORGANIZING'
  )

  // ─── Creation form ───────────────────────────────────────────────────────────

  function todayISO(): string {
    const d = new Date()
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }

  let formTitle = $state('')
  let formDate = $state(todayISO())
  let formTime = $state('14:00')
  let formDuration = $state('60')

  const durationOptions = [
    { value: '15', label: '15 min' },
    { value: '30', label: '30 min' },
    { value: '60', label: '60 min' },
    { value: '90', label: '90 min' },
    { value: '120', label: '120 min' }
  ]

  function submitCreation(platform: 'ZOOM' | 'GOOGLE_MEET') {
    const scheduledFor = toISOLocal(formDate, formTime)
    const shared = {
      eventId,
      title: formTitle.trim() || eventTitle || t('meetings.create.defaultTitle'),
      scheduledFor,
      duration: parseInt(formDuration, 10) || 60,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
    }
    if (platform === 'ZOOM') {
      actor.send({ type: 'CREATE_ZOOM', input: shared })
    } else {
      actor.send({ type: 'CREATE_GOOGLE_MEET', input: shared })
    }
  }

  // ─── Copy link ───────────────────────────────────────────────────────────────

  let copiedId = $state<string | null>(null)
  let copyTimeout: ReturnType<typeof setTimeout> | undefined

  async function copyLink(meeting: MeetingDTO) {
    const url = meeting.meetingLink || meeting.targetUrl
    if (!url) return
    try {
      await navigator.clipboard.writeText(url)
    } catch {
      // Clipboard API unavailable (permissions/insecure context) — fallback
      const textarea = document.createElement('textarea')
      textarea.value = url
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      textarea.remove()
    }
    copiedId = meeting.id
    clearTimeout(copyTimeout)
    copyTimeout = setTimeout(() => { copiedId = null }, 2000)
  }

  // ─── Cancel Zoom (with confirmation) ─────────────────────────────────────────

  let pendingCancel = $state<MeetingDTO | null>(null)

  function requestCancel(meeting: MeetingDTO) {
    pendingCancel = meeting
  }

  function confirmCancel() {
    if (!pendingCancel) return
    actor.send({ type: 'CANCEL_ZOOM', meetingId: pendingCancel.hostMeetingId })
    pendingCancel = null
  }

  function canCancel(meeting: MeetingDTO): boolean {
    return (
      meeting.platform === 'ZOOM' &&
      (meeting.status === 'SCHEDULED' || meeting.status === 'STARTED')
    )
  }

  // ─── Display helpers ─────────────────────────────────────────────────────────

  const dateLocale = $derived(currentLocale.value === 'en' ? 'en-US' : 'fr-FR')

  const statusStyles: Record<MeetingStatus, string> = {
    SCHEDULED: 'bg-sky-100 text-sky-800',
    STARTED: 'bg-emerald-100 text-emerald-800',
    ENDED: 'bg-gray-100 text-gray-600',
    CANCELLED: 'bg-red-100 text-red-700'
  }

  const platformIcons: Record<string, string> = {
    ZOOM: '🎥',
    GOOGLE_MEET: '📹',
    FACETIME: '📱'
  }
</script>

<div class="flex flex-col gap-4">

  <!-- Loading -->
  {#if stateValue === 'loading'}
    <div class="flex flex-col gap-3" aria-busy="true" aria-label={t('meetings.loading')}>
      {#each { length: 3 } as _, i (i)}
        <SkeletonBlock height="h-20" width="w-full" rounded="rounded-card" />
      {/each}
    </div>

  <!-- Load error -->
  {:else if stateValue === 'error'}
    <ErrorBanner message={loadError ?? t('meetings.error')} onretry={() => actor.send({ type: 'RELOAD' })} />

  {:else}
    <!-- List -->
    {#if meetings.length === 0}
      <StatePanel
        tone="empty"
        title={t('meetings.emptyTitle')}
        description={t('meetings.emptyDesc')}
      />
    {:else}
      <p class="text-sm text-gray-500">
        {t('meetings.count', { count: meetings.length })}
      </p>

      {#if cancelError}
        <ErrorBanner message={cancelError} />
      {/if}

      <ul class="flex flex-col gap-3" role="list">
        {#each meetings as meeting (meeting.id)}
          <li class="rounded-card border border-border bg-white p-4 flex flex-col gap-3">
            <!-- Header: platform + title + status -->
            <div class="flex items-start justify-between gap-3 flex-wrap">
              <div class="flex items-center gap-2 min-w-0">
                <span aria-hidden="true" class="text-lg">{platformIcons[meeting.platform] ?? '🔗'}</span>
                <span class="text-sm font-semibold text-gray-900 truncate">{meeting.title}</span>
              </div>
              <span
                class="shrink-0 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium {statusStyles[meeting.status]}"
              >
                {t(`meetings.status.${meeting.status}`)}
              </span>
            </div>

            <!-- Details -->
            <dl class="flex flex-col gap-1 text-sm">
              <div class="flex items-center justify-between gap-3">
                <dt class="text-gray-500">{t('meetings.platformLabel')}</dt>
                <dd class="font-medium text-gray-800">{t(`meetings.platform.${meeting.platform}`)}</dd>
              </div>
              <div class="flex items-center justify-between gap-3">
                <dt class="text-gray-500">{t('meetings.startsAtLabel')}</dt>
                <dd class="font-medium text-gray-800">{formatDateTime(meeting.startTime, dateLocale)}</dd>
              </div>
              <div class="flex items-center justify-between gap-3">
                <dt class="text-gray-500">{t('meetings.durationLabel')}</dt>
                <dd class="font-medium text-gray-800">{meeting.duration}</dd>
              </div>
            </dl>

            <!-- Link + actions -->
            <div class="flex items-center gap-2 flex-wrap">
              <a
                href={meeting.meetingLink || meeting.targetUrl}
                target="_blank"
                rel="noopener noreferrer"
                class="text-sm font-medium text-wakeve-600 underline underline-offset-2
                  hover:text-wakeve-700 transition-default truncate max-w-full
                  focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
              >
                {meeting.meetingLink || meeting.targetUrl}
              </a>
            </div>

            <div class="flex items-center gap-2 flex-wrap">
              <Button
                variant="secondary"
                size="sm"
                onclick={() => copyLink(meeting)}
              >
                {copiedId === meeting.id ? `✓ ${t('meetings.copied')}` : t('meetings.copy')}
              </Button>
              {#if canCancel(meeting)}
                <Button
                  variant="danger"
                  size="sm"
                  onclick={() => requestCancel(meeting)}
                >
                  {t('meetings.cancel')}
                </Button>
              {/if}
            </div>
          </li>
        {/each}
      </ul>
    {/if}

    <!-- Creation -->
    <div class="border-t border-border pt-4">
      <h3 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
        {t('meetings.create.title')}
      </h3>

      {#if isLockedByStatus}
        <div class="mt-3">
          <StatePanel
            tone="info"
            title={t('meetings.create.lockedTitle')}
            description={t('meetings.create.locked')}
          />
        </div>
      {:else if canCreate}
        {#if createError}
          <div class="mt-3">
            <ErrorBanner message={createError} />
          </div>
        {/if}

        <form
          id="meetings-create-form"
          class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3"
          onsubmit={(e) => e.preventDefault()}
        >
          <div class="sm:col-span-2">
            <Input
              id="meeting-title"
              type="text"
              label={t('meetings.create.name')}
              value={formTitle}
              placeholder={eventTitle}
              required
              oninput={(e) => { formTitle = e.currentTarget.value }}
            />
          </div>
          <Input
            id="meeting-date"
            type="date"
            label={t('meetings.create.date')}
            value={formDate}
            required
            oninput={(e) => { formDate = e.currentTarget.value }}
          />
          <Input
            id="meeting-time"
            type="time"
            label={t('meetings.create.time')}
            value={formTime}
            required
            oninput={(e) => { formTime = e.currentTarget.value }}
          />
          <div class="sm:col-span-2">
            <Select
              id="meeting-duration"
              label={t('meetings.create.duration')}
              options={durationOptions}
              value={formDuration}
              onchange={(e) => { formDuration = e.currentTarget.value }}
            />
          </div>

          <div class="sm:col-span-2 flex items-center gap-2 flex-wrap">
            <Button
              type="submit"
              loading={isCreating}
              onclick={() => submitCreation('ZOOM')}
            >
              🎥 {t('meetings.create.submitZoom')}
            </Button>
            <Button
              variant="secondary"
              type="submit"
              loading={isCreating}
              onclick={() => submitCreation('GOOGLE_MEET')}
            >
              📹 {t('meetings.create.submitMeet')}
            </Button>
          </div>
        </form>
      {/if}
    </div>
  {/if}
</div>

<!-- Cancel confirmation modal -->
<Modal
  open={pendingCancel !== null}
  title={t('meetings.cancelConfirmTitle')}
  onclose={() => { pendingCancel = null }}
>
  <p class="text-sm text-gray-700">
    {t('meetings.cancelConfirmDesc', { title: pendingCancel?.title ?? '' })}
  </p>
  {#snippet footer()}
    <div class="flex items-center justify-end gap-2">
      <Button variant="ghost" onclick={() => { pendingCancel = null }}>
        {t('common.cancel')}
      </Button>
      <Button variant="danger" onclick={confirmCancel}>
        {t('meetings.cancelConfirm')}
      </Button>
    </div>
  {/snippet}
</Modal>
