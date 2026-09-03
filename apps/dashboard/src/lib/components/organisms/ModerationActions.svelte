<script lang="ts">
  import { t } from '$lib/i18n'
  import { reportContent, decide, blockUser } from '$lib/api/moderation.api'
  import type { Comment, ModerationStatus, ReportReason } from '$lib/types/api'
  import Select from '$lib/components/atoms/Select.svelte'
  import Textarea from '$lib/components/atoms/Textarea.svelte'

  interface Props {
    comment: Comment
    eventid: string
    currentuserid: string
    /** Whether the current user may record moderation decisions (MODERATOR/ADMIN role). */
    canmoderate?: boolean
  }

  const { comment, eventid, currentuserid, canmoderate = false }: Props = $props()

  let open = $state(false)
  let submitting = $state(false)
  let statusMessage = $state('')
  let errorMessage = $state('')
  let reason = $state<ReportReason>('HARASSMENT')
  let details = $state('')

  const canBlock = $derived(comment.authorId !== currentuserid)

  const REASON_KEYS: ReportReason[] = [
    'HARASSMENT',
    'HATE_OR_ABUSE',
    'SEXUAL_CONTENT',
    'VIOLENCE_OR_THREAT',
    'SPAM_OR_SCAM',
    'PRIVATE_INFORMATION',
    'OTHER'
  ]

  const reasonOptions = $derived(
    REASON_KEYS.map((value) => ({ value, label: t(`moderation.reasons.${value}`) }))
  )

  const STATUS_BADGES: Partial<Record<ModerationStatus, { key: string; classes: string }>> = {
    PENDING_REVIEW: { key: 'moderation.status.PENDING_REVIEW', classes: 'bg-amber-100 text-amber-700' },
    REJECTED: { key: 'moderation.status.REJECTED', classes: 'bg-red-100 text-red-700' },
    HIDDEN: { key: 'moderation.status.HIDDEN', classes: 'bg-gray-200 text-gray-600' }
  }

  const statusBadge = $derived(
    comment.moderationStatus ? STATUS_BADGES[comment.moderationStatus] : undefined
  )

  function handleReasonChange(e: Event & { currentTarget: HTMLSelectElement }) {
    reason = e.currentTarget.value as ReportReason
  }

  function handleDetailsInput(e: Event & { currentTarget: HTMLTextAreaElement }) {
    details = e.currentTarget.value
  }

  function reset() {
    statusMessage = ''
    errorMessage = ''
    submitting = false
  }

  function toggle() {
    open = !open
    if (open) reset()
  }

  async function submit(action: 'report-comment' | 'report-user' | 'block' | 'hide') {
    submitting = true
    errorMessage = ''
    statusMessage = ''
    const trimmedDetails = details.trim()

    try {
      if (action === 'report-comment' || action === 'hide') {
        const report = await reportContent({
          targetType: 'COMMENT',
          targetId: comment.id,
          eventId: eventid || undefined,
          reason,
          details: trimmedDetails || undefined
        })
        if (action === 'hide') {
          await decide(report.id, {
            targetType: 'COMMENT',
            targetId: comment.id,
            action: 'HIDE',
            reason: reason
          })
          statusMessage = t('moderation.hideSubmitted')
        } else {
          statusMessage = t('moderation.reportSubmitted')
        }
      } else if (action === 'report-user') {
        await reportContent({
          targetType: 'USER',
          targetId: comment.authorId,
          eventId: eventid || undefined,
          reason,
          details: trimmedDetails || undefined
        })
        statusMessage = t('moderation.reportSubmitted')
      } else {
        await blockUser({
          blockedUserId: comment.authorId,
          eventId: eventid || undefined,
          reason
        })
        statusMessage = t('moderation.userBlocked')
      }
    } catch {
      errorMessage = t('moderation.error')
    } finally {
      submitting = false
    }
  }
</script>

