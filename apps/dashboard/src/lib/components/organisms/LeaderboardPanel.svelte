<script lang="ts">
  import { t } from '$lib/i18n'
  import { useAuth } from '$lib/actors/auth.actor.svelte'
  import { getLeaderboard, getUserBadges, getUserPoints } from '$lib/api/gamification.api'
  import type { Badge, LeaderboardEntry, UserPointsResponse } from '$lib/types/api'
  import Avatar from '$lib/components/atoms/Avatar.svelte'
  import ProgressBar from '$lib/components/atoms/ProgressBar.svelte'
  import SkeletonBlock from '$lib/components/ui/SkeletonBlock.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'

  interface Props {
    limit?: number
  }

  const { limit = 10 }: Props = $props()

  const { snapshot: authSnapshot } = useAuth()
  const currentUserId = $derived(authSnapshot.context.user?.id ?? '')

  let loading = $state(true)
  let error = $state(false)
  let entries = $state<LeaderboardEntry[]>([])
  let points = $state<UserPointsResponse | null>(null)
  let badges = $state<Badge[]>([])

  async function load() {
    if (!currentUserId) return
    loading = true
    error = false
    try {
      const [leaderboardRes, pointsRes, badgesRes] = await Promise.all([
        getLeaderboard({ limit, type: 'ALL_TIME' }),
        getUserPoints(currentUserId),
        getUserBadges(currentUserId)
      ])
      entries = leaderboardRes.leaderboard
      points = pointsRes
      badges = badgesRes.badges
    } catch {
      error = true
    } finally {
      loading = false
    }
  }

  $effect(() => {
    if (currentUserId) void load()
  })

  const currentUserEntry = $derived(
    entries.find((e) => e.isCurrentUser || e.userId === currentUserId) ?? null
  )
  const unlockedBadges = $derived(badges.filter((b) => b.unlockedAt))
  const levelProgressPct = $derived(
    points ? Math.round(Math.min(1, Math.max(0, points.progressToNextLevel)) * 100) : 0
  )

  const RARITY_STYLES: Record<Badge['rarity'], string> = {
    COMMON: 'bg-gray-100 text-gray-600',
    RARE: 'bg-sky-100 text-sky-700',
    EPIC: 'bg-violet-100 text-violet-700',
    LEGENDARY: 'bg-amber-100 text-amber-700'
  }

  function rankStyle(rank: number): string {
    if (rank === 1) return 'bg-amber-100 text-amber-700'
    if (rank === 2) return 'bg-gray-100 text-gray-600'
    if (rank === 3) return 'bg-orange-100 text-orange-700'
    return 'bg-gray-50 text-gray-500'
  }
</script>

<section aria-label={t('leaderboard.title')} class="rounded-card border border-border bg-surface p-4 shadow-card sm:p-6">
  <div class="flex flex-col gap-1 mb-4">
    <h2 class="text-lg font-semibold text-gray-900">🏆 {t('leaderboard.title')}</h2>
    <p class="text-sm text-gray-500">{t('leaderboard.subtitle')}</p>
  </div>

  {#if loading}
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div class="lg:col-span-2 flex flex-col gap-2">
        {#each { length: 5 } as _, i (i)}
          <div class="flex items-center gap-3 rounded-btn border border-border p-3">
            <SkeletonBlock height="h-6" width="w-8" rounded="rounded-full" />
            <SkeletonBlock height="h-8" width="h-8" rounded="rounded-full" />
            <SkeletonBlock height="h-3" rounded="rounded" />
          </div>
        {/each}
      </div>
      <div class="rounded-btn border border-border p-4 flex flex-col gap-3">
        <SkeletonBlock height="h-4" width="w-1/2" rounded="rounded" />
        <SkeletonBlock height="h-3" rounded="rounded" />
        <SkeletonBlock height="h-2" rounded="rounded-full" />
        <SkeletonBlock height="h-3" width="w-2/3" rounded="rounded" />
      </div>
    </div>

  {:else if error}
    <ErrorBanner message={t('leaderboard.error')} onretry={() => void load()} />

  {:else if entries.length === 0}
    <div class="flex flex-col items-center justify-center py-10 text-center gap-2">
      <span class="text-3xl" aria-hidden="true">🏅</span>
      <p class="text-sm text-gray-500">{t('leaderboard.empty')}</p>
    </div>

  {:else}
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <!-- Rankings -->
      <ol class="lg:col-span-2 flex flex-col gap-2" role="list">
        {#each entries as entry (entry.rank)}
          <li
            class="flex items-center gap-3 rounded-btn border p-3
              {entry.isCurrentUser || entry.userId === currentUserId
                ? 'border-wakeve-300 bg-wakeve-50'
                : 'border-border'}"
          >
            <span
              class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold {rankStyle(entry.rank)}"
              aria-label={t('leaderboard.rank', { rank: entry.rank })}
            >
              {entry.rank}
            </span>
            <Avatar name={entry.username} size="sm" />
            <span class="flex-1 min-w-0 truncate text-sm font-medium text-gray-900">
              {entry.username}
              {#if entry.isCurrentUser || entry.userId === currentUserId}
                <span class="ml-1 text-xs font-semibold text-wakeve-600">({t('leaderboard.you')})</span>
              {/if}
            </span>
            <span class="shrink-0 text-xs text-gray-400" title={t('leaderboard.badges')}>
              🎖️ {entry.badgesCount}
            </span>
            <span class="shrink-0 text-sm font-semibold text-gray-800 tabular-nums">
              {entry.totalPoints} {t('leaderboard.pointsShort')}
            </span>
          </li>
        {/each}
      </ol>

      <!-- Current user stats -->
      <aside class="rounded-btn border border-border p-4 flex flex-col gap-4 self-start" aria-label={t('leaderboard.yourStats')}>
        <h3 class="text-sm font-semibold text-gray-800">{t('leaderboard.yourStats')}</h3>

        {#if points}
          <div class="flex items-baseline gap-2">
            <span class="text-3xl font-bold text-wakeve-600 tabular-nums">{points.totalPoints}</span>
            <span class="text-sm text-gray-500">{t('leaderboard.points')}</span>
          </div>

          {#if currentUserEntry}
            <p class="text-sm text-gray-600">
              {t('leaderboard.yourRank', { rank: currentUserEntry.rank })}
            </p>
          {/if}

          <div class="flex flex-col gap-1">
            <div class="flex justify-between text-xs text-gray-500">
              <span>{t('leaderboard.level', { level: points.level })} — {points.levelName}</span>
            </div>
            <ProgressBar value={levelProgressPct} label={t('leaderboard.nextLevel', { points: points.pointsForNextLevel })} />
          </div>
        {/if}

        <div class="flex flex-col gap-2">
          <h4 class="text-xs font-semibold text-gray-500 uppercase tracking-wide">{t('leaderboard.yourBadges')}</h4>
          {#if unlockedBadges.length === 0}
            <p class="text-xs text-gray-400">{t('leaderboard.noBadges')}</p>
          {:else}
            <ul class="flex flex-wrap gap-2" role="list">
              {#each unlockedBadges as badge (badge.id)}
                <li
                  class="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium {RARITY_STYLES[badge.rarity]}"
                  title={badge.description}
                >
                  <span aria-hidden="true">{badge.icon}</span>
                  {badge.name}
                </li>
              {/each}
            </ul>
          {/if}
        </div>
      </aside>
    </div>
  {/if}
</section>
