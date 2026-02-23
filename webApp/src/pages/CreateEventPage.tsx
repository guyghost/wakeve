import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { eventsApi } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import type { CreateTimeSlotRequest, EventType } from '../types/api';

const EVENT_TYPE_KEYS: { value: EventType; key: string }[] = [
  { value: 'BIRTHDAY', key: 'events.typeBirthday' },
  { value: 'WEDDING', key: 'events.typeWedding' },
  { value: 'TEAM_BUILDING', key: 'events.typeTeamBuilding' },
  { value: 'CONFERENCE', key: 'events.typeConference' },
  { value: 'WORKSHOP', key: 'events.typeWorkshop' },
  { value: 'PARTY', key: 'events.typeParty' },
  { value: 'SPORTS_EVENT', key: 'events.typeSportsEvent' },
  { value: 'CULTURAL_EVENT', key: 'events.typeCulturalEvent' },
  { value: 'FAMILY_GATHERING', key: 'events.typeFamilyGathering' },
  { value: 'OUTDOOR_ACTIVITY', key: 'events.typeOutdoorActivity' },
  { value: 'FOOD_TASTING', key: 'events.typeFoodTasting' },
  { value: 'TECH_MEETUP', key: 'events.typeTechMeetup' },
  { value: 'WELLNESS_EVENT', key: 'events.typeWellnessEvent' },
  { value: 'CREATIVE_WORKSHOP', key: 'events.typeCreativeWorkshop' },
  { value: 'OTHER', key: 'events.typeOther' },
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
  const { t } = useTranslation();

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
      setError(t('create.errorNotLoggedIn'));
      return;
    }

    if (!title.trim()) {
      setError(t('create.errorTitleRequired'));
      return;
    }

    if (!deadline) {
      setError(t('create.errorDeadlineRequired'));
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
      setError(err instanceof Error ? err.message : t('create.errorCreation'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="pb-20 md:pb-0 max-w-2xl mx-auto">
      <nav className="mb-4" aria-label={t('common.breadcrumb')}>
        <Link to="/" className="text-sm text-wakeve-600 hover:text-wakeve-700">
          {t('nav.events')}
        </Link>
        <span className="mx-2 text-gray-400">/</span>
        <span className="text-sm text-gray-500">{t('create.breadcrumbNew')}</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('create.title')}</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Title */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
          <h2 className="font-semibold text-gray-900">{t('create.generalInfo')}</h2>

          <div>
            <label htmlFor="event-title" className="block text-sm font-medium text-gray-700 mb-1">
              {t('create.titleLabel')}
            </label>
            <input
              id="event-title"
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder={t('create.titlePlaceholder')}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none"
              maxLength={100}
              disabled={isLoading}
            />
          </div>

          <div>
            <label htmlFor="event-description" className="block text-sm font-medium text-gray-700 mb-1">
              {t('create.descriptionLabel')}
            </label>
            <textarea
              id="event-description"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('create.descriptionPlaceholder')}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none resize-none"
              maxLength={500}
              disabled={isLoading}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="event-type" className="block text-sm font-medium text-gray-700 mb-1">
                {t('create.eventTypeLabel')}
              </label>
              <select
                id="event-type"
                value={eventType}
                onChange={(e) => setEventType(e.target.value as EventType)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none bg-white"
                disabled={isLoading}
              >
                {EVENT_TYPE_KEYS.map((et) => (
                  <option key={et.value} value={et.value}>
                    {t(et.key)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="event-participants" className="block text-sm font-medium text-gray-700 mb-1">
                {t('create.expectedParticipants')}
              </label>
              <input
                id="event-participants"
                type="number"
                min="1"
                value={expectedParticipants}
                onChange={(e) => setExpectedParticipants(e.target.value)}
                placeholder={t('create.expectedParticipantsPlaceholder')}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none"
                disabled={isLoading}
              />
            </div>
          </div>

          <div>
            <label htmlFor="event-deadline" className="block text-sm font-medium text-gray-700 mb-1">
              {t('create.deadlineLabel')}
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
            <h2 className="font-semibold text-gray-900">{t('create.proposedSlots')}</h2>
            <button
              type="button"
              onClick={addSlot}
              className="text-sm text-wakeve-600 hover:text-wakeve-700 font-medium"
              disabled={isLoading}
            >
              {t('create.addSlot')}
            </button>
          </div>

          {slots.map((slot, index) => (
            <div key={slot.id} className="border border-gray-200 rounded-lg p-4 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">
                  {t('create.slotNumber', { number: index + 1 })}
                </span>
                {slots.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeSlot(index)}
                    className="text-sm text-red-500 hover:text-red-700"
                    aria-label={t('create.removeSlotAriaLabel', { number: index + 1 })}
                    disabled={isLoading}
                  >
                    {t('create.remove')}
                  </button>
                )}
              </div>

              <div>
                <label className="block text-xs text-gray-500 mb-1">{t('create.timeOfDay')}</label>
                <select
                  value={slot.timeOfDay}
                  onChange={(e) => updateSlot(index, 'timeOfDay', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none bg-white text-sm"
                  disabled={isLoading}
                >
                  <option value="SPECIFIC">{t('events.timeSpecific')}</option>
                  <option value="ALL_DAY">{t('events.timeAllDay')}</option>
                  <option value="MORNING">{t('events.timeMorning')}</option>
                  <option value="AFTERNOON">{t('events.timeAfternoon')}</option>
                  <option value="EVENING">{t('events.timeEvening')}</option>
                </select>
              </div>

              <div>
                <label className="block text-xs text-gray-500 mb-1">{t('create.date')}</label>
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
                    <label className="block text-xs text-gray-500 mb-1">{t('create.start')}</label>
                    <input
                      type="time"
                      value={slot.startTime}
                      onChange={(e) => updateSlot(index, 'startTime', e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none text-sm"
                      disabled={isLoading}
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">{t('create.end')}</label>
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
            {t('common.cancel')}
          </Link>
          <button
            type="submit"
            disabled={isLoading}
            className="flex-1 py-2.5 px-4 bg-wakeve-600 text-white font-medium rounded-lg hover:bg-wakeve-700 focus:outline-none focus:ring-2 focus:ring-wakeve-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading ? t('create.creating') : t('create.submit')}
          </button>
        </div>
      </form>
    </div>
  );
}
