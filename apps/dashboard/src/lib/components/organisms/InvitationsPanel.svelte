<script lang="ts">
  import type {
    DirectInviteBatchResponse,
    DirectInviteCapability,
    EventResponse,
    InvitationResponse
  } from '$lib/types/api'
  import {
    createInvitation,
    getDirectInviteCapability,
    sendDirectInviteBatch
  } from '$lib/api/invitations.api'
  import * as participantsApi from '$lib/api/participants.api'
  import { t } from '$lib/i18n'
  import Button from '$lib/components/atoms/Button.svelte'
  import Textarea from '$lib/components/atoms/Textarea.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import { formatDateTime } from '$lib/utils/date'

  interface Props {
    event: EventResponse
    currentUserId: string
  }

  const { event, currentUserId }: Props = $props()

  const isOrganizer = $derived(currentUserId !== '' && currentUserId === event.organizerId)

  // ── Clipboard helper ─────────────────────────────────────────────────────
  let copied = $state<string | null>(null)
  let copyError = $state(false)

  async function copy(text: string, kind: string) {
    try {
      await navigator.clipboard.writeText(text)
      copyError = false
      copied = kind
      setTimeout(() => (copied = null), 1500)
    } catch {
      copyError = true
    }
  }

  // ── Invitation code (organizer only) ─────────────────────────────────────
  let invitation = $state<InvitationResponse | null>(null)
  let generating = $state(false)
  let generateError = $state<string | null>(null)

  async function generate() {
    generating = true
    generateError = null
    try {
      invitation = await createInvitation(event.id)
    } catch {
      generateError = t('invitations.errors.generate')
    } finally {
      generating = false
    }
  }

  // ── Direct invite delivery (capability + batch) ──────────────────────────
  type CapabilityStatus = 'unchecked' | 'checking' | 'ready' | 'unavailable'

  let capabilityStatus = $state<CapabilityStatus>('unchecked')
  let capability = $state<DirectInviteCapability | null>(null)

  async function checkCapability() {
    capabilityStatus = 'checking'
    try {
      capability = await getDirectInviteCapability(event.id)
      capabilityStatus = 'ready'
    } catch {
      // 403 (not organizer / not DRAFT) or 503 (provider not configured)
      capability = null
      capabilityStatus = 'unavailable'
    }
  }

  // Check availability once on mount when the viewer is the organizer.
  $effect(() => {
    if (isOrganizer) void checkCapability()
  })

  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  function parseEmails(raw: string): string[] {
    const seen = new Set<string>()
    for (const part of raw.split(/[\n,;]+/)) {
      const email = part.trim().toLowerCase()
      if (email) seen.add(email)
    }
    return [...seen]
  }

  function toBase64(value: string): string {
    const bytes = new TextEncoder().encode(value)
    let binary = ''
    for (const b of bytes) binary += String.fromCharCode(b)
    return btoa(binary)
  }

  // The recipient key uses the opaque identifier form accepted by the
  // backend contract (RecipientKey regex); the keyed HMAC digest port is
  // not available in the browser, so keys are opaque per-recipient UUIDs.
  function buildRecipientKey(): string {
    return `opaque:v1:${crypto.randomUUID()}`
  }

  // The payload is sealed by the delivery provider; without the platform
  // sealing port the dashboard can only transmit a descriptive ciphertext.
  function buildCiphertext(email: string): string {
    return toBase64(
      JSON.stringify({
        email,
        eventTitle: event.title,
        inviteUrl: invitation?.inviteUrl ?? null
      })
    )
  }

  let emailsRaw = $state('')
  let sending = $state(false)
  let batchError = $state<string | null>(null)
  let batchResult = $state<DirectInviteBatchResponse | null>(null)
  let emailByKey = $state<Record<string, string>>({})

  async function sendBatch() {
    const cap = capability
    if (!cap) return
    const emails = parseEmails(emailsRaw)
    const invalid = emails.filter((e) => !EMAIL_RE.test(e))
    if (invalid.length > 0) {
      batchError = t('invitations.errors.invalidEmails', { emails: invalid.join(', ') })
      return
    }
    if (emails.length === 0) return

    sending = true
    batchError = null
    batchResult = null
    try {
      const expiresAt = new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString()
      const mapping: Record<string, string> = {}
      const envelopes = emails.map((email) => {
        const recipientKey = buildRecipientKey()
        mapping[recipientKey] = email
        return {
          recipientKey,
          ciphertext: buildCiphertext(email),
          keyVersion: cap.keyVersion ?? 1,
          expiresAt
        }
      })
      const res = await sendDirectInviteBatch(event.id, {
        accessRevision: cap.accessRevision,
        batchId: crypto.randomUUID(),
        operationId: crypto.randomUUID(),
        envelopes
      })
      emailByKey = mapping
      batchResult = res
      emailsRaw = ''
    } catch (e) {
      batchError =
        e instanceof Error && e.message.includes('409')
          ? t('invitations.errors.batchStale')
          : t('invitations.errors.batch')
      // The event may have changed — refresh the capability revision.
      void checkCapability()
    } finally {
      sending = false
    }
  }

  // ── Audience (participants + known RSVP outcomes) ────────────────────────
  let participants = $state<string[]>([])
  let audienceLoading = $state(true)
  let audienceError = $state<string | null>(null)

  async function loadAudience() {
    audienceLoading = true
    audienceError = null
    try {
      const res = await participantsApi.list(event.id)
      participants = res.participants
    } catch {
      audienceError = t('participants.errors.load')
    } finally {
      audienceLoading = false
    }
  }

  loadAudience()

  // The API does not expose a global RSVP state yet: "joined" comes from the
  // participants list, and "pending" is derived from expectedParticipants
  // when the organizer provided an estimate.
  const joinedCount = $derived(Math.max(participants.length, event.participantCount))
  const pendingCount = $derived.by(() => {
    if (event.expectedParticipants == null) return null
    return Math.max(event.expectedParticipants - joinedCount, 0)
  })
