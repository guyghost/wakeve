<script lang="ts">
  interface Props {
    yesCount: number
    maybeCount: number
    noCount: number
    totalParticipants: number
  }

  const { yesCount, maybeCount, noCount, totalParticipants }: Props = $props()

  const total = $derived(yesCount + maybeCount + noCount)

  const yesPct = $derived(total > 0 ? (yesCount / total) * 100 : 0)
  const maybePct = $derived(total > 0 ? (maybeCount / total) * 100 : 0)
  const noPct = $derived(total > 0 ? (noCount / total) * 100 : 0)

  const responseRate = $derived(
    totalParticipants > 0 ? Math.round((total / totalParticipants) * 100) : 0
  )
</script>

<div class="flex flex-col gap-1.5">
  <!-- Stacked bar -->
  <div
    class="flex h-2 w-full overflow-hidden rounded-full bg-gray-200"
    role="img"
    aria-label="Répartition des votes : {yesCount} oui, {maybeCount} peut-être, {noCount} non"
  >
    {#if total > 0}
      {#if yesPct > 0}
        <div
          class="h-full bg-green-500 transition-all duration-500"
          style="width: {yesPct}%"
        ></div>
      {/if}
      {#if maybePct > 0}
        <div
          class="h-full bg-amber-400 transition-all duration-500"
          style="width: {maybePct}%"
        ></div>
      {/if}
      {#if noPct > 0}
        <div
          class="h-full bg-red-400 transition-all duration-500"
          style="width: {noPct}%"
        ></div>
      {/if}
    {/if}
  </div>

  <!-- Count legend -->
  <div class="flex items-center justify-between text-xs text-gray-500">
    <span class="flex items-center gap-2">
      <span><span aria-hidden="true">✅</span> {yesCount}</span>
      <span><span aria-hidden="true">🤔</span> {maybeCount}</span>
      <span><span aria-hidden="true">❌</span> {noCount}</span>
    </span>
    {#if totalParticipants > 0}
      <span class="text-gray-400">{responseRate}% répondu</span>
    {/if}
  </div>
</div>
