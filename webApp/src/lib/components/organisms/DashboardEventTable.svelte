<script lang="ts">
  import type { DashboardEventItem } from '$lib/types/api'
  import Badge from '$lib/components/atoms/Badge.svelte'
  import { EVENT_TYPE_ICONS, EVENT_TYPE_LABELS } from '$lib/utils/event-type'
  import { formatDate } from '$lib/utils/date'

  interface Props {
    events: DashboardEventItem[]
    onselectevent: (id: string) => void
  }

  const { events, onselectevent }: Props = $props()
</script>

{#if events.length === 0}
  <div class="flex flex-col items-center justify-center py-12 text-center gap-3">
    <span class="text-4xl" aria-hidden="true">📋</span>
    <p class="text-sm text-gray-500">Aucun événement à afficher.</p>
  </div>
{:else}
  <!-- Desktop table -->
  <div class="hidden sm:block overflow-x-auto rounded-card border border-border">
    <table class="w-full text-sm text-left">
      <thead class="bg-gray-50 border-b border-border">
        <tr>
          <th scope="col" class="px-4 py-3 font-semibold text-gray-700">Événement</th>
          <th scope="col" class="px-4 py-3 font-semibold text-gray-700">Statut</th>
          <th scope="col" class="px-4 py-3 font-semibold text-gray-700">Type</th>
          <th scope="col" class="px-4 py-3 font-semibold text-gray-700 text-right">Participants</th>
          <th scope="col" class="px-4 py-3 font-semibold text-gray-700 text-right">Votes</th>
          <th scope="col" class="px-4 py-3 font-semibold text-gray-700">Limite</th>
          <th scope="col" class="px-4 py-3 font-semibold text-gray-700">
            <span class="sr-only">Analytiques</span>
          </th>
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-100 bg-white">
        {#each events as event (event.id)}
          <tr class="hover:bg-gray-50 transition-default group">
            <td class="px-4 py-3">
              <a
                href="/events/{event.id}"
                class="font-medium text-gray-900 hover:text-wakeve-600 transition-default line-clamp-1
                  focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
              >
                {event.title}
              </a>
            </td>
            <td class="px-4 py-3">
              <Badge status={event.status} size="sm" />
            </td>
            <td class="px-4 py-3">
              <span class="flex items-center gap-1.5 text-gray-600">
                <span aria-hidden="true">{EVENT_TYPE_ICONS[event.type] ?? '📅'}</span>
                <span class="truncate max-w-[8rem]">{EVENT_TYPE_LABELS[event.type] ?? event.type}</span>
              </span>
            </td>
            <td class="px-4 py-3 text-right text-gray-700">{event.participantCount}</td>
            <td class="px-4 py-3 text-right text-gray-700">{event.voteCount}</td>
            <td class="px-4 py-3 text-gray-500 text-xs">
              {event.deadline ? formatDate(event.deadline) : '—'}
            </td>
            <td class="px-4 py-3 text-right">
              <button
                type="button"
                onclick={() => onselectevent(event.id)}
                class="text-xs font-medium text-wakeve-600 hover:text-wakeve-800 transition-default
                  focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded px-1"
              >
                Analytiques
              </button>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>

  <!-- Mobile cards -->
  <ul class="sm:hidden flex flex-col gap-3" role="list">
    {#each events as event (event.id)}
      <li class="bg-white rounded-card shadow-card border border-border p-4 flex flex-col gap-2">
        <div class="flex items-start justify-between gap-2">
          <a
            href="/events/{event.id}"
            class="font-semibold text-gray-900 text-sm hover:text-wakeve-600 transition-default line-clamp-1
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
          >
            {event.title}
          </a>
          <Badge status={event.status} size="sm" />
        </div>

        <div class="flex items-center gap-1.5 text-xs text-gray-500">
          <span aria-hidden="true">{EVENT_TYPE_ICONS[event.type] ?? '📅'}</span>
          <span>{EVENT_TYPE_LABELS[event.type] ?? event.type}</span>
        </div>

        <div class="flex items-center justify-between text-xs text-gray-500 pt-1 border-t border-gray-100">
          <span>👥 {event.participantCount} · 🗳️ {event.voteCount}</span>
          {#if event.deadline}
            <span>Limite : {formatDate(event.deadline)}</span>
          {/if}
        </div>

        <button
          type="button"
          onclick={() => onselectevent(event.id)}
          class="self-start text-xs font-medium text-wakeve-600 hover:text-wakeve-800 transition-default
            focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
        >
          Voir les analytiques →
        </button>
      </li>
    {/each}
  </ul>
{/if}
