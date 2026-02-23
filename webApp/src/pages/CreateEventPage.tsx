import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { eventsApi } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import type { CreateTimeSlotRequest, EventType } from '../types/api';

const EVENT_TYPES: { value: EventType; label: string }[] = [
  { value: 'BIRTHDAY', label: 'Anniversaire' },
  { value: 'WEDDING', label: 'Mariage' },
  { value: 'TEAM_BUILDING', label: 'Team Building' },
  { value: 'CONFERENCE', label: 'Conference' },
  { value: 'WORKSHOP', label: 'Atelier' },
  { value: 'PARTY', label: 'Fete' },
  { value: 'SPORTS_EVENT', label: 'Sport' },
  { value: 'CULTURAL_EVENT', label: 'Evenement culturel' },
  { value: 'FAMILY_GATHERING', label: 'Reunion de famille' },
  { value: 'OUTDOOR_ACTIVITY', label: 'Plein air' },
  { value: 'FOOD_TASTING', label: 'Degustation' },
  { value: 'TECH_MEETUP', label: 'Tech Meetup' },
  { value: 'WELLNESS_EVENT', label: 'Bien-etre' },
  { value: 'CREATIVE_WORKSHOP', label: 'Atelier creatif' },
  { value: 'OTHER', label: 'Autre' },
];

interface SlotForm {
  id: string;
  date: string;
  startTime: string;
  endTime: string;
  timeOfDay: string;
}

