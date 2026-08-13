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
    id: '11111111-1111-1111-1111-111111111111',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@kurum.gov.tr',
    password: 'demo123',
    role: 'CALISAN',
    mustChangePassword: false,
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    firstName: 'Ayşe',
    lastName: 'Kaya',
    email: 'ayse.kaya@kurum.gov.tr',
    password: 'demo123',
    role: 'BASKAN_YARDIMCISI',
    mustChangePassword: false,
  },
  {
    id: '33333333-3333-3333-3333-333333333333',
    firstName: 'Mehmet',
    lastName: 'Demir',
    email: 'mehmet.demir@kurum.gov.tr',
    password: 'demo123',
    role: 'BASKAN',
    mustChangePassword: false,
  },
  {
    id: '44444444-4444-4444-4444-444444444444',
    firstName: 'Zeynep',
    lastName: 'Yönetici',
    email: 'admin@kurum.gov.tr',
    password: 'demo123',
    role: 'ADMIN',
    mustChangePassword: false,
  },
  {
    id: '55555555-5555-5555-5555-555555555555',
    firstName: 'İlk',
    lastName: 'Giriş',
    email: 'ilk.giris@kurum.gov.tr',
    password: 'Gecici123',
    role: 'CALISAN',
    mustChangePassword: true,
  },
]

const initialMockApiUsers = mockApiUsers.map((user) => ({ ...user }))

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
