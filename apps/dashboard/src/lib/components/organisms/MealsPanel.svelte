<script lang="ts">
  import type {
    CreateMealRequest,
    DailyMealSchedule,
    MealStatus,
    MealType,
    MealPlanningSummary
  } from '$lib/types/api'
  import * as mealsApi from '$lib/api/meals.api'
  import { t } from '$lib/i18n'
  import Button from '$lib/components/atoms/Button.svelte'
  import Input from '$lib/components/atoms/Input.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import Textarea from '$lib/components/atoms/Textarea.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import StatePanel from '$lib/components/ui/StatePanel.svelte'
  import { formatDate } from '$lib/utils/date'

  interface Props {
    eventId: string
    isOrganizer: boolean
  }

  const { eventId, isOrganizer }: Props = $props()

  let loading = $state(true)
  let loadError = $state<string | null>(null)
  let schedule = $state<DailyMealSchedule[]>([])
  let summary = $state<MealPlanningSummary | null>(null)
  let dietaryCounts = $state<Partial<Record<string, number>>>({})

  let showForm = $state(false)
  let adding = $state(false)
  let actionError = $state<string | null>(null)
  let deletingMealId = $state<string | null>(null)

  let type = $state<MealType>('DINNER')
  let name = $state('')
  let date = $state('')
  let time = $state('19:00')
  let servings = $state(6)
  let costEuros = $state(60)
  let location = $state('')
  let notes = $state('')

  const typeOptions: { value: MealType; label: string }[] = (
    ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK', 'APERITIF'] as MealType[]
  ).map((value) => ({ value, label: t(`eventDetail.meals.types.${value}`) }))

  async function load() {
    loading = true
    loadError = null
    try {
      const results = await Promise.allSettled([
        mealsApi.getSchedule(eventId),
        mealsApi.getSummary(eventId),
        mealsApi.getDietaryCounts(eventId)
      ])
      schedule = results[0].status === 'fulfilled' ? results[0].value : []
      summary = results[1].status === 'fulfilled' ? results[1].value : null
      dietaryCounts = results[2].status === 'fulfilled' ? results[2].value : {}
      if (results[0].status === 'rejected') {
        loadError = t('eventDetail.meals.error')
      }
    } finally {
      loading = false
    }
  }

  $effect(() => {
    void load()
  })

  function resetForm() {
    name = ''
    date = ''
    time = '19:00'
    servings = 6
    costEuros = 60
    location = ''
    notes = ''
  }

  async function handleCreate(e: SubmitEvent) {
    e.preventDefault()
    if (!name.trim() || !date || !time) return
    adding = true
    actionError = null
    try {
      const request: CreateMealRequest = {
        eventId,
        type,
        name: name.trim(),
        date,
        time,
        responsibleParticipantIds: [],
        estimatedCost: Math.round(costEuros * 100),
        servings: Math.max(1, servings),
        status: 'PLANNED' satisfies MealStatus
      }
      if (location.trim()) request.location = location.trim()
      if (notes.trim()) request.notes = notes.trim()
      await mealsApi.create(eventId, request)
      resetForm()
      showForm = false
      await load()
    } catch {
      actionError = t('eventDetail.meals.errors.create')
    } finally {
      adding = false
    }
  }

  async function handleDelete(mealId: string) {
    if (!window.confirm(t('eventDetail.meals.schedule.delete') + ' ?')) return
    actionError = null
    deletingMealId = mealId
    try {
      await mealsApi.remove(eventId, mealId)
      await load()
    } catch {
      actionError = t('eventDetail.meals.errors.delete')
    } finally {
      deletingMealId = null
    }
  }

  function formatCents(cents: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 2
    }).format(cents / 100)
  }
</script>

