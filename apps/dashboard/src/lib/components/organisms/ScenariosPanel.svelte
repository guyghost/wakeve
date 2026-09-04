<script lang="ts">
  import type { EventStatus, ScenarioVoteType, ScenarioWithVotesResponse } from '$lib/types/api'
  import * as scenariosApi from '$lib/api/scenarios.api'
  import { t } from '$lib/i18n'
  import Button from '$lib/components/atoms/Button.svelte'
  import Input from '$lib/components/atoms/Input.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import Textarea from '$lib/components/atoms/Textarea.svelte'
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

  // Business rule: scenarios are only allowed for CONFIRMED or COMPARING events.
  const unlocked = $derived(eventStatus === 'CONFIRMED' || eventStatus === 'COMPARING')

  let loading = $state(true)
  let loadError = $state<string | null>(null)
  let scenarios = $state<ScenarioWithVotesResponse[]>([])
  let actionError = $state<string | null>(null)
  let votingScenarioId = $state<string | null>(null)
  let selectingScenarioId = $state<string | null>(null)

  let showForm = $state(false)
  let creating = $state(false)
  let name = $state('')
  let dateOrPeriod = $state('')
  let location = $state('')
  let duration = $state(2)
  let estimatedParticipants = $state(6)
  let estimatedBudgetPerPerson = $state(120)
  let description = $state('')

  const voteOptions: { value: ScenarioVoteType; label: string }[] = [
    { value: 'PREFER', label: t('eventDetail.scenarios.vote.prefer') },
    { value: 'NEUTRAL', label: t('eventDetail.scenarios.vote.neutral') },
    { value: 'AGAINST', label: t('eventDetail.scenarios.vote.against') }
  ]

  async function load() {
    loading = true
    loadError = null
    try {
      const response = await scenariosApi.listWithVotes(eventId)
      scenarios = response.scenarios
    } catch {
      loadError = t('eventDetail.scenarios.error')
    } finally {
      loading = false
    }
  }

  $effect(() => {
    if (!unlocked) return
    void load()
  })

  function getMyVote(item: ScenarioWithVotesResponse): ScenarioVoteType | null {
    return item.votes.find((v) => v.participantId === currentUserId)?.vote ?? null
  }

  async function handleVote(scenarioId: string, value: ScenarioVoteType) {
    actionError = null
    votingScenarioId = scenarioId
    try {
      await scenariosApi.vote(eventId, scenarioId, { participantId: currentUserId, vote: value })
      await load()
    } catch {
      actionError = t('eventDetail.scenarios.vote.error')
    } finally {
      votingScenarioId = null
    }
  }

  async function handleSelectFinal(scenarioId: string) {
    actionError = null
    selectingScenarioId = scenarioId
    try {
      await scenariosApi.selectFinal(eventId, scenarioId)
      await load()
    } catch {
      actionError = t('eventDetail.scenarios.deleteError')
    } finally {
      selectingScenarioId = null
    }
  }

  function resetForm() {
    name = ''
    dateOrPeriod = ''
    location = ''
    duration = 2
    estimatedParticipants = 6
    estimatedBudgetPerPerson = 120
    description = ''
  }

  async function handleCreate(e: SubmitEvent) {
    e.preventDefault()
    if (!name.trim() || !dateOrPeriod.trim() || !location.trim()) return
    creating = true
    actionError = null
    try {
      await scenariosApi.create(eventId, {
        eventId,
        name: name.trim(),
        dateOrPeriod: dateOrPeriod.trim(),
        location: location.trim(),
        duration,
        estimatedParticipants,
        estimatedBudgetPerPerson,
        description: description.trim()
      })
      resetForm()
      showForm = false
      await load()
    } catch {
      actionError = t('eventDetail.scenarios.error')
    } finally {
      creating = false
    }
  }

  function formatMoney(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0
    }).format(amount)
  }
</script>

