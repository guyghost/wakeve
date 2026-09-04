<script lang="ts">
  import { createActor } from 'xstate'
  import { onDestroy } from 'svelte'
  import { budgetMachine } from '$lib/machines/budget.machine'
  import type { BudgetCategory, BudgetItem } from '$lib/types/api'
  import { t, currentLocale } from '$lib/i18n'
  import Avatar from '$lib/components/atoms/Avatar.svelte'
  import Button from '$lib/components/atoms/Button.svelte'
  import Input from '$lib/components/atoms/Input.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import Textarea from '$lib/components/atoms/Textarea.svelte'
  import MetricTile from '$lib/components/molecules/MetricTile.svelte'
  import Modal from '$lib/components/ui/Modal.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import StatePanel from '$lib/components/ui/StatePanel.svelte'

  interface Props {
    eventId: string
    canManage?: boolean
  }

  const { eventId, canManage = false }: Props = $props()

  // eventId is intentionally captured once: the panel is remounted per tab visit.
  // svelte-ignore state_referenced_locally
  const actor = createActor(budgetMachine, { input: { eventId: $state.snapshot(eventId) } })
  let snapshot = $state(actor.getSnapshot())
  const sub = actor.subscribe((s) => { snapshot = s })
  actor.start()

  onDestroy(() => { sub.unsubscribe(); actor.stop() })

  const ctx = $derived(snapshot.context)
  const stateValue = $derived(snapshot.value as string)
  const isMutating = $derived(stateValue === 'addingItem' || stateValue === 'deletingItem')

  // ─── Amounts & helpers ──────────────────────────────────────────────────────

  const totalSpent = $derived(
    ctx.items.reduce(
      (sum, item) => sum + (item.isPaid && item.actualCost > 0 ? item.actualCost : item.estimatedCost),
      0
    )
  )
  const plannedTotal = $derived(
    ctx.budget && ctx.budget.totalEstimated > 0 ? ctx.budget.totalEstimated : null
  )

  function formatAmount(value: number): string {
    return new Intl.NumberFormat(currentLocale.value === 'en' ? 'en-US' : 'fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(value)
  }

  const participantsById = $derived(new Map(ctx.participants.map((p) => [p.id, p.displayName])))

  function displayName(participantId: string | null): string {
    if (!participantId) return '?'
    return participantsById.get(participantId) ?? participantId.slice(0, 8)
  }

  function categoryLabel(category: BudgetCategory): string {
    return t(`budget.categories.${category}`)
  }

  function settlementStatusLabel(status: string): string {
    const key = `budget.settlementStatus.${status}`
    const label = t(key)
    // Unknown status: fall back to the raw value.
    return label === key ? status : label
  }

  const CATEGORY_BADGE: Record<BudgetCategory, string> = {
    TRANSPORT: 'bg-sky-50 text-sky-700',
    ACCOMMODATION: 'bg-violet-50 text-violet-700',
    MEALS: 'bg-amber-50 text-amber-700',
    ACTIVITIES: 'bg-emerald-50 text-emerald-700',
    EQUIPMENT: 'bg-slate-100 text-slate-700',
    OTHER: 'bg-gray-100 text-gray-600'
  }

  const categoryOptions: { value: BudgetCategory; label: string }[] = [
    { value: 'TRANSPORT', label: categoryLabel('TRANSPORT') },
    { value: 'ACCOMMODATION', label: categoryLabel('ACCOMMODATION') },
    { value: 'MEALS', label: categoryLabel('MEALS') },
    { value: 'ACTIVITIES', label: categoryLabel('ACTIVITIES') },
    { value: 'EQUIPMENT', label: categoryLabel('EQUIPMENT') },
    { value: 'OTHER', label: categoryLabel('OTHER') }
  ]

  // ─── Add item form (component-local state) ──────────────────────────────────

  let showAddModal = $state(false)
  let name = $state('')
  let description = $state('')
  let category = $state<BudgetCategory>('TRANSPORT')
  let amount = $state('')

  const parsedAmount = $derived(Number(amount.replace(',', '.')))
  const canSubmit = $derived(
    name.trim().length > 0 && Number.isFinite(parsedAmount) && parsedAmount > 0
  )

  function openAddModal() {
    name = ''
    description = ''
    category = 'TRANSPORT'
    amount = ''
    showAddModal = true
  }

  function closeAddModal() {
    showAddModal = false
  }

  function handleNameInput(e: Event & { currentTarget: HTMLInputElement }) {
    name = e.currentTarget.value
  }

  function handleDescriptionInput(e: Event & { currentTarget: HTMLTextAreaElement }) {
    description = e.currentTarget.value
  }

  function handleCategoryChange(e: Event & { currentTarget: HTMLSelectElement }) {
    category = e.currentTarget.value as BudgetCategory
  }

  function handleAmountInput(e: Event & { currentTarget: HTMLInputElement }) {
    amount = e.currentTarget.value
  }

  function submitAddItem() {
    if (!canSubmit) return
    actor.send({
      type: 'ADD_ITEM',
      data: {
        name: name.trim(),
        description: description.trim(),
        category,
        estimatedCost: parsedAmount
      }
    })
    showAddModal = false
  }

  // ─── Delete confirmation (component-local state) ────────────────────────────

  let itemToDelete = $state<BudgetItem | null>(null)

  function confirmDelete() {
    if (!itemToDelete) return
    actor.send({ type: 'DELETE_ITEM', itemId: itemToDelete.id })
    itemToDelete = null
  }
</script>

{#if stateValue === 'error'}
  {#if ctx.errorKind === 'permission'}
    <StatePanel
      tone="permission"
      title={t('budget.errors.permission')}
      description={t('budget.readOnly')}
      actionLabel={t('common.retry')}
      onretry={() => actor.send({ type: 'RELOAD' })}
    />
  {:else if ctx.errorKind === 'offline'}
    <StatePanel
      tone="offline"
      title={t('budget.errors.offline')}
      description={ctx.error ?? ''}
      actionLabel={t('common.retry')}
      onretry={() => actor.send({ type: 'RELOAD' })}
    />
  {:else}
    <StatePanel
      tone="error"
      title={t('budget.errors.load')}
      description={ctx.error ?? ''}
      actionLabel={t('common.retry')}
      onretry={() => actor.send({ type: 'RELOAD' })}
    />
  {/if}
{:else if stateValue === 'loading'}
  <!-- Loading skeleton -->
  <div class="flex flex-col gap-6" role="status" aria-label={t('budget.loading')}>
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
      {#each { length: 3 } as _, i (i)}
        <SkeletonBlock height="h-20" rounded="rounded-lg" />
      {/each}
    </div>
    <div class="bg-white rounded-card shadow-card p-5 flex flex-col gap-3">
      {#each { length: 3 } as _, i (i)}
        <SkeletonBlock height="h-12" rounded="rounded-btn" />
      {/each}
    </div>
  </div>
{:else}
  <div class="flex flex-col gap-6">

    <!-- Summary tiles -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <MetricTile label={t('budget.total')} value={formatAmount(totalSpent)} />
      <MetricTile
        label={t('budget.totalEstimated')}
        value={plannedTotal !== null ? formatAmount(plannedTotal) : '—'}
      />
      <MetricTile
        label={t('budget.items')}
        value={ctx.items.length === 1
          ? t('budget.itemCount', { count: ctx.items.length })
          : t('budget.itemCountPlural', { count: ctx.items.length })}
      />
    </div>

    <!-- Items -->
    <section class="bg-white rounded-card shadow-card p-5 flex flex-col gap-4" aria-label={t('budget.items')}>
      <div class="flex items-center justify-between gap-4 flex-wrap">
        <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">{t('budget.items')}</h2>
        {#if canManage}
          <Button variant="secondary" size="sm" onclick={openAddModal} disabled={isMutating}>
            <span aria-hidden="true">＋</span> {t('budget.add')}
          </Button>
        {/if}
      </div>

      {#if !canManage && ctx.items.length > 0}
        <p class="text-xs text-gray-400">{t('budget.readOnly')}</p>
      {/if}

      {#if ctx.itemError}
        <ErrorBanner message={t('budget.errors.addItem')} />
      {:else if ctx.deleteError}
        <ErrorBanner message={t('budget.errors.deleteItem')} onretry={() => actor.send({ type: 'RELOAD' })} />
      {/if}

      {#if ctx.items.length === 0}
        <div class="flex flex-col items-center justify-center py-10 text-center gap-2">
          <span class="text-3xl" aria-hidden="true">💰</span>
          <p class="text-sm font-medium text-gray-700">{t('budget.empty')}</p>
          <p class="text-sm text-gray-500 max-w-sm">{t('budget.emptyDesc')}</p>
          {#if canManage}
            <div class="mt-2">
              <Button variant="primary" size="sm" onclick={openAddModal} disabled={isMutating}>
                {t('budget.add')}
              </Button>
            </div>
          {/if}
        </div>
      {:else}
        <ul class="flex flex-col gap-2" role="list">
          {#each ctx.items as item (item.id)}
            <li class="flex items-center gap-3 rounded-btn bg-gray-50 px-3 py-2.5">
              <Avatar name={displayName(item.paidBy)} size="sm" />
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="text-sm font-medium text-gray-900 truncate">{item.name}</span>
                  <span
                    class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium {CATEGORY_BADGE[item.category]}"
                  >
                    {categoryLabel(item.category)}
                  </span>
                  <span
                    class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium {item.isPaid
                      ? 'bg-emerald-50 text-emerald-700'
                      : 'bg-amber-50 text-amber-700'}"
                  >
                    {item.isPaid ? t('budget.paid') : t('budget.unpaid')}
                  </span>
                </div>
                {#if item.description}
                  <p class="text-xs text-gray-500 truncate">{item.description}</p>
                {/if}
                {#if item.paidBy}
                  <p class="text-xs text-gray-400">{t('budget.payer')}&nbsp;: {displayName(item.paidBy)}</p>
                {/if}
              </div>
              <span class="shrink-0 text-sm font-semibold tabular-nums text-gray-900">
                {formatAmount(item.isPaid && item.actualCost > 0 ? item.actualCost : item.estimatedCost)}
              </span>
              {#if canManage}
                <button
                  type="button"
                  onclick={() => (itemToDelete = item)}
                  disabled={isMutating}
                  class="shrink-0 rounded p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 transition-default
                    disabled:opacity-50 disabled:cursor-not-allowed
                    focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-600"
                  aria-label="{t('budget.deleteItem')} — {item.name}"
                  title={t('budget.deleteItem')}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    class="h-4 w-4"
                    aria-hidden="true"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"
                    />
                  </svg>
                </button>
              {/if}
            </li>
          {/each}
        </ul>
      {/if}
    </section>

    <!-- Settlements -->
    <section class="bg-white rounded-card shadow-card p-5 flex flex-col gap-4" aria-label={t('budget.settlements')}>
      <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">{t('budget.settlements')}</h2>

      {#if ctx.settlements.length === 0}
        <p class="text-sm text-gray-500">{t('budget.settlementsEmpty')}</p>
      {:else}
        <ul class="flex flex-col gap-2" role="list">
          {#each ctx.settlements as settlement (settlement.settlementId)}
            {@const fromName = displayName(settlement.fromParticipantId)}
            {@const toName = displayName(settlement.toParticipantId)}
            <li
              class="flex items-center gap-3 rounded-btn bg-gray-50 px-3 py-2.5"
              aria-label={t('budget.owes', { from: fromName, amount: formatAmount(settlement.amount), to: toName })}
            >
              <Avatar name={fromName} size="sm" />
              <div class="flex-1 min-w-0 flex items-center gap-2 flex-wrap">
                <span class="text-sm text-gray-900 truncate">{fromName}</span>
                <span class="text-gray-400" aria-hidden="true">→</span>
                <span class="text-sm text-gray-900 truncate">{toName}</span>
              </div>
              <span
                class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium {settlement.status === 'PENDING'
                  ? 'bg-amber-50 text-amber-700'
                  : 'bg-emerald-50 text-emerald-700'}"
              >
                {settlementStatusLabel(settlement.status)}
              </span>
              <span class="shrink-0 text-sm font-semibold tabular-nums text-gray-900">
                {formatAmount(settlement.amount)}
              </span>
            </li>
          {/each}
        </ul>
      {/if}
    </section>
  </div>

  <!-- Add item modal -->
  <Modal open={showAddModal} title={t('budget.addTitle')} onclose={closeAddModal}>
    {#snippet children()}
      <div class="flex flex-col gap-4">
        <Input
          id="budget-item-name"
          label={t('budget.name')}
          value={name}
          placeholder={t('budget.namePlaceholder')}
          required
          oninput={handleNameInput}
        />
        <Textarea
          id="budget-item-description"
          label={t('budget.description')}
          value={description}
          placeholder={t('budget.descriptionPlaceholder')}
          rows={2}
          oninput={handleDescriptionInput}
        />
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Select
            id="budget-item-category"
            label={t('budget.category')}
            value={category}
            options={categoryOptions}
            onchange={handleCategoryChange}
          />
          <Input
            id="budget-item-amount"
            type="number"
            label={t('budget.amount')}
            value={amount}
            placeholder={t('budget.amountPlaceholder')}
            required
            oninput={handleAmountInput}
          />
        </div>
      </div>
    {/snippet}

    {#snippet footer()}
      <div class="flex items-center justify-end gap-3">
        <Button variant="ghost" size="md" onclick={closeAddModal}>
          {t('common.cancel')}
        </Button>
        <Button variant="primary" size="md" disabled={!canSubmit} onclick={submitAddItem}>
          {t('budget.submit')}
        </Button>
      </div>
    {/snippet}
  </Modal>

  <!-- Delete confirmation modal -->
  <Modal
    open={itemToDelete !== null}
    title={t('budget.deleteTitle')}
    onclose={() => (itemToDelete = null)}
  >
    {#snippet children()}
      <p class="text-sm text-gray-700">
        {t('budget.deleteMessage', { name: itemToDelete?.name ?? '' })}
      </p>
    {/snippet}

    {#snippet footer()}
      <div class="flex items-center justify-end gap-3">
        <Button variant="ghost" size="md" onclick={() => (itemToDelete = null)}>
          {t('common.cancel')}
        </Button>
        <Button variant="danger" size="md" disabled={isMutating} onclick={confirmDelete}>
          {t('common.delete')}
        </Button>
      </div>
    {/snippet}
  </Modal>
{/if}
