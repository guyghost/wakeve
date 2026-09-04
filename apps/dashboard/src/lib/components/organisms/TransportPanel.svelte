<script lang="ts">
  import type { EventStatus, OptimizationType, TransportPlan, TransportReadiness } from '$lib/types/api'
  import * as transportApi from '$lib/api/transport.api'
  import { ApiError } from '$lib/api/client'
  import { t } from '$lib/i18n'
  import Button from '$lib/components/atoms/Button.svelte'
  import Input from '$lib/components/atoms/Input.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import StatePanel from '$lib/components/ui/StatePanel.svelte'

  interface Props {
    eventId: string
    eventStatus: EventStatus
    currentUserId: string
    isOrganizer: boolean
  }

  const { eventId, eventStatus, currentUserId, isOrganizer }: Props = $props()

  // Business rule: transport logistics require a confirmed event date
  // (CONFIRMED / COMPARING / ORGANIZING — read access, mutations for FINALIZED are refused).
  const unlocked = $derived(
    eventStatus === 'CONFIRMED' || eventStatus === 'COMPARING' || eventStatus === 'ORGANIZING'
  )

  let loading = $state(true)
  let loadError = $state<string | null>(null)
  let noDestination = $state(false)
  let readiness = $state<TransportReadiness | null>(null)
  let planIds = $state<string[]>([])
  let plansById = $state<Record<string, TransportPlan>>({})
  let selectedPlanId = $state<string | null>(null)
  let transportNotNeeded = $state(false)

  let generating = $state(false)
  let optimizationType = $state<OptimizationType>('BALANCED')
  let selectingPlanId = $state<string | null>(null)
  let deletingPlanId = $state<string | null>(null)
  let markingNotNeeded = $state(false)
  let actionError = $state<string | null>(null)

  let departureName = $state('')
  let departureAddress = $state('')
  let savingDeparture = $state(false)
  let departureSaved = $state(false)

  const optimizationOptions: { value: OptimizationType; label: string }[] = [
    { value: 'BALANCED', label: t('eventDetail.transport.plans.optimizations.BALANCED') },
    { value: 'COST_MINIMIZE', label: t('eventDetail.transport.plans.optimizations.COST_MINIMIZE') },
    { value: 'TIME_MINIMIZE', label: t('eventDetail.transport.plans.optimizations.TIME_MINIMIZE') }
  ]

  async function load() {
    loading = true
    loadError = null
    noDestination = false
    try {
      const results = await Promise.allSettled([
        transportApi.getReadiness(eventId),
        transportApi.listPlans(eventId)
      ])
      if (results[0].status === 'fulfilled') {
        readiness = results[0].value
        transportNotNeeded = results[0].value.transportNotNeeded
      } else {
        readiness = null
        // 409 = no selected destination yet
        noDestination =
          results[0].reason instanceof ApiError && results[0].reason.status === 409
      }
      if (results[1].status === 'fulfilled') {
        plansById = Object.fromEntries(results[1].value.plans.map((p) => [p.id, p]))
        planIds = results[1].value.plans.map((p) => p.id)
      } else {
        planIds = []
        plansById = {}
      }
      if ((results[0].status === 'rejected' && !noDestination) || results[1].status === 'rejected') {
        loadError = t('eventDetail.transport.error')
      }
    } finally {
      loading = false
    }
  }

  $effect(() => {
    if (!unlocked) return
    void load()
  })

  async function handleGenerate() {
    actionError = null
    generating = true
    try {
      await transportApi.generatePlan(eventId, { optimizationType })
      await load()
    } catch {
      actionError = t('eventDetail.transport.errors.generate')
    } finally {
      generating = false
    }
  }

  async function handleSelect(planId: string) {
    actionError = null
    selectingPlanId = planId
    try {
      const summary = await transportApi.selectPlan(eventId, planId)
      selectedPlanId = summary.planId
      readiness = summary.readiness
      transportNotNeeded = summary.readiness.transportNotNeeded
    } catch {
      actionError = t('eventDetail.transport.errors.select')
    } finally {
      selectingPlanId = null
    }
  }

  async function handleDelete(planId: string) {
    if (!window.confirm(t('eventDetail.transport.plans.delete') + ' ?')) return
    actionError = null
    deletingPlanId = planId
    try {
      await transportApi.deletePlan(eventId, planId)
      await load()
    } catch {
      actionError = t('eventDetail.transport.errors.delete')
    } finally {
      deletingPlanId = null
    }
  }

  async function handleNotNeeded() {
    actionError = null
    markingNotNeeded = true
    try {
      await transportApi.markNotNeeded(eventId)
      transportNotNeeded = true
    } catch {
      actionError = t('eventDetail.transport.errors.notNeeded')
    } finally {
      markingNotNeeded = false
    }
  }

  async function handleSaveDeparture(e: SubmitEvent) {
    e.preventDefault()
    if (!departureName.trim()) return
    actionError = null
    departureSaved = false
    savingDeparture = true
    try {
      await transportApi.saveDeparture(eventId, currentUserId, {
        name: departureName.trim(),
        address: departureAddress.trim() || undefined
      })
      departureSaved = true
      await load()
    } catch {
      actionError = t('eventDetail.transport.departure.error')
    } finally {
      savingDeparture = false
    }
  }

  function formatMoney(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0
    }).format(amount)
  }

  function formatDuration(minutes: number): string {
    if (minutes < 60) return `${minutes} min`
    const h = Math.floor(minutes / 60)
    const m = minutes % 60
    return m > 0 ? `${h} h ${m}` : `${h} h`
  }
