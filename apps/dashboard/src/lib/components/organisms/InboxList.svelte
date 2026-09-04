<script lang="ts">
  import type { Notification } from '$lib/types/api'
  import Button from '$lib/components/atoms/Button.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import { timeAgo } from '$lib/utils/date'
  import { t, getLocale } from '$lib/i18n'

  interface Props {
    notifications: Notification[]
    loading: boolean
    error: string | null
    mutationError: string | null
    unreadTotal: number
    unreadOnly: boolean
    busy: boolean
    onsetfilter: (unreadOnly: boolean) => void
    onmarkread: (id: string) => void
    onmarkallread: () => void
    ondelete: (id: string) => void
    onreload: () => void
  }

  const {
    notifications,
    loading,
    error,
    mutationError,
    unreadTotal,
    unreadOnly,
    busy,
    onsetfilter,
    onmarkread,
    onmarkallread,
    ondelete,
    onreload
  }: Props = $props()
</script>

<div class="flex flex-col gap-4">
  <!-- Toolbar: unread filter + mark all read -->
  <div class="flex flex-wrap items-center justify-between gap-3">
    <button
      type="button"
      aria-pressed={unreadOnly}
      onclick={() => onsetfilter(!unreadOnly)}
      class="inline-flex items-center gap-2 rounded-btn border px-3 py-1.5 text-sm font-medium transition-default
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
        {unreadOnly
          ? 'border-wakeve-600 bg-wakeve-50 text-wakeve-700 shadow-sm'
          : 'border-border bg-white text-gray-600 hover:bg-gray-50'}"
    >
      {t('inbox.filterUnread')}
      {#if unreadTotal > 0}
        <span
          class="inline-flex min-w-[1.25rem] items-center justify-center rounded-full bg-wakeve-600 px-1.5 py-0.5
            text-[0.6875rem] font-semibold leading-none text-white"
          aria-label={t('inbox.unreadBadge', { n: unreadTotal })}
        >
          {unreadTotal > 99 ? '99+' : unreadTotal}
        </span>
      {/if}
    </button>

    <Button
      variant="ghost"
      size="sm"
      disabled={busy || unreadTotal === 0}
      onclick={onmarkallread}
    >
      {t('inbox.markAllRead')}
    </Button>
  </div>

  {#if mutationError}
    <ErrorBanner message={t('inbox.actionError')} />
  {/if}

  <!-- Loading skeletons -->
  {#if loading}
    <ul class="flex flex-col gap-2" role="list" aria-label={t('inbox.loading')}>
      {#each { length: 4 } as _, i (i)}
        <li class="bg-white rounded-card shadow-card p-4 flex gap-3">
          <SkeletonBlock height="h-2.5" width="w-2.5" rounded="rounded-full" />
          <div class="flex-1 flex flex-col gap-2">
            <SkeletonBlock height="h-3" width="w-24" rounded="rounded-full" />
            <SkeletonBlock height="h-4" width="w-3/4" />
            <SkeletonBlock height="h-3" width="w-5/6" />
          </div>
        </li>
      {/each}
    </ul>

  <!-- Error -->
  {:else if error}
    <ErrorBanner message={error} onretry={onreload} />

  <!-- Empty states -->
  {:else if notifications.length === 0}
    <div class="flex flex-col items-center justify-center py-20 text-center gap-4">
      <span class="text-6xl" aria-hidden="true">📬</span>
      <div class="flex flex-col gap-1">
        {#if unreadOnly}
          <p class="text-lg font-semibold text-gray-800">{t('inbox.emptyUnread')}</p>
          <p class="text-sm text-gray-500 max-w-xs text-balance">{t('inbox.emptyUnreadDesc')}</p>
        {:else}
          <p class="text-lg font-semibold text-gray-800">{t('inbox.empty')}</p>
          <p class="text-sm text-gray-500 max-w-xs text-balance">{t('inbox.emptyDesc')}</p>
        {/if}
      </div>
    </div>

  <!-- List -->
  {:else}
    <ul class="flex flex-col gap-2" role="list">
      {#each notifications as notification (notification.id)}
        <li
          class="bg-white rounded-card shadow-card p-4 flex gap-3
            {notification.readAt === null ? '' : 'opacity-75'}"
        >
          <!-- Unread dot -->
          <div class="pt-1.5 shrink-0 w-2.5">
            {#if notification.readAt === null}
              <span
                class="block h-2.5 w-2.5 rounded-full bg-wakeve-600"
                aria-label={t('inbox.unread')}
              ></span>
            {/if}
          </div>

          <!-- Content -->
          <div class="flex-1 min-w-0 flex flex-col gap-1">
            <div class="flex items-center gap-2 flex-wrap">
              <span
                class="text-[0.6875rem] font-semibold uppercase tracking-wide text-wakeve-700
                  bg-wakeve-50 rounded-full px-2 py-0.5"
              >
                {t(`inbox.types.${notification.type}`)}
              </span>
              {#if notification.sentAt}
                <span class="text-xs text-gray-400">{timeAgo(notification.sentAt, getLocale())}</span>
              {/if}
            </div>
            <p
              class="text-sm text-gray-900 truncate
                {notification.readAt === null ? 'font-semibold' : 'font-medium'}"
            >
              {notification.title}
            </p>
            <p class="text-sm text-gray-600">{notification.body}</p>
          </div>

          <!-- Actions -->
          <div class="flex flex-col items-end gap-1 shrink-0">
            {#if notification.readAt === null}
              <Button
                variant="ghost"
                size="sm"
                disabled={busy}
                onclick={() => onmarkread(notification.id)}
              >
                {t('inbox.markRead')}
              </Button>
            {/if}
            <Button
              variant="ghost"
              size="sm"
              disabled={busy}
              class="!text-red-600 hover:!bg-red-50"
              onclick={() => ondelete(notification.id)}
            >
              {t('inbox.delete')}
            </Button>
          </div>
        </li>
      {/each}
    </ul>
  {/if}
</div>