</script>

<div class="flex flex-col gap-6">
  {#if isOrganizer}
    <!-- Invitation code -->
    <section class="flex flex-col gap-3 rounded-card border border-border bg-surface p-4">
      <h3 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
        {t('invitations.codeTitle')}
      </h3>
      <p class="text-sm text-gray-500">{t('invitations.codeIntro')}</p>

      {#if generateError}
        <ErrorBanner message={generateError} onretry={generate} />
      {/if}
      {#if copyError}
        <ErrorBanner message={t('invitations.errors.copy')} />
      {/if}

      {#if invitation}
        <div class="flex flex-col gap-3">
          <div class="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:justify-between">
            <div class="min-w-0">
              <p class="text-xs text-gray-500">{t('invitations.code')}</p>
              <p class="font-mono text-lg font-semibold tracking-widest text-gray-900">
                {invitation.code}
              </p>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onclick={() => copy(invitation!.code, 'code')}
            >
              {copied === 'code' ? t('invitations.copied') : t('invitations.copy')}
            </Button>
          </div>

          <div class="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:justify-between">
            <div class="min-w-0">
              <p class="text-xs text-gray-500">{t('invitations.link')}</p>
              <p class="truncate text-sm text-wakeve-700">{invitation.inviteUrl}</p>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onclick={() => copy(invitation!.inviteUrl, 'link')}
            >
              {copied === 'link' ? t('invitations.copied') : t('invitations.copy')}
            </Button>
          </div>

          <p class="text-xs text-gray-400">
            {invitation.maxUses != null
              ? t('invitations.uses', { current: invitation.currentUses })
              : t('invitations.unlimitedUses')}
          </p>
        </div>
      {:else}
        <div>
          <Button loading={generating} onclick={generate}>
            {t('invitations.generate')}
          </Button>
        </div>
      {/if}
    </section>

    <!-- Direct email delivery -->
    <section class="flex flex-col gap-3 rounded-card border border-border bg-surface p-4">
      <h3 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
        {t('invitations.directTitle')}
      </h3>

      {#if capabilityStatus === 'checking'}
        <p class="text-sm text-gray-500">{t('invitations.checking')}</p>
      {:else if capabilityStatus === 'unavailable'}
        <div class="rounded-btn bg-amber-50 px-3 py-2 text-sm text-amber-800" role="status">
          {t('invitations.directUnavailable')}
        </div>
        <div>
          <Button variant="ghost" size="sm" onclick={checkCapability}>
            {t('invitations.directCheck')}
          </Button>
        </div>
      {:else if capabilityStatus === 'ready' && capability}
        <p class="text-sm text-green-700">✓ {t('invitations.directReady')}</p>

        <div class="flex flex-col gap-2">
          <Textarea
            id="invite-emails"
            label={t('invitations.emailsLabel')}
            placeholder={t('invitations.emailsPlaceholder')}
            rows={3}
            value={emailsRaw}
            disabled={sending}
            oninput={(e) => (emailsRaw = e.currentTarget.value)}
          />
          <div>
            <Button loading={sending} disabled={!emailsRaw.trim()} onclick={sendBatch}>
              {t('invitations.send')}
            </Button>
          </div>
        </div>

        {#if batchError}
          <ErrorBanner message={batchError} />
        {/if}

        {#if batchResult}
          <div class="rounded-btn bg-green-50 px-3 py-2 text-sm text-green-800" role="status">
            {t('invitations.batchSent')}
          </div>
          <ul class="flex flex-col gap-1.5" role="list">
            {#each batchResult.outcomes as outcome (outcome.recipientKey)}
              <li class="flex items-center justify-between rounded-btn bg-gray-50 px-3 py-1.5 text-sm">
                <span class="truncate text-gray-700">
                  {emailByKey[outcome.recipientKey] ?? outcome.recipientKey}
                </span>
                <span
                  class="ml-2 shrink-0 rounded-full px-2 py-0.5 text-xs font-medium
                    {outcome.status === 'SERVER_ACCEPTED' ? 'bg-green-100 text-green-700'
                      : outcome.status === 'INVALID' ? 'bg-amber-100 text-amber-700'
                      : 'bg-red-100 text-red-700'}"
                >
                  {t(`invitations.outcome.${outcome.status}`)}
                  {#if outcome.reasonCode}
                    <span class="ml-1 opacity-70">({outcome.reasonCode})</span>
                  {/if}
                </span>
              </li>
            {/each}
          </ul>
        {/if}
      {/if}
    </section>
  {/if}

  <!-- Audience -->
  <section class="flex flex-col gap-3 rounded-card border border-border bg-surface p-4">
    <h3 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
      {t('invitations.audienceTitle')}
    </h3>

    {#if audienceLoading}
      <p class="text-sm text-gray-500">{t('common.loading')}</p>
    {:else if audienceError}
      <ErrorBanner message={audienceError} onretry={loadAudience} />
    {:else}
      <dl class="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
        <div class="rounded-btn bg-green-50 px-3 py-2">
          <dt class="text-xs text-green-600">{t('invitations.audienceJoined')}</dt>
          <dd class="mt-0.5 text-lg font-semibold text-green-700">{joinedCount}</dd>
        </div>
        <div class="rounded-btn bg-amber-50 px-3 py-2">
          <dt class="text-xs text-amber-600">{t('invitations.audiencePending')}</dt>
          <dd class="mt-0.5 text-lg font-semibold text-amber-700">
            {pendingCount ?? '—'}
          </dd>
        </div>
      </dl>
      {#if event.finalDate}
        <p class="text-sm text-green-700">
          ✓ {t('invitations.audienceDateValidated', { date: formatDateTime(event.finalDate) })}
        </p>
      {/if}
      <p class="text-xs text-gray-400">{t('invitations.audienceHint')}</p>
    {/if}
  </section>
</div>
