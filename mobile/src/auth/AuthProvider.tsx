import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

import type { ChangePasswordRequest, LoginRequest } from '@/api/auth';

import {
  endSession,
  restoreSession,
  startSession,
  subscribeToSession,
  type AuthSession,
  updatePassword,
} from './sessionManager';

type AuthContextValue = {
  changePassword: (request: ChangePasswordRequest) => Promise<void>;
  isAuthenticated: boolean;
  isReady: boolean;
  mustChangePassword: boolean;
  signIn: (credentials: LoginRequest) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    let isMounted = true;
    const unsubscribe = subscribeToSession((nextSession) => {
      if (isMounted) setSession(nextSession);
    });

    void restoreSession()
      .catch(() => null)
      .finally(() => {
        if (isMounted) setIsReady(true);
      });

    return () => {
      isMounted = false;
      unsubscribe();
    };
  }, []);

  const signIn = useCallback(async (credentials: LoginRequest) => {
    await startSession(credentials);
  }, []);

  const signOut = useCallback(async () => {
    await endSession();
  }, []);

  const changePassword = useCallback(async (request: ChangePasswordRequest) => {
    await updatePassword(request);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      changePassword,
      isAuthenticated: session !== null,
      isReady,
      mustChangePassword: session?.mustChangePassword ?? false,
      signIn,
      signOut,
    }),
    [changePassword, isReady, session, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth, AuthProvider içinde kullanılmalıdır.');
  }

  return context;
}
