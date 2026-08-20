import {
  login as requestLogin,
  logout as requestLogout,
  refreshSession as requestRefreshSession,
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

async function requestFreshSession(): Promise<LoginResponse> {
  const refreshToken = await getRefreshToken();

  if (!refreshToken) {
    throw new Error('Kayıtlı oturum bulunamadı.');
  }

  try {
    const session = await requestRefreshSession({ refreshToken });
    await saveSessionTokens(session);
    return session;
  } catch (error) {
    await clearSessionTokens();
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
  } finally {
    await clearSessionTokens();
  }
}

setApiAuthHandlers({
  getAccessToken,
  refreshAccessToken: async () => {
    const session = await refreshCurrentSession();
    return session.accessToken;
  },
});
