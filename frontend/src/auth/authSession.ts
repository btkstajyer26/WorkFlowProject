import {
  api,
  clearApiAccessToken,
  setApiAccessToken,
  setApiAccessTokenRefresher,
} from '../api/client'
import { ApiClientError } from '../api/errors'
import type { LoginResponse, UserResponse } from '../api/generated/data-contracts'
import type { AuthUser } from '../types/auth'
import { toSystemRoleKey } from '../types/auth'

type AuthTokens = Required<Pick<LoginResponse, 'accessToken' | 'refreshToken'>>
type AuthSession = AuthTokens & { mustChangePassword: boolean; user: AuthUser }
type TokenSession = Omit<AuthSession, 'user'>
type LoginResponseWithPasswordState = LoginResponse & { mustChangePassword?: boolean }
type SessionExpiredListener = () => void

const refreshTokenStorageKey = 'ebys:refresh-token:v1'
const legacyMockSessionKey = 'ebys:mock-session:v1'

let refreshToken: string | null = null
let restorePromise: Promise<AuthUser | null> | null = null
let refreshPromise: Promise<AuthSession> | null = null
const sessionExpiredListeners = new Set<SessionExpiredListener>()

function notifySessionExpired() {
  sessionExpiredListeners.forEach((listener) => listener())
}

function readStoredRefreshToken() {
  try {
    return window.localStorage.getItem(refreshTokenStorageKey)
  } catch {
    return null
  }
}

function persistRefreshToken(token: string) {
  try {
    window.localStorage.setItem(refreshTokenStorageKey, token)
  } catch {
    // Oturum bu sayfa açık kaldığı sürece bellekten çalışmaya devam eder.
  }
}

function removePersistedSession() {
  try {
    window.localStorage.removeItem(refreshTokenStorageKey)
    window.sessionStorage.removeItem(legacyMockSessionKey)
  } catch {
    // Depolama erişimi kapalıysa bellek içi oturum yine temizlenir.
  }
}

function requireAuthTokens(response: LoginResponse): AuthTokens {
  if (!response.accessToken || !response.refreshToken) {
    throw new ApiClientError({
      code: 'INVALID_AUTH_RESPONSE',
      message: 'Sunucu geçerli oturum bilgileri döndürmedi.',
      status: 0,
    })
  }

  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
  }
}

function applyAuthTokens(response: LoginResponse): TokenSession {
  const tokens = requireAuthTokens(response)
  setApiAccessToken(tokens.accessToken)
  refreshToken = tokens.refreshToken
  persistRefreshToken(tokens.refreshToken)
  return {
    ...tokens,
    mustChangePassword: (response as LoginResponseWithPasswordState).mustChangePassword === true,
  }
}

/**
 * Rol adı **doğrulanmaz**. AP-2 ile panelden dinamik rol açılabiliyor ve yerleşik
 * roller yeniden adlandırılabiliyor; adı kapalı bir listeye karşı denetlemek, o
 * kullanıcıların oturum açmasını tamamen engellerdi. Kimlik `roleId` ve
 * `systemKey` ile taşınır, ad yalnız gösterim içindir.
 */
function normalizeCurrentUser(response: UserResponse, mustChangePassword: boolean): AuthUser {
  if (
    !response.id ||
    !response.firstName?.trim() ||
    !response.lastName?.trim() ||
    !response.email?.trim() ||
    typeof response.roleId !== 'number' ||
    !response.roleName?.trim() ||
    response.active !== true
  ) {
    throw new ApiClientError({
      code: 'INVALID_CURRENT_USER_RESPONSE',
      message: 'Sunucu geçerli oturum kullanıcı bilgisi döndürmedi.',
      status: 0,
    })
  }

  return {
    id: response.id,
    firstName: response.firstName.trim(),
    lastName: response.lastName.trim(),
    email: response.email.trim().toLowerCase(),
    roleId: response.roleId,
    systemKey: toSystemRoleKey(response.systemKey),
    roleName: response.roleName.trim(),
    mustChangePassword,
  }
}

async function loadCurrentUser(mustChangePassword: boolean) {
  return normalizeCurrentUser(await api.users.me(), mustChangePassword)
}

export async function startAuthSession(email: string, password: string) {
  clearAuthSession()
  try {
    const response = await api.auth.login({ email, password })
    const tokenSession = applyAuthTokens(response)
    const user = await loadCurrentUser(tokenSession.mustChangePassword)
    return { ...tokenSession, user }
  } catch (error) {
    clearAuthSession()
    throw error
  }
}

async function performAuthRefresh() {
  try {
    const currentRefreshToken = refreshToken ?? readStoredRefreshToken()
    if (!currentRefreshToken) {
      throw new ApiClientError({
        code: 'AUTH_SESSION_NOT_FOUND',
        message: 'Yenilenecek bir oturum bulunamadı.',
        status: 401,
      })
    }

    refreshToken = currentRefreshToken
    const response = await api.auth.refresh({ refreshToken: currentRefreshToken })
    const tokenSession = applyAuthTokens(response)
    const user = await loadCurrentUser(tokenSession.mustChangePassword)
    return { ...tokenSession, user }
  } catch (error) {
    clearAuthSession()
    notifySessionExpired()
    throw error
  }
}

export function refreshAuthSession() {
  if (refreshPromise) return refreshPromise

  refreshPromise = performAuthRefresh().finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

export function restoreAuthSession() {
  if (restorePromise) return restorePromise

  restorePromise = (async () => {
    const storedRefreshToken = readStoredRefreshToken()

    if (!storedRefreshToken) {
      clearAuthSession()
      return null
    }

    refreshToken = storedRefreshToken

    try {
      const session = await refreshAuthSession()
      return session.user
    } catch {
      clearAuthSession()
      return null
    }
  })().finally(() => {
    restorePromise = null
  })

  return restorePromise
}

export async function endAuthSession() {
  const currentRefreshToken = refreshToken
  const logoutRequest = currentRefreshToken
    ? api.auth.logout({ refreshToken: currentRefreshToken })
    : Promise.resolve()

  clearAuthSession()
  await logoutRequest
}

export function clearAuthSession() {
  refreshToken = null
  clearApiAccessToken()
  removePersistedSession()
}

export function subscribeAuthSessionExpired(listener: SessionExpiredListener) {
  sessionExpiredListeners.add(listener)
  return () => {
    sessionExpiredListeners.delete(listener)
  }
}

setApiAccessTokenRefresher(async () => (await refreshAuthSession()).accessToken)
