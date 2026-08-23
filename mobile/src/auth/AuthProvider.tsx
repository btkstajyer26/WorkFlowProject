import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useQueryClient } from '@tanstack/react-query';

import type { ChangePasswordRequest, LoginRequest } from '@/api/auth';

import {
  endSession,
  restoreSession,
  startSession,
  subscribeToSession,
  type AuthSession,
  type EndSessionOptions,
  updatePassword,
} from './sessionManager';

type AuthContextValue = {
  changePassword: (request: ChangePasswordRequest) => Promise<void>;
  isAuthenticated: boolean;
  isReady: boolean;
  mustChangePassword: boolean;
  signIn: (credentials: LoginRequest) => Promise<void>;
  signOut: (options?: EndSessionOptions) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    let isMounted = true;
    const unsubscribe = subscribeToSession((nextSession) => {
      if (!isMounted) return;

      if (!nextSession) queryClient.clear();
      setSession(nextSession);
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
  }, [queryClient]);

  const signIn = useCallback(async (credentials: LoginRequest) => {
    queryClient.clear();
    await startSession(credentials);
  }, [queryClient]);

  const signOut = useCallback(async (options?: EndSessionOptions) => {
    await endSession(options);
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
