import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { eventsApi, pollApi, commentsApi } from '../services/api';
import { useApi } from '../hooks/useApi';
import { useAuth } from '../contexts/AuthContext';
import { StatusBadge } from '../components/StatusBadge';
import type { VoteValue, Comment as CommentType } from '../types/api';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '-';
  try {
    return new Date(iso).toLocaleString('fr-FR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  } catch {
    return iso;
  }
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  } catch {
    return iso;
  }
}

const TIME_OF_DAY_LABELS: Record<string, string> = {
  ALL_DAY: 'Toute la journee',
  MORNING: 'Matin',
  AFTERNOON: 'Apres-midi',
  EVENING: 'Soir',
  SPECIFIC: 'Horaire precis',
};

const VOTE_CONFIG: Record<VoteValue, { label: string; emoji: string; className: string; activeClassName: string }> = {
  YES: {
    label: 'Oui',
    emoji: '',
    className: 'border-green-200 text-green-700 hover:bg-green-50',
    activeClassName: 'bg-green-100 border-green-500 text-green-800 ring-2 ring-green-500',
  },
  MAYBE: {
    label: 'Peut-etre',
    emoji: '',
    className: 'border-yellow-200 text-yellow-700 hover:bg-yellow-50',
    activeClassName: 'bg-yellow-100 border-yellow-500 text-yellow-800 ring-2 ring-yellow-500',
  },
  NO: {
    label: 'Non',
    emoji: '',
    className: 'border-red-200 text-red-700 hover:bg-red-50',
    activeClassName: 'bg-red-100 border-red-500 text-red-800 ring-2 ring-red-500',
  },
};

// ---------------------------------------------------------------------------
// Tab type
// ---------------------------------------------------------------------------

