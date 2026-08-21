import {
  changePassword as requestChangePassword,
  login as requestLogin,
  logout as requestLogout,
  refreshSession as requestRefreshSession,
  type ChangePasswordRequest,
  type LoginRequest,
  type LoginResponse,
} from '@/api/auth';
import { setApiAuthHandlers } from '@/api/client';

import {
  clearSessionTokens,
  getAccessToken,
  getRefreshToken,
  saveSessionTokens,
} from './tokenStore';

let refreshRequest: Promise<LoginResponse> | null = null;

export type AuthSession = {
  mustChangePassword: boolean;
};

type SessionListener = (session: AuthSession | null) => void;

const sessionListeners = new Set<SessionListener>();

function publishSession(session: LoginResponse | null): void {
  const publicSession = session
    ? { mustChangePassword: session.mustChangePassword }
    : null;

  sessionListeners.forEach((listener) => listener(publicSession));
}

async function requestFreshSession(): Promise<LoginResponse> {
  const refreshToken = await getRefreshToken();

  if (!refreshToken) {
    throw new Error('Kayıtlı oturum bulunamadı.');
  }

  try {
    const session = await requestRefreshSession({ refreshToken });
    await saveSessionTokens(session);
    publishSession(session);
    return session;
  } catch (error) {
    await clearSessionTokens();
    publishSession(null);
    throw error;
  }
}

function refreshCurrentSession(): Promise<LoginResponse> {
  if (!refreshRequest) {
    refreshRequest = requestFreshSession().finally(() => {
      refreshRequest = null;
    });
  }

  return refreshRequest;
}

export async function startSession(credentials: LoginRequest): Promise<LoginResponse> {
  const session = await requestLogin(credentials);
  await saveSessionTokens(session);
  publishSession(session);
  return session;
}

export async function restoreSession(): Promise<LoginResponse | null> {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) return null;

  try {
    return await refreshCurrentSession();
  } catch {
    return null;
  }
}

export async function endSession(): Promise<void> {
  const refreshToken = await getRefreshToken();

  try {
    if (refreshToken) {
      await requestLogout({ refreshToken });
    }
  } catch {
    // Sunucuya ulaşılamasa da cihazdaki oturum güvenli biçimde kapatılır.
  } finally {
    await clearSessionTokens();
    publishSession(null);
  }
}

export async function updatePassword(request: ChangePasswordRequest): Promise<void> {
  await requestChangePassword(request);
  await clearSessionTokens();
  publishSession(null);
}

export function subscribeToSession(listener: SessionListener): () => void {
  sessionListeners.add(listener);
  return () => sessionListeners.delete(listener);
}

setApiAuthHandlers({
  getAccessToken,
  refreshAccessToken: async () => {
    const session = await refreshCurrentSession();
    return session.accessToken;
  },
});
