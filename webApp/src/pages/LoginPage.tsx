import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '../services/api';
import { useAuth } from '../contexts/AuthContext';

type Step = 'email' | 'otp';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [step, setStep] = useState<Step>('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [otpMessage, setOtpMessage] = useState('');

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  const handleRequestOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const response = await authApi.requestOtp({ email });
      setOtpMessage(response.message);
      setStep('otp');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur lors de l\'envoi du code');
    } finally {
      setIsLoading(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const response = await authApi.verifyOtp({ email, otp });
      authApi.saveAuthData(response);
      login(response.user);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Code invalide');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGuestLogin = async () => {
    setError('');
    setIsLoading(true);

    try {
      const deviceId = `web_${Date.now()}_${Math.random().toString(36).slice(2)}`;
      const response = await authApi.loginGuest({ deviceId });
      authApi.saveAuthData(response);
      login(response.user);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur de connexion');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-wakeve-50 to-blue-50 px-4">
      <div className="w-full max-w-md">
        {/* Logo / Title */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-wakeve-700 tracking-tight">Wakeve</h1>
          <p className="mt-2 text-gray-500">Planifiez vos evenements ensemble</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-lg p-8">
          {step === 'email' ? (
            <>
              <h2 className="text-lg font-semibold text-gray-900 mb-6">Connexion</h2>

              <form onSubmit={handleRequestOtp} className="space-y-4">
                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                    Adresse email
                  </label>
                  <input
                    id="email"
                    type="email"
                    required
                    autoFocus
                    autoComplete="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="vous@exemple.com"
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none transition-shadow"
                    aria-describedby={error ? 'login-error' : undefined}
                    disabled={isLoading}
                  />
                </div>

                {error && (
                  <p id="login-error" className="text-sm text-red-600" role="alert">
                    {error}
                  </p>
                )}

                <button
                  type="submit"
                  disabled={isLoading || !email}
                  className="w-full py-2.5 px-4 bg-wakeve-600 text-white font-medium rounded-lg hover:bg-wakeve-700 focus:outline-none focus:ring-2 focus:ring-wakeve-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {isLoading ? 'Envoi en cours...' : 'Recevoir un code'}
                </button>
              </form>

              {/* Divider */}
              <div className="relative my-6">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-gray-200" />
                </div>
                <div className="relative flex justify-center text-sm">
                  <span className="px-2 bg-white text-gray-400">ou</span>
                </div>
              </div>

              {/* Guest mode */}
              <button
                onClick={handleGuestLogin}
                disabled={isLoading}
                className="w-full py-2.5 px-4 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-wakeve-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Continuer en tant qu'invite
              </button>
            </>
          ) : (
            <>
              <button
                onClick={() => {
                  setStep('email');
                  setOtp('');
                  setError('');
                }}
                className="flex items-center gap-1 text-sm text-wakeve-600 hover:text-wakeve-700 mb-4"
                aria-label="Retour a l'etape email"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
                </svg>
                Retour
              </button>

              <h2 className="text-lg font-semibold text-gray-900 mb-2">Entrez le code</h2>
              <p className="text-sm text-gray-500 mb-6">
                {otpMessage || `Un code a 6 chiffres a ete envoye a ${email}`}
              </p>

              <form onSubmit={handleVerifyOtp} className="space-y-4">
                <div>
                  <label htmlFor="otp" className="block text-sm font-medium text-gray-700 mb-1">
                    Code OTP
                  </label>
                  <input
                    id="otp"
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]{6}"
                    maxLength={6}
                    required
                    autoFocus
                    autoComplete="one-time-code"
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    placeholder="000000"
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-wakeve-500 focus:border-wakeve-500 outline-none transition-shadow text-center text-2xl tracking-[0.5em] font-mono"
                    aria-describedby={error ? 'otp-error' : undefined}
                    disabled={isLoading}
                  />
                </div>

                {error && (
                  <p id="otp-error" className="text-sm text-red-600" role="alert">
                    {error}
                  </p>
                )}

                <button
                  type="submit"
                  disabled={isLoading || otp.length !== 6}
                  className="w-full py-2.5 px-4 bg-wakeve-600 text-white font-medium rounded-lg hover:bg-wakeve-700 focus:outline-none focus:ring-2 focus:ring-wakeve-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {isLoading ? 'Verification...' : 'Verifier'}
                </button>
              </form>

              <button
                onClick={handleRequestOtp}
                disabled={isLoading}
                className="w-full mt-3 text-sm text-wakeve-600 hover:text-wakeve-700 disabled:opacity-50"
              >
                Renvoyer le code
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
