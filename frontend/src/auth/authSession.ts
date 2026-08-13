import { api, clearApiAccessToken, setApiAccessToken } from '../api/client'
import { ApiClientError } from '../api/errors'
import type { LoginResponse } from '../api/generated/data-contracts'
import type { AuthUser, UserRole } from '../types/auth'

type AuthTokens = Required<Pick<LoginResponse, 'accessToken' | 'refreshToken'>>

const refreshTokenStorageKey = 'ebys:refresh-token:v1'
const authenticatedUserStorageKey = 'ebys:authenticated-user:v1'
const legacyMockSessionKey = 'ebys:mock-session:v1'
const userRoles: UserRole[] = ['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN']

let refreshToken: string | null = null
let restorePromise: Promise<AuthUser | null> | null = null

function isAuthUser(value: unknown): value is AuthUser {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<AuthUser>
  return (
    typeof candidate.id === 'string' &&
    typeof candidate.firstName === 'string' &&
    typeof candidate.lastName === 'string' &&
    typeof candidate.email === 'string' &&
    Boolean(candidate.role && userRoles.includes(candidate.role))
  )
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
    window.localStorage.removeItem(authenticatedUserStorageKey)
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

function applyAuthTokens(response: LoginResponse) {
  const tokens = requireAuthTokens(response)
  setApiAccessToken(tokens.accessToken)
  refreshToken = tokens.refreshToken
  persistRefreshToken(tokens.refreshToken)
  return tokens
}

export async function startAuthSession(email: string, password: string) {
  clearAuthSession()
  const response = await api.auth.login({ email, password })
  return applyAuthTokens(response)
}

export async function refreshAuthSession() {
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
  return applyAuthTokens(response)
}

export function persistAuthenticatedUser(user: AuthUser | null) {
  try {
    if (user) window.localStorage.setItem(authenticatedUserStorageKey, JSON.stringify(user))
    else window.localStorage.removeItem(authenticatedUserStorageKey)
  } catch {
    // Kullanıcı görünümü App state'inde çalışmaya devam eder.
  }
}

export function readPersistedAuthenticatedUser(): AuthUser | null {
  try {
    const storedUser = window.localStorage.getItem(authenticatedUserStorageKey)
    if (!storedUser) return null
    const parsedUser: unknown = JSON.parse(storedUser)
    if (isAuthUser(parsedUser)) return parsedUser
  } catch {
    // Bozuk veya erişilemeyen storage aşağıda temizlenir.
  }

  persistAuthenticatedUser(null)
  return null
}

export function restoreAuthSession() {
  if (restorePromise) return restorePromise

  restorePromise = (async () => {
    const storedUser = readPersistedAuthenticatedUser()
    const storedRefreshToken = readStoredRefreshToken()

    if (!storedUser || !storedRefreshToken) {
      clearAuthSession()
      return null
    }

    refreshToken = storedRefreshToken

    try {
      await refreshAuthSession()
      return storedUser
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