<div class="mt-1">
  {#if statusBadge}
    <span
      class="mb-1 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium {statusBadge.classes}"
      role="note"
    >
      {t(statusBadge.key)}
    </span>
  {/if}

  <button
    type="button"
    onclick={toggle}
    aria-expanded={open}
    aria-haspopup="true"
    aria-label={t('moderation.actionsLabel')}
    class="inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-xs text-gray-400
      hover:bg-gray-100 hover:text-gray-600 transition-default
      focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
  >
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="h-3.5 w-3.5" aria-hidden="true">
      <path d="M10 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM6 8a2 2 0 1 1-4 0 2 2 0 0 1 4 0ZM1.49 15.326a.78.78 0 0 1-.358-.442 3 3 0 0 1 4.308-3.516 6.484 6.484 0 0 1-3.005 3.811.75.75 0 0 1-.945.147ZM14.7 11.368a3 3 0 0 1 4.308 3.517.78.78 0 0 1-.358.441.75.75 0 0 1-.946-.146 6.484 6.484 0 0 1-3.004-3.812Z" />
      <path d="M10 18a7.5 7.5 0 0 1-7.41-6.578a.75.75 0 0 1 .358-.588L4 10.5l1.25-.75a.75.75 0 0 1 .898.12A5.98 5.98 0 0 0 10 11.5a5.98 5.98 0 0 0 3.852-1.63.75.75 0 0 1 .898-.12L16 10.5l1.052.334a.75.75 0 0 1 .358.588A7.5 7.5 0 0 1 10 18Z" />
    </svg>
    {t('moderation.actionsLabel')}
  </button>

  {#if open}
    <form
      class="mt-2 flex flex-col gap-3 rounded-btn border border-border bg-gray-50 p-3"
      onsubmit={(e) => e.preventDefault()}
    >
      <p class="text-xs text-gray-500">{t('moderation.help')}</p>

      <Select
        id="moderation-reason-{comment.id}"
        label={t('moderation.reason')}
        value={reason}
        options={reasonOptions}
        onchange={handleReasonChange}
      />

      <Textarea
        id="moderation-details-{comment.id}"
        label=""
        value={details}
        placeholder={t('moderation.detailsPlaceholder')}
        rows={2}
        oninput={handleDetailsInput}
      />

      <div class="flex flex-wrap items-center gap-2">
        <button
          type="button"
          disabled={submitting}
          onclick={() => submit('report-comment')}
          class="rounded-btn border border-border bg-surface px-3 py-1.5 text-xs font-medium text-gray-700
            hover:bg-gray-100 transition-default disabled:opacity-50
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
        >
          {t('moderation.reportComment')}
        </button>

        <button
          type="button"
          disabled={submitting}
          onclick={() => submit('report-user')}
          class="rounded-btn border border-border bg-surface px-3 py-1.5 text-xs font-medium text-gray-700
            hover:bg-gray-100 transition-default disabled:opacity-50
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
        >
          {t('moderation.reportUser')}
        </button>

        {#if canBlock}
          <button
            type="button"
            disabled={submitting}
            onclick={() => submit('block')}
            class="rounded-btn border border-red-200 bg-surface px-3 py-1.5 text-xs font-medium text-red-600
              hover:bg-red-50 transition-default disabled:opacity-50
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-500"
          >
            {t('moderation.blockUser')}
          </button>
        {/if}

        {#if canmoderate}
          <button
            type="button"
            disabled={submitting}
            onclick={() => submit('hide')}
            class="rounded-btn bg-gray-800 px-3 py-1.5 text-xs font-medium text-white
              hover:bg-gray-900 transition-default disabled:opacity-50
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gray-800"
          >
            {t('moderation.hideComment')}
          </button>
        {/if}
      </div>

      {#if statusMessage}
        <p class="text-xs font-medium text-emerald-700" role="status">{statusMessage}</p>
      {/if}
      {#if errorMessage}
        <p class="text-xs font-medium text-red-600" role="alert">{errorMessage}</p>
      {/if}
    </form>
  {/if}
</div>