</script>

{#if !unlocked}
  <StatePanel
    tone="info"
    title={t('eventDetail.transport.lockedTitle')}
    description={t('eventDetail.transport.lockedDesc')}
  />
{:else}
  <div class="flex flex-col gap-4">
    {#if loadError}
      <ErrorBanner message={loadError} onretry={load} />
    {:else if loading}
      <div class="flex flex-col gap-3" aria-busy="true">
        <SkeletonBlock height="h-24" rounded="rounded-card" />
        <SkeletonBlock height="h-40" rounded="rounded-card" />
      </div>
    {:else if noDestination || !readiness}
      <StatePanel
        tone="info"
        title={t('eventDetail.transport.noDestinationTitle')}
        description={t('eventDetail.transport.noDestinationDesc')}
      />
    {:else}
      {#if actionError}
        <ErrorBanner message={actionError} />
      {/if}

      <!-- Readiness card -->
      <section class="rounded-card border border-border bg-white p-4 flex flex-col gap-3">
        <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          {t('eventDetail.transport.readiness.title')}
        </h2>
        <dl class="flex flex-col gap-2 text-sm">
          <div class="flex items-center justify-between gap-3">
            <dt class="text-gray-500">{t('eventDetail.transport.readiness.destination')}</dt>
            <dd class="font-medium text-gray-800">
              <span aria-hidden="true">📍</span> {readiness.destination.name}
            </dd>
          </div>
          <div class="flex items-center justify-between gap-3">
            <dt class="text-gray-500">{t('eventDetail.transport.readiness.missing')}</dt>
            <dd class="text-right">
              {#if readiness.missingDepartureParticipantNames.length === 0}
                <span class="text-green-700">
                  ✓ {t('eventDetail.transport.readiness.allDepartures')}
                </span>
              {:else}
                <span class="text-amber-700">
                  {readiness.missingDepartureParticipantNames.join(', ')}
                </span>
              {/if}
            </dd>
          </div>
          {#if transportNotNeeded}
            <div class="flex items-center justify-between gap-3">
              <dt class="text-gray-500">{t('eventDetail.transport.markNotNeeded')}</dt>
              <dd class="font-medium text-green-700">✓</dd>
            </div>
          {/if}
        </dl>
      </section>

      <!-- Departure form -->
      <form
        onsubmit={handleSaveDeparture}
        class="flex flex-col gap-3 rounded-card border border-border bg-white p-4"
      >
        <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          {t('eventDetail.transport.departure.title')}
        </h2>
        <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div class="flex-1">
            <Input
              id="departure-name"
              label={t('eventDetail.transport.departure.name')}
              placeholder={t('eventDetail.transport.departure.namePlaceholder')}
              required
              value={departureName}
              oninput={(e) => (departureName = e.currentTarget.value)}
            />
          </div>
          <div class="flex-1">
            <Input
              id="departure-address"
              label={t('eventDetail.transport.departure.address')}
              placeholder={t('eventDetail.transport.departure.addressPlaceholder')}
              value={departureAddress}
              oninput={(e) => (departureAddress = e.currentTarget.value)}
            />
          </div>
          <Button type="submit" loading={savingDeparture} disabled={!departureName.trim()}>
            {t('eventDetail.transport.departure.save')}
          </Button>
        </div>
        {#if departureSaved}
          <p class="text-xs text-green-700" role="status">
            ✓ {t('eventDetail.transport.departure.saved')}
          </p>
        {/if}
      </form>

      <!-- Plans -->
      <section class="flex flex-col gap-3">
        <div class="flex items-center justify-between gap-3 flex-wrap">
          <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
            {t('eventDetail.transport.plans.title')}
          </h2>
          {#if isOrganizer && !transportNotNeeded}
            <div class="flex items-end gap-2">
              <Select
                id="transport-optimization"
                label={t('eventDetail.transport.plans.optimization')}
                value={optimizationType}
                options={optimizationOptions}
                onchange={(e) => (optimizationType = e.currentTarget.value as OptimizationType)}
              />
              <Button
                size="sm"
                loading={generating}
                disabled={generating || !readiness.canGeneratePlan}
                onclick={handleGenerate}
              >
                {generating
                  ? t('eventDetail.transport.plans.generating')
                  : t('eventDetail.transport.plans.generate')}
              </Button>
            </div>
          {/if}
        </div>

        {#if transportNotNeeded}
          <StatePanel
            tone="success"
            title={t('eventDetail.transport.markNotNeeded')}
            description={t('eventDetail.transport.readiness.notNeeded')}
          />
        {:else if planIds.length === 0}
          <StatePanel
            tone="empty"
            title={t('eventDetail.transport.plans.empty')}
            description={
              readiness.canGeneratePlan
                ? t('eventDetail.transport.readiness.ready')
                : t('eventDetail.transport.noDestinationDesc')
            }
          />
        {:else}
          <ul class="flex flex-col gap-4" role="list">
            {#each planIds as planId (planId)}
              {@const plan = plansById[planId]}
              {@const routeEntries = Object.entries(plan.participantRoutes)}
              <li
                class="rounded-card border bg-white p-4 flex flex-col gap-3
                  {selectedPlanId === plan.id
                    ? 'border-wakeve-400 ring-1 ring-wakeve-200'
                    : 'border-border'}"
              >
                <div class="flex items-start justify-between gap-3 flex-wrap">
                  <div class="flex flex-col gap-0.5">
                    <span class="text-sm font-semibold text-gray-900">
                      {t(`eventDetail.transport.plans.optimizations.${plan.optimizationType}`)}
                      {#if selectedPlanId === plan.id}
                        <span
                          class="ml-1 rounded-full bg-wakeve-100 px-2 py-0.5 text-xs font-medium text-wakeve-700"
                        >
                          ✓ {t('eventDetail.transport.plans.selected')}
                        </span>
                      {/if}
                    </span>
                    <span class="text-xs text-gray-500">
                      {routeEntries.length}
                      {t('eventDetail.transport.plans.routes').toLowerCase()}
                    </span>
                  </div>
                  <span class="text-sm font-semibold text-gray-800">
                    {t('eventDetail.transport.plans.groupCost')}:
                    {formatMoney(plan.totalGroupCost)}
                  </span>
                </div>

                {#if routeEntries.length > 0}
                  <ul class="flex flex-col gap-2" role="list">
                    {#each routeEntries as [participantId, route] (participantId)}
                      <li class="rounded-btn bg-gray-50 px-3 py-2 text-xs text-gray-700">
                        <div class="flex items-center justify-between gap-2 flex-wrap">
                          <span class="font-medium">👤 {participantId}</span>
                          <span>
                            {formatMoney(route.totalCost)} ·
                            {formatDuration(route.totalDurationMinutes)} ·
                            {route.segments.length} {t('eventDetail.transport.segment')}
                          </span>
                        </div>
                        {#each route.segments as segment (segment.id)}
                          <p class="mt-1 text-gray-500">
                            🚆 {segment.provider} · {segment.departure.name} → {segment.arrival.name}
                          </p>
                        {/each}
                      </li>
                    {/each}
                  </ul>
                {/if}

                {#if isOrganizer}
                  <div class="flex items-center gap-2">
                    {#if selectedPlanId !== plan.id}
                      <Button
                        variant="secondary"
                        size="sm"
                        loading={selectingPlanId === plan.id}
                        onclick={() => handleSelect(plan.id)}
                      >
                        {t('eventDetail.transport.plans.select')}
                      </Button>
                    {/if}
                    <Button
                      variant="ghost"
                      size="sm"
                      loading={deletingPlanId === plan.id}
                      onclick={() => handleDelete(plan.id)}
                    >
                      {t('eventDetail.transport.plans.delete')}
                    </Button>
                  </div>
                {/if}
              </li>
            {/each}
          </ul>
        {/if}

        {#if isOrganizer && !transportNotNeeded}
          <div>
            <Button
              variant="ghost"
              size="sm"
              loading={markingNotNeeded}
              onclick={handleNotNeeded}
            >
              {markingNotNeeded
                ? t('eventDetail.transport.markingNotNeeded')
                : `✓ ${t('eventDetail.transport.markNotNeeded')}`}
            </Button>
          </div>
        {/if}
      </section>
    {/if}
  </div>
{/if}
