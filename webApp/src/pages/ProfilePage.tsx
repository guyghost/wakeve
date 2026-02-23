import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import { supportedLanguages } from '../i18n';

export function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="pb-20 md:pb-0 max-w-lg mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('profile.title')}</h1>

      {/* Organizer Dashboard Link */}
      <button
        onClick={() => navigate('/dashboard')}
        className="w-full mb-4 bg-white rounded-xl border border-gray-200 p-4 flex items-center gap-4 hover:shadow-md transition-shadow text-left"
      >
        <div className="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center shrink-0">
          <svg className="w-5 h-5 text-purple-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
          </svg>
        </div>
        <div className="flex-1">
          <h3 className="text-sm font-semibold text-gray-900">{t('profile.dashboard')}</h3>
          <p className="text-xs text-gray-500">{t('profile.dashboardDescription')}</p>
        </div>
        <svg className="w-5 h-5 text-gray-400 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
        </svg>
      </button>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {/* Avatar */}
        <div className="flex items-center gap-4 mb-6">
          <div className="w-16 h-16 rounded-full bg-wakeve-100 text-wakeve-700 flex items-center justify-center text-2xl font-bold" aria-hidden="true">
            {(user?.name || user?.email || 'I').charAt(0).toUpperCase()}
          </div>
          <div>
            <h2 className="text-lg font-semibold text-gray-900">
              {user?.name || t('profile.defaultUser')}
            </h2>
            <p className="text-sm text-gray-500">
              {user?.email || (user?.isGuest ? t('profile.guestSession') : '')}
            </p>
          </div>
        </div>

        {/* User info */}
        <dl className="space-y-4 mb-6">
          <div>
            <dt className="text-xs text-gray-500 uppercase tracking-wider">{t('profile.userId')}</dt>
            <dd className="mt-0.5 text-sm text-gray-900 font-mono truncate">{user?.id || '-'}</dd>
          </div>
          {user?.authMethod && (
            <div>
              <dt className="text-xs text-gray-500 uppercase tracking-wider">{t('profile.authMethod')}</dt>
              <dd className="mt-0.5 text-sm text-gray-900">{user.authMethod}</dd>
            </div>
          )}
          <div>
            <dt className="text-xs text-gray-500 uppercase tracking-wider">{t('profile.accountType')}</dt>
            <dd className="mt-0.5 text-sm text-gray-900">
              {user?.isGuest ? t('profile.guestAccount') : t('profile.fullAccount')}
            </dd>
          </div>
        </dl>

        {/* Language switcher */}
        <div className="mb-6">
          <label htmlFor="language-select" className="text-xs text-gray-500 uppercase tracking-wider block mb-1">
            {t('profile.language')}
          </label>
          <select
            id="language-select"
            value={i18n.language}
            onChange={(e) => i18n.changeLanguage(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none bg-white text-sm"
          >
            {supportedLanguages.map((lang) => (
              <option key={lang.code} value={lang.code}>
                {lang.label}
              </option>
            ))}
          </select>
        </div>

        {/* Logout */}
        <button
          onClick={handleLogout}
          className="w-full py-2.5 px-4 border border-red-300 text-red-600 font-medium rounded-lg hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 transition-colors"
        >
          {t('profile.logout')}
        </button>
      </div>
    </div>
  );
}