type Tab = 'info' | 'poll' | 'comments';

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<Tab>('info');

  const { data: event, error, isLoading, refetch: refetchEvent } = useApi(
    () => eventsApi.get(id!),
    [id],
  );

  const { data: poll, refetch: refetchPoll } = useApi(
    () => pollApi.get(id!).catch(() => null),
    [id],
  );

  const {
    data: comments,
    refetch: refetchComments,
  } = useApi(
    () => commentsApi.list(id!).catch(() => [] as CommentType[]),
    [id],
  );

  const [votingSlot, setVotingSlot] = useState<string | null>(null);
  const [voteLoading, setVoteLoading] = useState(false);

  const handleVote = async (slotId: string, vote: VoteValue) => {
    if (!user || !event) return;
    setVoteLoading(true);
    try {
      await pollApi.vote(event.id, {
        eventId: event.id,
        participantId: user.id,
        slotId,
        vote,
      });
      refetchPoll();
      setVotingSlot(null);
    } catch (err) {
      console.error('Vote error:', err);
    } finally {
      setVoteLoading(false);
    }
  };

  // Comment submission
  const [commentText, setCommentText] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);

  const handleComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !event || !commentText.trim()) return;
    setCommentLoading(true);
    try {
      await commentsApi.create(event.id, {
        section: 'GENERAL',
        content: commentText.trim(),
        authorId: user.id,
        authorName: user.name || user.email || 'Anonyme',
      });
      setCommentText('');
      refetchComments();
    } catch (err) {
      console.error('Comment error:', err);
    } finally {
      setCommentLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-12" role="status" aria-label="Chargement">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-wakeve-600" />
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600" role="alert">{error || 'Evenement introuvable'}</p>
        <Link to="/" className="text-wakeve-600 hover:text-wakeve-700 mt-4 inline-block">
          Retour aux evenements
        </Link>
      </div>
    );
  }

  // Compute vote summary per slot
  const voteSummary: Record<string, { YES: number; MAYBE: number; NO: number }> = {};
  if (poll?.votes) {
    for (const participantVotes of Object.values(poll.votes)) {
      for (const [slotId, vote] of Object.entries(participantVotes)) {
        if (!voteSummary[slotId]) voteSummary[slotId] = { YES: 0, MAYBE: 0, NO: 0 };
        if (vote === 'YES' || vote === 'MAYBE' || vote === 'NO') {
          voteSummary[slotId][vote]++;
        }
      }
    }
  }

  const userVotes: Record<string, string> = user && poll?.votes?.[user.id] ? poll.votes[user.id] : {};

  const tabs: { key: Tab; label: string }[] = [
    { key: 'info', label: 'Informations' },
    { key: 'poll', label: 'Sondage' },
    { key: 'comments', label: `Commentaires${comments && comments.length > 0 ? ` (${comments.length})` : ''}` },
  ];

  return (
    <div className="pb-20 md:pb-0">
      {/* Breadcrumb */}
      <nav className="mb-4" aria-label="Fil d'Ariane">
        <Link to="/" className="text-sm text-wakeve-600 hover:text-wakeve-700">
          Evenements
        </Link>
        <span className="mx-2 text-gray-400">/</span>
        <span className="text-sm text-gray-500 truncate">{event.title}</span>
      </nav>

      {/* Header */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 mb-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{event.title}</h1>
            {event.description && (
              <p className="mt-2 text-gray-600">{event.description}</p>
            )}
          </div>
          <StatusBadge status={event.status} />
        </div>

        <div className="mt-4 flex flex-wrap gap-4 text-sm text-gray-500">
          <span>Date limite : {formatDate(event.deadline)}</span>
          <span>{event.participants.length} participant{event.participants.length !== 1 ? 's' : ''}</span>
          {event.finalDate && <span>Date finale : {formatDate(event.finalDate)}</span>}
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6" role="tablist" aria-label="Sections de l'evenement">
        <div className="flex gap-6">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              role="tab"
              aria-selected={activeTab === tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`pb-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.key
                  ? 'border-wakeve-600 text-wakeve-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tab panels */}
      {activeTab === 'info' && (
        <div role="tabpanel" aria-label="Informations">
          <div className="grid gap-6 md:grid-cols-2">
            {/* Event details */}
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h2 className="font-semibold text-gray-900 mb-4">Details</h2>
              <dl className="space-y-3">
                <div>
                  <dt className="text-xs text-gray-500 uppercase tracking-wider">Statut</dt>
                  <dd className="mt-0.5"><StatusBadge status={event.status} /></dd>
                </div>
                {event.eventType && (
                  <div>
                    <dt className="text-xs text-gray-500 uppercase tracking-wider">Type</dt>
                    <dd className="mt-0.5 text-sm text-gray-900">
                      {event.eventTypeCustom || event.eventType}
                    </dd>
                  </div>
                )}
                <div>
                  <dt className="text-xs text-gray-500 uppercase tracking-wider">Date limite</dt>
                  <dd className="mt-0.5 text-sm text-gray-900">{formatDate(event.deadline)}</dd>
                </div>
                {event.finalDate && (
                  <div>
                    <dt className="text-xs text-gray-500 uppercase tracking-wider">Date finale</dt>
                    <dd className="mt-0.5 text-sm text-gray-900">{formatDate(event.finalDate)}</dd>
                  </div>
                )}
                {event.expectedParticipants && (
                  <div>
                    <dt className="text-xs text-gray-500 uppercase tracking-wider">Participants attendus</dt>
                    <dd className="mt-0.5 text-sm text-gray-900">{event.expectedParticipants}</dd>
                  </div>
                )}
              </dl>
            </div>

            {/* Participants */}
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h2 className="font-semibold text-gray-900 mb-4">
                Participants ({event.participants.length})
              </h2>
              {event.participants.length === 0 ? (
                <p className="text-sm text-gray-500">Aucun participant pour le moment.</p>
              ) : (
                <ul className="space-y-2">
                  {event.participants.map((p) => (
                    <li key={p} className="flex items-center gap-2 text-sm text-gray-700">
                      <div className="w-8 h-8 rounded-full bg-wakeve-100 text-wakeve-700 flex items-center justify-center text-xs font-medium" aria-hidden="true">
                        {p.charAt(0).toUpperCase()}
                      </div>
                      <span>{p}</span>
                      {p === event.organizerId && (
                        <span className="text-xs text-wakeve-600 font-medium">(Organisateur)</span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          {/* Proposed slots */}
          {event.proposedSlots.length > 0 && (
            <div className="mt-6 bg-white rounded-xl border border-gray-200 p-5">
              <h2 className="font-semibold text-gray-900 mb-4">
                Creneaux proposes ({event.proposedSlots.length})
              </h2>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {event.proposedSlots.map((slot) => (
                  <div
                    key={slot.id}
                    className="border border-gray-200 rounded-lg p-3"
                  >
                    <div className="text-sm font-medium text-gray-900">
                      {slot.timeOfDay && slot.timeOfDay !== 'SPECIFIC'
                        ? TIME_OF_DAY_LABELS[slot.timeOfDay] || slot.timeOfDay
                        : formatDateTime(slot.start)}
                    </div>
                    {slot.start && slot.end && slot.timeOfDay === 'SPECIFIC' && (
                      <div className="text-xs text-gray-500 mt-1">
                        {formatDateTime(slot.start)} - {formatDateTime(slot.end)}
                      </div>
                    )}
                    <div className="text-xs text-gray-400 mt-1">{slot.timezone}</div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {activeTab === 'poll' && (
        <div role="tabpanel" aria-label="Sondage">
          {event.proposedSlots.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              Aucun creneau propose pour voter.
            </div>
          ) : (
            <div className="space-y-4">
              {event.proposedSlots.map((slot) => {
                const summary = voteSummary[slot.id] || { YES: 0, MAYBE: 0, NO: 0 };
                const total = summary.YES + summary.MAYBE + summary.NO;
                const currentVote = userVotes[slot.id] as VoteValue | undefined;

                return (
                  <div
                    key={slot.id}
                    className="bg-white rounded-xl border border-gray-200 p-5"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <div className="font-medium text-gray-900">
                          {slot.timeOfDay && slot.timeOfDay !== 'SPECIFIC'
                            ? TIME_OF_DAY_LABELS[slot.timeOfDay] || slot.timeOfDay
                            : formatDateTime(slot.start)}
                        </div>
                        {slot.start && slot.end && slot.timeOfDay === 'SPECIFIC' && (
                          <div className="text-sm text-gray-500 mt-0.5">
                            {formatDateTime(slot.start)} - {formatDateTime(slot.end)}
                          </div>
                        )}
                      </div>

                      {total > 0 && (
                        <div className="text-xs text-gray-500">
                          {total} vote{total !== 1 ? 's' : ''}
                        </div>
                      )}
                    </div>

                    {/* Vote results bar */}
                    {total > 0 && (
                      <div className="mt-3 flex rounded-full h-2 overflow-hidden bg-gray-100">
                        {summary.YES > 0 && (
                          <div
                            className="bg-green-500"
                            style={{ width: `${(summary.YES / total) * 100}%` }}
                            title={`${summary.YES} Oui`}
                          />
                        )}
                        {summary.MAYBE > 0 && (
                          <div
                            className="bg-yellow-400"
                            style={{ width: `${(summary.MAYBE / total) * 100}%` }}
                            title={`${summary.MAYBE} Peut-etre`}
                          />
                        )}
                        {summary.NO > 0 && (
                          <div
                            className="bg-red-400"
                            style={{ width: `${(summary.NO / total) * 100}%` }}
                            title={`${summary.NO} Non`}
                          />
                        )}
                      </div>
                    )}

                    {/* Vote counts */}
                    <div className="mt-2 flex gap-4 text-xs text-gray-500">
                      <span className="text-green-600">{summary.YES} Oui</span>
                      <span className="text-yellow-600">{summary.MAYBE} Peut-etre</span>
                      <span className="text-red-600">{summary.NO} Non</span>
                    </div>

                    {/* Vote buttons */}
                    <div className="mt-3 flex gap-2">
                      {(['YES', 'MAYBE', 'NO'] as VoteValue[]).map((vote) => {
                        const config = VOTE_CONFIG[vote];
                        const isActive = currentVote === vote;
                        return (
                          <button
                            key={vote}
                            onClick={() => handleVote(slot.id, vote)}
                            disabled={voteLoading}
                            className={`flex-1 py-2 px-3 text-sm font-medium rounded-lg border transition-all ${
                              isActive ? config.activeClassName : config.className
                            } disabled:opacity-50`}
                            aria-pressed={isActive}
                            aria-label={`Voter ${config.label} pour ce creneau`}
                          >
                            {config.label}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {activeTab === 'comments' && (
        <div role="tabpanel" aria-label="Commentaires">
          {/* Comment form */}
          <form onSubmit={handleComment} className="bg-white rounded-xl border border-gray-200 p-4 mb-4">
            <label htmlFor="comment-input" className="sr-only">Ajouter un commentaire</label>
            <textarea
              id="comment-input"
              rows={3}
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="Ecrire un commentaire..."
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none resize-none"
              maxLength={2000}
              disabled={commentLoading}
            />
            <div className="flex justify-between items-center mt-2">
              <span className="text-xs text-gray-400">{commentText.length}/2000</span>
              <button
                type="submit"
                disabled={commentLoading || !commentText.trim()}
                className="px-4 py-1.5 bg-wakeve-600 text-white text-sm font-medium rounded-lg hover:bg-wakeve-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {commentLoading ? 'Envoi...' : 'Commenter'}
              </button>
            </div>
          </form>

          {/* Comment list */}
          {comments && comments.length > 0 ? (
            <div className="space-y-3">
              {(Array.isArray(comments) ? comments : []).map((comment: CommentType) => (
                <div key={comment.id} className="bg-white rounded-xl border border-gray-200 p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <div className="w-7 h-7 rounded-full bg-wakeve-100 text-wakeve-700 flex items-center justify-center text-xs font-medium" aria-hidden="true">
                      {comment.authorName.charAt(0).toUpperCase()}
                    </div>
                    <span className="text-sm font-medium text-gray-900">{comment.authorName}</span>
                    <span className="text-xs text-gray-400">
                      {formatDateTime(comment.createdAt)}
                    </span>
                    {comment.isPinned && (
                      <span className="text-xs text-wakeve-600 font-medium">Epingle</span>
                    )}
                  </div>
                  <p className="text-sm text-gray-700 whitespace-pre-wrap">{comment.content}</p>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500 text-sm">
              Aucun commentaire pour le moment. Soyez le premier !
            </div>
          )}
        </div>
      )}
    </div>
  );
}