function generateId(): string {
  return `slot_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

export function CreateEventPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [eventType, setEventType] = useState<EventType>('OTHER');
  const [deadline, setDeadline] = useState('');
  const [expectedParticipants, setExpectedParticipants] = useState('');
  const [slots, setSlots] = useState<SlotForm[]>([
    { id: generateId(), date: '', startTime: '', endTime: '', timeOfDay: 'SPECIFIC' },
  ]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const addSlot = () => {
    setSlots([
      ...slots,
      { id: generateId(), date: '', startTime: '', endTime: '', timeOfDay: 'SPECIFIC' },
    ]);
  };

  const removeSlot = (index: number) => {
    if (slots.length <= 1) return;
    setSlots(slots.filter((_, i) => i !== index));
  };

  const updateSlot = (index: number, field: keyof SlotForm, value: string) => {
    const updated = [...slots];
    updated[index] = { ...updated[index], [field]: value };
    setSlots(updated);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!user) {
      setError('Vous devez etre connecte pour creer un evenement.');
      return;
    }

    if (!title.trim()) {
      setError('Le titre est requis.');
      return;
    }

    if (!deadline) {
      setError('La date limite est requise.');
      return;
    }

    const proposedSlots: CreateTimeSlotRequest[] = slots
      .filter((s) => s.date || s.timeOfDay !== 'SPECIFIC')
      .map((s) => {
        const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        if (s.timeOfDay !== 'SPECIFIC') {
          return {
            id: s.id,
            start: s.date ? `${s.date}T00:00:00Z` : null,
            end: s.date ? `${s.date}T23:59:59Z` : null,
            timezone,
            timeOfDay: s.timeOfDay,
          };
        }
        return {
          id: s.id,
          start: s.date && s.startTime ? `${s.date}T${s.startTime}:00Z` : null,
          end: s.date && s.endTime ? `${s.date}T${s.endTime}:00Z` : null,
          timezone,
          timeOfDay: 'SPECIFIC',
        };
      });

    setIsLoading(true);
    try {
      const response = await eventsApi.create({
        title: title.trim(),
        description: description.trim(),
        organizerId: user.id,
        deadline: `${deadline}T23:59:59Z`,
        proposedSlots,
        eventType,
        expectedParticipants: expectedParticipants ? parseInt(expectedParticipants) : undefined,
      });
      navigate(`/events/${response.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur lors de la creation');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="pb-20 md:pb-0 max-w-2xl mx-auto">
      <nav className="mb-4" aria-label="Fil d'Ariane">
        <Link to="/" className="text-sm text-wakeve-600 hover:text-wakeve-700">
          Evenements
        </Link>
        <span className="mx-2 text-gray-400">/</span>
        <span className="text-sm text-gray-500">Nouvel evenement</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-6">Creer un evenement</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Title */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
          <h2 className="font-semibold text-gray-900">Informations generales</h2>

          <div>
            <label htmlFor="event-title" className="block text-sm font-medium text-gray-700 mb-1">
              Titre *
            </label>
            <input
              id="event-title"
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ex: Soiree d'anniversaire"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none"
              maxLength={100}
              disabled={isLoading}
            />
          </div>

          <div>
            <label htmlFor="event-description" className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              id="event-description"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Decrivez votre evenement..."
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none resize-none"
              maxLength={500}
              disabled={isLoading}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="event-type" className="block text-sm font-medium text-gray-700 mb-1">
                Type d'evenement
              </label>
              <select
                id="event-type"
                value={eventType}
                onChange={(e) => setEventType(e.target.value as EventType)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none bg-white"
                disabled={isLoading}
              >
                {EVENT_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="event-participants" className="block text-sm font-medium text-gray-700 mb-1">
                Participants attendus
              </label>
              <input
                id="event-participants"
                type="number"
                min="1"
                value={expectedParticipants}
                onChange={(e) => setExpectedParticipants(e.target.value)}
                placeholder="Ex: 10"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none"
                disabled={isLoading}
              />
            </div>
          </div>

          <div>
            <label htmlFor="event-deadline" className="block text-sm font-medium text-gray-700 mb-1">
              Date limite de vote *
            </label>
            <input
              id="event-deadline"
              type="date"
              required
              value={deadline}
              onChange={(e) => setDeadline(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none"
              disabled={isLoading}
            />
          </div>
        </div>

        {/* Time slots */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-gray-900">Creneaux proposes</h2>
            <button
              type="button"
              onClick={addSlot}
              className="text-sm text-wakeve-600 hover:text-wakeve-700 font-medium"
              disabled={isLoading}
            >
              + Ajouter un creneau
            </button>
          </div>

          {slots.map((slot, index) => (
            <div key={slot.id} className="border border-gray-200 rounded-lg p-4 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">
                  Creneau {index + 1}
                </span>
                {slots.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeSlot(index)}
                    className="text-sm text-red-500 hover:text-red-700"
                    aria-label={`Supprimer le creneau ${index + 1}`}
                    disabled={isLoading}
                  >
                    Supprimer
                  </button>
                )}
              </div>

              <div>
                <label className="block text-xs text-gray-500 mb-1">Moment de la journee</label>
                <select
                  value={slot.timeOfDay}
                  onChange={(e) => updateSlot(index, 'timeOfDay', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none bg-white text-sm"
                  disabled={isLoading}
                >
                  <option value="SPECIFIC">Horaire precis</option>
                  <option value="ALL_DAY">Toute la journee</option>
                  <option value="MORNING">Matin</option>
                  <option value="AFTERNOON">Apres-midi</option>
                  <option value="EVENING">Soir</option>
                </select>
              </div>

              <div>
                <label className="block text-xs text-gray-500 mb-1">Date</label>
                <input
                  type="date"
                  value={slot.date}
                  onChange={(e) => updateSlot(index, 'date', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none text-sm"
                  disabled={isLoading}
                />
              </div>

              {slot.timeOfDay === 'SPECIFIC' && (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">Debut</label>
                    <input
                      type="time"
                      value={slot.startTime}
                      onChange={(e) => updateSlot(index, 'startTime', e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none text-sm"
                      disabled={isLoading}
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">Fin</label>
                    <input
                      type="time"
                      value={slot.endTime}
                      onChange={(e) => updateSlot(index, 'endTime', e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none text-sm"
                      disabled={isLoading}
                    />
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Error */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700 text-sm" role="alert">
            {error}
          </div>
        )}

        {/* Submit */}
        <div className="flex gap-3">
          <Link
            to="/"
            className="flex-1 py-2.5 px-4 border border-gray-300 text-gray-700 font-medium rounded-lg text-center hover:bg-gray-50 transition-colors"
          >
            Annuler
          </Link>
          <button
            type="submit"
            disabled={isLoading}
            className="flex-1 py-2.5 px-4 bg-wakeve-600 text-white font-medium rounded-lg hover:bg-wakeve-700 focus:outline-none focus:ring-2 focus:ring-wakeve-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading ? 'Creation en cours...' : 'Creer l\'evenement'}
          </button>
        </div>
      </form>
    </div>
  );
}