{#if !unlocked}
  <StatePanel
    tone="info"
    title={t('eventDetail.scenarios.lockedTitle')}
    description={t('eventDetail.scenarios.lockedDesc')}
  />
{:else}
  <div class="flex flex-col gap-4">
    {#if loadError}
      <ErrorBanner message={loadError} onretry={load} />
    {/if}

    {#if isOrganizer}
      <div class="flex items-center justify-between gap-3">
        <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          {t('eventDetail.scenarios.create')}
        </h2>
        <Button variant="secondary" size="sm" onclick={() => (showForm = !showForm)}>
          {showForm ? t('eventDetail.scenarios.cancel') : `+ ${t('eventDetail.scenarios.create')}`}
        </Button>
      </div>

      {#if showForm}
        <form
          onsubmit={handleCreate}
          class="flex flex-col gap-3 rounded-card border border-border bg-white p-4"
        >
          <Input
            id="scenario-name"
            label={t('eventDetail.scenarios.form.name')}
            placeholder={t('eventDetail.scenarios.form.namePlaceholder')}
            required
            value={name}
            oninput={(e) => (name = e.currentTarget.value)}
          />
          <Input
            id="scenario-period"
            label={t('eventDetail.scenarios.form.dateOrPeriod')}
            placeholder={t('eventDetail.scenarios.form.dateOrPeriodPlaceholder')}
            required
            value={dateOrPeriod}
            oninput={(e) => (dateOrPeriod = e.currentTarget.value)}
          />
          <Input
            id="scenario-location"
            label={t('eventDetail.scenarios.form.location')}
            placeholder={t('eventDetail.scenarios.form.locationPlaceholder')}
            required
            value={location}
            oninput={(e) => (location = e.currentTarget.value)}
          />
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <Input
              id="scenario-duration"
              type="number"
              label={t('eventDetail.scenarios.form.duration')}
              value={String(duration)}
              oninput={(e) => (duration = Math.max(1, Number(e.currentTarget.value) || 1))}
            />
            <Input
              id="scenario-participants"
              type="number"
              label={t('eventDetail.scenarios.form.participants')}
              value={String(estimatedParticipants)}
              oninput={(e) =>
                (estimatedParticipants = Math.max(1, Number(e.currentTarget.value) || 1))}
            />
            <Input
              id="scenario-budget"
              type="number"
              label={t('eventDetail.scenarios.form.budget')}
              value={String(estimatedBudgetPerPerson)}
              oninput={(e) =>
                (estimatedBudgetPerPerson = Math.max(0, Number(e.currentTarget.value) || 0))}
            />
          </div>
          <Textarea
            id="scenario-description"
            label={t('eventDetail.scenarios.form.description')}
            placeholder={t('eventDetail.scenarios.form.descriptionPlaceholder')}
            rows={3}
            value={description}
            oninput={(e) => (description = e.currentTarget.value)}
          />
          <Button type="submit" loading={creating} disabled={!name.trim() || !dateOrPeriod.trim() || !location.trim()}>
            {t('eventDetail.scenarios.form.submit')}
          </Button>
        </form>
      {/if}
    {/if}

    {#if actionError}
      <ErrorBanner message={actionError} />
    {/if}

    {#if loading}
      <div class="flex flex-col gap-3" aria-busy="true">
        {#each { length: 2 } as _, i (i)}
          <SkeletonBlock height="h-40" rounded="rounded-card" />
        {/each}
      </div>
    {:else if scenarios.length === 0}
      <StatePanel
        tone="empty"
        title={t('eventDetail.scenarios.emptyTitle')}
        description={t('eventDetail.scenarios.emptyDesc')}
      />
    {:else}
      <ul class="flex flex-col gap-4" role="list">
        {#each scenarios as item (item.scenario.id)}
          {@const s = item.scenario}
          {@const myVote = getMyVote(item)}
          <li
            class="rounded-card border bg-white p-4 flex flex-col gap-3
              {s.status === 'SELECTED' ? 'border-wakeve-400 ring-1 ring-wakeve-200' : 'border-border'}"
          >
            <!-- Header -->
            <div class="flex items-start justify-between gap-3 flex-wrap">
              <div class="flex flex-col gap-0.5 min-w-0">
                <span class="text-sm font-semibold text-gray-900 flex items-center gap-2">
                  <span aria-hidden="true">📍</span> {s.location}
                  {#if s.status === 'SELECTED'}
                    <span
                      class="rounded-full bg-wakeve-100 px-2 py-0.5 text-xs font-medium text-wakeve-700"
                    >
                      ★ {t('eventDetail.scenarios.selected')}
                    </span>
                  {/if}
                </span>
                <span class="text-xs text-gray-500">{s.name} · {s.dateOrPeriod}</span>
              </div>
              <span
                class="shrink-0 rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600"
              >
                {t(`eventDetail.scenarios.statuses.${s.status}`)}
              </span>
            </div>

            <!-- Key figures -->
            <dl class="flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-600">
              <div class="flex items-center gap-1">
                <dt class="text-gray-400">{t('eventDetail.scenarios.form.duration')}:</dt>
                <dd>{s.duration} {t('eventDetail.scenarios.durationDays')}</dd>
              </div>
              <div class="flex items-center gap-1">
                <dt class="text-gray-400">{t('eventDetail.scenarios.form.participants')}:</dt>
                <dd>{s.estimatedParticipants}</dd>
              </div>
              <div class="flex items-center gap-1">
                <dt class="text-gray-400">{t('eventDetail.scenarios.form.budget')}:</dt>
                <dd>{formatMoney(s.estimatedBudgetPerPerson)} {t('eventDetail.scenarios.budgetPerPerson')}</dd>
              </div>
            </dl>

            {#if s.description}
              <p class="text-sm text-gray-700 whitespace-pre-wrap">{s.description}</p>
            {/if}

            <!-- Votes -->
            <div class="flex flex-col gap-2">
              <div
                class="flex h-2 w-full overflow-hidden rounded-full bg-gray-200"
                role="img"
                aria-label="{item.result.preferCount} {t('eventDetail.scenarios.vote.prefer')}, {item.result.neutralCount} {t('eventDetail.scenarios.vote.neutral')}, {item.result.againstCount} {t('eventDetail.scenarios.vote.against')}"
              >
                {#if item.result.totalVotes > 0}
                  <div class="h-full bg-green-500" style="width: {item.result.preferPercentage}%"></div>
                  <div class="h-full bg-amber-400" style="width: {item.result.neutralPercentage}%"></div>
                  <div class="h-full bg-red-400" style="width: {item.result.againstPercentage}%"></div>
                {/if}
              </div>
              <div class="flex items-center justify-between text-xs text-gray-500">
                <span class="flex items-center gap-2">
                  <span><span aria-hidden="true">👍</span> {item.result.preferCount}</span>
                  <span><span aria-hidden="true">🤔</span> {item.result.neutralCount}</span>
                  <span><span aria-hidden="true">👎</span> {item.result.againstCount}</span>
                </span>
                <span class="text-gray-400">
                  {item.result.totalVotes} {t('eventDetail.scenarios.votes')}
                </span>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex items-center gap-2 flex-wrap">
              <div
                class="flex rounded-btn border border-border overflow-hidden"
                role="group"
                aria-label={t('eventDetail.scenarios.vote.yourVote')}
              >
                {#each voteOptions as option (option.value)}
                  <button
                    type="button"
                    disabled={votingScenarioId === s.id}
                    onclick={() => handleVote(s.id, option.value)}
                    class="px-3 py-1.5 text-xs font-medium transition-default
                      focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
                      {myVote === option.value
                        ? 'bg-wakeve-600 text-white'
                        : 'bg-white text-gray-600 hover:bg-gray-50'}
                      {option.value !== 'AGAINST' ? 'border-r border-border' : ''}"
                    aria-pressed={myVote === option.value}
                  >
                    {option.label}
                  </button>
                {/each}
              </div>

              {#if isOrganizer && s.status !== 'SELECTED'}
                <Button
                  variant="secondary"
                  size="sm"
                  loading={selectingScenarioId === s.id}
                  onclick={() => handleSelectFinal(s.id)}
                >
                  {selectingScenarioId === s.id
                    ? t('eventDetail.scenarios.selectingFinal')
                    : `★ ${t('eventDetail.scenarios.selectFinal')}`}
                </Button>
              {/if}
            </div>
          </li>
        {/each}
      </ul>
    {/if}
  </div>
{/if}
