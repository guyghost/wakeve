import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { UserDTO } from '../types/api';
import { authApi } from '../services/api';

interface AuthContextValue {
  user: UserDTO | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (user: UserDTO) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Restore session on mount
  useEffect(() => {
    const storedUser = authApi.getStoredUser();
    if (storedUser && authApi.isLoggedIn()) {
      setUser(storedUser);
    }
    setIsLoading(false);
  }, []);

  // Listen for auth expiry events
  useEffect(() => {
    const handleExpired = () => {
      setUser(null);
    };
    window.addEventListener('wakeve:auth-expired', handleExpired);
    return () => window.removeEventListener('wakeve:auth-expired', handleExpired);
  }, []);

  const login = useCallback((u: UserDTO) => {
    setUser(u);
  }, []);

  const logout = useCallback(() => {
    authApi.logout();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: !!user,
      isLoading,
      login,
      logout,
    }),
    [user, isLoading, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
