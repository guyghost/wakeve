<script lang="ts">
  import { createActor } from 'xstate'
  import { onDestroy } from 'svelte'
  import {
    inboxMachine,
    deriveVisibleNotifications,
    countUnreadNotifications
  } from '$lib/machines/inbox.machine'
  import InboxList from '$lib/components/organisms/InboxList.svelte'
  import { t } from '$lib/i18n'

  const actor = createActor(inboxMachine)
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => {
    sub.unsubscribe()
    actor.stop()
  })

  const visibleNotifications = $derived(
    deriveVisibleNotifications(snapshot.context.notifications, snapshot.context.unreadOnly)
  )
  const unreadTotal = $derived(countUnreadNotifications(snapshot.context.notifications))
  const busy = $derived(
    snapshot.value === 'markingRead' ||
      snapshot.value === 'markingAllRead' ||
      snapshot.value === 'deleting'
  )
</script>

<div class="flex flex-col gap-6">
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-bold text-gray-900">{t('inbox.title')}</h1>
  </div>

  <InboxList
    notifications={visibleNotifications}
    loading={snapshot.value === 'loading'}
    error={snapshot.context.error}
    mutationError={snapshot.context.mutationError}
    {unreadTotal}
    unreadOnly={snapshot.context.unreadOnly}
    {busy}
    onsetfilter={(value) => actor.send({ type: 'SET_UNREAD_FILTER', value })}
    onmarkread={(id) => actor.send({ type: 'MARK_READ', id })}
    onmarkallread={() => actor.send({ type: 'MARK_ALL_READ' })}
    ondelete={(id) => actor.send({ type: 'DELETE', id })}
    onreload={() => actor.send({ type: 'RELOAD' })}
  />
</div>
