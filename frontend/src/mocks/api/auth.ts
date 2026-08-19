export type MockApiRole = 'CALISAN' | 'BASKAN_YARDIMCISI' | 'BASKAN' | 'ADMIN'

export type MockApiUser = {
  id: string
  firstName: string
  lastName: string
  email: string
  password: string
  role: MockApiRole
  mustChangePassword: boolean
}

export const mockApiUsers: MockApiUser[] = [
  {
    id: 'user-demo-001',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@kurum.gov.tr',
    password: 'demo123',
    role: 'CALISAN',
    mustChangePassword: false,
  },
  {
    id: 'user-demo-002',
    firstName: 'Ayşe',
    lastName: 'Kaya',
    email: 'ayse.kaya@kurum.gov.tr',
    password: 'demo123',
    role: 'BASKAN_YARDIMCISI',
    mustChangePassword: false,
  },
  {
    id: 'user-demo-003',
    firstName: 'Mehmet',
    lastName: 'Demir',
    email: 'mehmet.demir@kurum.gov.tr',
    password: 'demo123',
    role: 'BASKAN',
    mustChangePassword: false,
  },
  {
    id: 'user-demo-admin',
    firstName: 'Zeynep',
    lastName: 'Yönetici',
    email: 'admin@kurum.gov.tr',
    password: 'demo123',
    role: 'ADMIN',
    mustChangePassword: false,
  },
  {
    id: 'user-demo-first-login',
    firstName: 'İlk',
    lastName: 'Giriş',
    email: 'ilk.giris@kurum.gov.tr',
    password: 'Gecici123',
    role: 'CALISAN',
    mustChangePassword: true,
  },
]

const initialMockApiUsers = mockApiUsers.map((user) => ({ ...user }))
const issuedPasswordResetTokens = new Map<string, string>()

function accessTokenFor(user: MockApiUser) {
  return `msw-access-${user.id}`
}

function refreshTokenFor(user: MockApiUser) {
  return `msw-refresh-${user.id}`
}

export function createMockTokenPair(user: MockApiUser) {
  return {
    accessToken: accessTokenFor(user),
    refreshToken: refreshTokenFor(user),
    mustChangePassword: user.mustChangePassword,
  }
}

export function changeMockPassword(user: MockApiUser, newPassword: string) {
  user.password = newPassword
  user.mustChangePassword = false
}

export function resetMockAuthState() {
  mockApiUsers.splice(0, mockApiUsers.length, ...initialMockApiUsers.map((user) => ({ ...user })))
  issuedPasswordResetTokens.clear()
}

export function mockPasswordResetTokenFor(userId: string) {
  return `msw-password-reset-${userId}`
}

export function issueMockPasswordReset(email?: string) {
  const user = mockApiUsers.find((candidate) => candidate.email === email?.trim().toLowerCase())
  if (!user) return

  const token = mockPasswordResetTokenFor(user.id)
  issuedPasswordResetTokens.set(token, user.id)
}

export function consumeMockPasswordReset(token?: string, newPassword?: string) {
  if (!token || !newPassword) return false

  const userId = issuedPasswordResetTokens.get(token)
  const user = userId ? mockApiUsers.find((candidate) => candidate.id === userId) : undefined
  if (!user) return false

  changeMockPassword(user, newPassword)
  issuedPasswordResetTokens.delete(token)
  return true
}

export function findMockUserByCredentials(email?: string, password?: string) {
  return mockApiUsers.find((user) => (
    user.email === email?.trim().toLowerCase() && user.password === password
  ))
}

export function findMockUserByRefreshToken(refreshToken?: string) {
  return mockApiUsers.find((user) => refreshTokenFor(user) === refreshToken)
}

export function getAuthenticatedMockUser(request: Request) {
  const authorization = request.headers.get('Authorization')
  if (!authorization?.startsWith('Bearer ')) return undefined
  const accessToken = authorization.slice('Bearer '.length)
  return mockApiUsers.find((user) => accessTokenFor(user) === accessToken)
}

export function getMockUserByRole(role: MockApiRole) {
  return mockApiUsers.find((user) => user.role === role)!
}

export function getMockUserById(userId: string) {
  return mockApiUsers.find((user) => user.id === userId)
}