<div class="flex flex-col gap-4">
  {#if loadError}
    <ErrorBanner message={loadError} onretry={load} />
  {/if}

  {#if loading}
    <div class="flex flex-col gap-3" aria-busy="true">
      <SkeletonBlock height="h-20" rounded="rounded-card" />
      <SkeletonBlock height="h-40" rounded="rounded-card" />
    </div>
  {:else}
    {#if actionError}
      <ErrorBanner message={actionError} />
    {/if}

    {#if summary && summary.totalMeals > 0}
      <!-- Summary -->
      <section class="rounded-card border border-border bg-white p-4">
        <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide mb-3">
          {t('eventDetail.meals.summary.title')}
        </h2>
        <dl class="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div class="rounded-btn bg-gray-50 px-3 py-2">
            <dt class="text-xs text-gray-500">{t('eventDetail.meals.summary.totalMeals')}</dt>
            <dd class="text-lg font-semibold text-gray-900">{summary.totalMeals}</dd>
          </div>
          <div class="rounded-btn bg-gray-50 px-3 py-2">
            <dt class="text-xs text-gray-500">{t('eventDetail.meals.summary.completed')}</dt>
            <dd class="text-lg font-semibold text-green-700">{summary.mealsCompleted}</dd>
          </div>
          <div class="rounded-btn bg-gray-50 px-3 py-2">
            <dt class="text-xs text-gray-500">{t('eventDetail.meals.summary.remaining')}</dt>
            <dd class="text-lg font-semibold text-gray-900">{summary.mealsRemaining}</dd>
          </div>
          <div class="rounded-btn bg-gray-50 px-3 py-2">
            <dt class="text-xs text-gray-500">{t('eventDetail.meals.summary.estimatedCost')}</dt>
            <dd class="text-lg font-semibold text-gray-900">
              {formatCents(summary.totalEstimatedCost)}
            </dd>
          </div>
        </dl>
      </section>
    {/if}

    {#if Object.keys(dietaryCounts).length > 0}
      <!-- Dietary restrictions -->
      <section class="rounded-card border border-border bg-white p-4 flex flex-col gap-2">
        <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          {t('eventDetail.meals.restrictions.title')}
        </h2>
        <ul class="flex flex-wrap gap-2" role="list">
          {#each Object.entries(dietaryCounts) as [restriction, count] (restriction)}
            <li class="rounded-full bg-amber-50 border border-amber-200 px-3 py-1 text-xs text-amber-900">
              {t(`eventDetail.meals.restrictions.types.${restriction}`)} × {count}
            </li>
          {/each}
        </ul>
      </section>
    {/if}

    <!-- Add meal (organizer) -->
    {#if isOrganizer}
      <div class="flex items-center justify-between gap-3">
        <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          {t('eventDetail.meals.form.add')}
        </h2>
        <Button variant="secondary" size="sm" onclick={() => (showForm = !showForm)}>
          {showForm ? t('eventDetail.meals.form.cancel') : `+ ${t('eventDetail.meals.form.add')}`}
        </Button>
      </div>

      {#if showForm}
        <form
          onsubmit={handleCreate}
          class="flex flex-col gap-3 rounded-card border border-border bg-white p-4"
        >
          <div class="flex flex-col gap-3 sm:flex-row">
            <div class="w-full sm:w-48">
              <Select
                id="meal-type"
                label={t('eventDetail.meals.form.type')}
                value={type}
                options={typeOptions}
                onchange={(e) => (type = e.currentTarget.value as MealType)}
              />
            </div>
            <div class="flex-1">
              <Input
                id="meal-name"
                label={t('eventDetail.meals.form.name')}
                placeholder={t('eventDetail.meals.form.namePlaceholder')}
                required
                value={name}
                oninput={(e) => (name = e.currentTarget.value)}
              />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Input
              id="meal-date"
              type="date"
              label={t('eventDetail.meals.form.date')}
              required
              value={date}
              oninput={(e) => (date = e.currentTarget.value)}
            />
            <Input
              id="meal-time"
              type="time"
              label={t('eventDetail.meals.form.time')}
              required
              value={time}
              oninput={(e) => (time = e.currentTarget.value)}
            />
            <Input
              id="meal-servings"
              type="number"
              label={t('eventDetail.meals.form.servings')}
              value={String(servings)}
              oninput={(e) => (servings = Math.max(1, Number(e.currentTarget.value) || 1))}
            />
            <Input
              id="meal-cost"
              type="number"
              label={t('eventDetail.meals.form.cost')}
              value={String(costEuros)}
              oninput={(e) => (costEuros = Math.max(0, Number(e.currentTarget.value) || 0))}
            />
          </div>
          <Input
            id="meal-location"
            label={t('eventDetail.meals.form.location')}
            value={location}
            oninput={(e) => (location = e.currentTarget.value)}
          />
          <Textarea
            id="meal-notes"
            label={t('eventDetail.meals.form.notes')}
            rows={2}
            value={notes}
            oninput={(e) => (notes = e.currentTarget.value)}
          />
          <Button type="submit" loading={adding} disabled={!name.trim() || !date || !time}>
            {adding ? t('eventDetail.meals.form.adding') : t('eventDetail.meals.form.submit')}
          </Button>
        </form>
      {/if}
    {/if}

    <!-- Schedule -->
    <section class="flex flex-col gap-3">
      <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
        {t('eventDetail.meals.schedule.title')}
      </h2>

      {#if schedule.length === 0}
        <StatePanel
          tone="empty"
          title={t('eventDetail.meals.schedule.emptyTitle')}
          description={t('eventDetail.meals.schedule.emptyDesc')}
        />
      {:else}
        {#each schedule as day (day.date)}
          <div class="flex flex-col gap-2">
            <h3 class="text-sm font-semibold text-gray-800">{formatDate(day.date)}</h3>
            <ul class="flex flex-col gap-2" role="list">
              {#each day.meals as meal (meal.id)}
                <li
                  class="rounded-card border border-border bg-white px-4 py-3 flex flex-col gap-1
                    {meal.status === 'CANCELLED' ? 'opacity-60' : ''}"
                >
                  <div class="flex items-center justify-between gap-2 flex-wrap">
                    <span class="text-sm font-semibold text-gray-900">
                      {t(`eventDetail.meals.types.${meal.type}`)} · {meal.name}
                    </span>
                    <span class="flex items-center gap-2">
                      <span
                        class="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600"
                      >
                        {t(`eventDetail.meals.statuses.${meal.status}`)}
                      </span>
                      {#if isOrganizer}
                        <button
                          type="button"
                          class="rounded p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 transition-default
                            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-600"
                          aria-label="{t('eventDetail.meals.schedule.delete')} : {meal.name}"
                          disabled={deletingMealId === meal.id}
                          onclick={() => handleDelete(meal.id)}
                        >
                          🗑
                        </button>
                      {/if}
                    </span>
                  </div>
                  <p class="text-xs text-gray-500">
                    {meal.time} · {t('eventDetail.meals.schedule.servings', { n: meal.servings })}
                    {#if meal.location}
                      · 📍 {meal.location}
                    {/if}
                    · {formatCents(meal.estimatedCost)}
                  </p>
                  {#if meal.notes}
                    <p class="text-xs text-gray-500 whitespace-pre-wrap">{meal.notes}</p>
                  {/if}
                </li>
              {/each}
            </ul>
          </div>
        {/each}
      {/if}
    </section>
  {/if}
</div>
