import { setup, assign, fromPromise } from 'xstate'
import type { EventResponse, PollResponse, Comment, VoteValue, CommentSection } from '$lib/types/api'
import { get as getEvent } from '$lib/api/events.api'
import { get as getPoll, vote as castVote } from '$lib/api/poll.api'
import { list as listComments, create as createComment, deleteComment } from '$lib/api/comments.api'

export type DetailTab = 'info' | 'poll' | 'comments'

interface EventDetailContext {
  eventId: string
  event: EventResponse | null
  poll: PollResponse | null
  comments: Comment[]
  activeTab: DetailTab
  voteError: string | null
  commentError: string | null
  isVoting: boolean
  isCommenting: boolean
  deletingCommentId: string | null
  error: string | null
}

type EventDetailEvent =
  | { type: 'SWITCH_TAB'; tab: DetailTab }
  | { type: 'VOTE'; slotId: string; value: VoteValue; participantId: string }
  | { type: 'ADD_COMMENT'; content: string; section: CommentSection }
  | { type: 'DELETE_COMMENT'; commentId: string }
  | { type: 'RELOAD' }

type EventDetailInput = { eventId: string }

interface LoadAllOutput {
  event: EventResponse
  poll: PollResponse | null
  comments: Comment[]
}

export const eventDetailMachine = setup({
  types: {
    context: {} as EventDetailContext,
    events: {} as EventDetailEvent,
    input: {} as EventDetailInput
  },
  actors: {
    loadAll: fromPromise(async ({ input }: { input: { eventId: string } }): Promise<LoadAllOutput> => {
      const [event, poll, comments] = await Promise.all([
        getEvent(input.eventId),
        getPoll(input.eventId).catch(() => null),
        listComments(input.eventId).catch(() => [] as Comment[])
      ])
      return { event, poll, comments }
    }),

    voteActor: fromPromise(async ({
      input
    }: {
      input: { eventId: string; slotId: string; value: VoteValue; participantId: string }
    }): Promise<PollResponse> => {
      return castVote(input.eventId, {
        participantId: input.participantId,
        votes: { [input.slotId]: input.value }
      })
    }),

    addCommentActor: fromPromise(async ({
      input
    }: {
      input: { eventId: string; content: string; section: CommentSection }
    }): Promise<Comment> => {
      return createComment(input.eventId, { content: input.content, section: input.section })
    }),

    deleteCommentActor: fromPromise(async ({
      input
    }: {
      input: { eventId: string; commentId: string }
    }): Promise<string> => {
      await deleteComment(input.eventId, input.commentId)
      return input.commentId
    })
  },

  actions: {
    assignData: assign({
      event: ({ event }) => (event as { output: LoadAllOutput }).output.event,
      poll: ({ event }) => (event as { output: LoadAllOutput }).output.poll,
      comments: ({ event }) => (event as { output: LoadAllOutput }).output.comments,
      error: null
    }),

    assignTab: assign({
      activeTab: ({ event }) =>
        (event as { type: 'SWITCH_TAB'; tab: DetailTab }).tab
    }),

    assignPoll: assign({
      poll: ({ event }) => (event as { output: PollResponse }).output,
      voteError: null,
      isVoting: false
    }),

    assignVoteError: assign({
      voteError: ({ event }) => String((event as { error: unknown }).error),
      isVoting: false
    }),

    appendComment: assign({
      comments: ({ context, event }) => [
        ...context.comments,
        (event as { output: Comment }).output
      ],
      commentError: null,
      isCommenting: false
    }),

    assignCommentError: assign({
      commentError: ({ event }) => String((event as { error: unknown }).error),
      isCommenting: false
    }),

    storeDeletingId: assign({
      deletingCommentId: ({ event }) =>
        (event as { type: 'DELETE_COMMENT'; commentId: string }).commentId
    }),

    removeCommentOptimistic: assign({
      comments: ({ context, event }) =>
        context.comments.filter(
          (c) => c.id !== (event as { type: 'DELETE_COMMENT'; commentId: string }).commentId
        )
    }),

    clearDeletingId: assign({ deletingCommentId: null }),

    assignLoadError: assign({
      error: ({ event }) => String((event as { error: unknown }).error)
    })
  }
}).createMachine({
  id: 'eventDetail',
  initial: 'loading',
  context: ({ input }) => ({
    eventId: input.eventId,
    event: null,
    poll: null,
    comments: [],
    activeTab: 'info' as DetailTab,
    voteError: null,
    commentError: null,
    isVoting: false,
    isCommenting: false,
    deletingCommentId: null,
    error: null
  }),
  states: {
    loading: {
      entry: assign({ error: null }),
      invoke: {
        src: 'loadAll',
        input: ({ context }) => ({ eventId: context.eventId }),
        onDone: { target: 'ready', actions: 'assignData' },
        onError: { target: 'error', actions: 'assignLoadError' }
      }
    },

    ready: {
      on: {
        SWITCH_TAB: { actions: 'assignTab' },
        VOTE: { target: 'voting' },
        ADD_COMMENT: { target: 'addingComment' },
        DELETE_COMMENT: {
          target: 'deletingComment',
          actions: ['removeCommentOptimistic', 'storeDeletingId']
        },
        RELOAD: { target: 'loading' }
      }
    },

    voting: {
      entry: assign({ isVoting: true, voteError: null }),
      invoke: {
        src: 'voteActor',
        input: ({ context, event }) => {
          const e = event as { type: 'VOTE'; slotId: string; value: VoteValue; participantId: string }
          return {
            eventId: context.eventId,
            slotId: e.slotId,
            value: e.value,
            participantId: e.participantId
          }
        },
        onDone: { target: 'ready', actions: 'assignPoll' },
        onError: { target: 'ready', actions: 'assignVoteError' }
      }
    },

    addingComment: {
      entry: assign({ isCommenting: true, commentError: null }),
      invoke: {
        src: 'addCommentActor',
        input: ({ context, event }) => {
          const e = event as { type: 'ADD_COMMENT'; content: string; section: CommentSection }
          return {
            eventId: context.eventId,
            content: e.content,
            section: e.section
          }
        },
        onDone: { target: 'ready', actions: 'appendComment' },
        onError: { target: 'ready', actions: 'assignCommentError' }
      }
    },

    deletingComment: {
      invoke: {
        src: 'deleteCommentActor',
        input: ({ context }) => ({
          eventId: context.eventId,
          commentId: context.deletingCommentId!
        }),
        onDone: {
          target: 'ready',
          actions: 'clearDeletingId'
        },
        onError: {
          // Optimistic removal failed — reload from server to restore state
          target: 'loading',
          actions: 'clearDeletingId'
        }
      }
    },

    error: {
      on: {
        RELOAD: { target: 'loading' }
      }
    }
  }
})
