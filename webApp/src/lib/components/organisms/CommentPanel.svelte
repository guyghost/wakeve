<script lang="ts">
  import type { Comment, CommentSection } from '$lib/types/api'
  import CommentItem from '$lib/components/molecules/CommentItem.svelte'
  import Textarea from '$lib/components/atoms/Textarea.svelte'
  import Select from '$lib/components/atoms/Select.svelte'
  import Button from '$lib/components/atoms/Button.svelte'
  import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte'

  interface Props {
    comments: Comment[]
    currentUserId: string
    onaddcomment: (content: string, section: CommentSection) => void
    commenterror: string | null
    iscommenting: boolean
  }

  const { comments, currentUserId, onaddcomment, commenterror, iscommenting }: Props = $props()

  let content = $state('')
  let section = $state<CommentSection>('GENERAL')

  const sectionOptions: { value: CommentSection; label: string }[] = [
    { value: 'GENERAL', label: 'Général' },
    { value: 'LOGISTICS', label: 'Logistique' },
    { value: 'BUDGET', label: 'Budget' },
    { value: 'ACCOMMODATION', label: 'Hébergement' },
    { value: 'TRANSPORT', label: 'Transport' },
    { value: 'MEAL', label: 'Repas' },
    { value: 'ACTIVITY', label: 'Activité' },
    { value: 'EQUIPMENT', label: 'Équipement' },
    { value: 'OTHER', label: 'Autre' }
  ]

  // Pinned comments first, then chronological
  const sortedComments = $derived(
    [...comments].sort((a, b) => {
      if (a.isPinned && !b.isPinned) return -1
      if (!a.isPinned && b.isPinned) return 1
      return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    })
  )

  function handleContentInput(e: Event & { currentTarget: HTMLTextAreaElement }) {
    content = e.currentTarget.value
  }

  function handleSectionChange(e: Event & { currentTarget: HTMLSelectElement }) {
    section = e.currentTarget.value as CommentSection
  }

  function handleSubmit(e: SubmitEvent) {
    e.preventDefault()
    const trimmed = content.trim()
    if (!trimmed) return
    onaddcomment(trimmed, section)
    content = ''
  }
</script>

<div class="flex flex-col gap-6">
  <!-- Add comment form -->
  <form onsubmit={handleSubmit} class="flex flex-col gap-3 rounded-card border border-border bg-surface p-4">
    <h3 class="text-sm font-semibold text-gray-800">Ajouter un commentaire</h3>

    <Textarea
      id="comment-content"
      label=""
      value={content}
      placeholder="Votre commentaire…"
      rows={3}
      oninput={handleContentInput}
    />

    <div class="flex items-end gap-3">
      <div class="flex-1">
        <Select
          id="comment-section"
          label="Section"
          value={section}
          options={sectionOptions}
          onchange={handleSectionChange}
        />
      </div>
      <Button
        type="submit"
        variant="primary"
        size="md"
        disabled={!content.trim()}
        loading={iscommenting}
      >
        Envoyer
      </Button>
    </div>

    {#if commenterror}
      <ErrorBanner message={commenterror} />
    {/if}
  </form>

  <!-- Comment list -->
  {#if sortedComments.length === 0}
    <div class="flex flex-col items-center justify-center py-10 text-center gap-2">
      <span class="text-3xl" aria-hidden="true">💬</span>
      <p class="text-sm text-gray-500">Soyez le premier à commenter !</p>
    </div>
  {:else}
    <ul class="flex flex-col gap-5 divide-y divide-gray-100" role="list">
      {#each sortedComments as comment (comment.id)}
        <li class="pt-4 first:pt-0">
          <CommentItem
            {comment}
            ondelete={comment.authorId === currentUserId
              ? () => {/* parent handles via DELETE_COMMENT event */}
              : undefined}
          />
        </li>
      {/each}
    </ul>
  {/if}
</div>
