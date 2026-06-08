<script lang="ts">
  import type { Comment } from '$lib/types/api'
  import Avatar from '$lib/components/atoms/Avatar.svelte'
  import { timeAgo } from '$lib/utils/date'

  interface Props {
    comment: Comment
    ondelete?: () => void
  }

  const { comment, ondelete }: Props = $props()

  const ago = $derived(timeAgo(comment.createdAt))
</script>

<div class="flex gap-3 group">
  <!-- Left: avatar -->
  <div class="shrink-0 pt-0.5">
    <Avatar name={comment.authorName} size="md" />
  </div>

  <!-- Right: content -->
  <div class="flex-1 min-w-0">
    <!-- Author + time + pinned badge -->
    <div class="flex items-center gap-2 flex-wrap mb-0.5">
      <span class="text-sm font-medium text-gray-900">{comment.authorName}</span>
      <span class="text-xs text-gray-400">{ago}</span>
      {#if comment.isPinned}
        <span
          class="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700"
          role="note"
        >
          📌 Épinglé
        </span>
      {/if}
    </div>

    <!-- Body -->
    <p class="text-sm text-gray-700 whitespace-pre-wrap break-words">
      {comment.content}
    </p>
  </div>

  <!-- Delete button -->
  {#if ondelete}
    <button
      type="button"
      onclick={ondelete}
      class="shrink-0 self-start opacity-0 group-hover:opacity-100 transition-default
        text-gray-400 hover:text-red-500 focus-visible:opacity-100
        focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-500 rounded"
      aria-label="Supprimer le commentaire de {comment.authorName}"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 20 20"
        fill="currentColor"
        class="h-4 w-4"
        aria-hidden="true"
      >
        <path
          fill-rule="evenodd"
          d="M8.75 1A2.75 2.75 0 0 0 6 3.75v.443c-.795.077-1.584.176-2.365.298a.75.75 0 1 0 .23 1.482l.149-.022.841 10.518A2.75 2.75 0 0 0 7.596 19h4.807a2.75 2.75 0 0 0 2.742-2.53l.841-10.52.149.023a.75.75 0 0 0 .23-1.482A41.03 41.03 0 0 0 14 4.193v-.443A2.75 2.75 0 0 0 11.25 1h-2.5ZM10 4c.84 0 1.673.025 2.5.075V3.75c0-.69-.56-1.25-1.25-1.25h-2.5c-.69 0-1.25.56-1.25 1.25v.325C8.327 4.025 9.16 4 10 4ZM8.58 7.72a.75.75 0 0 0-1.5.06l.3 7.5a.75.75 0 1 0 1.5-.06l-.3-7.5Zm4.34.06a.75.75 0 1 0-1.5-.06l-.3 7.5a.75.75 0 1 0 1.5.06l.3-7.5Z"
          clip-rule="evenodd"
        />
      </svg>
    </button>
  {/if}
</div>
