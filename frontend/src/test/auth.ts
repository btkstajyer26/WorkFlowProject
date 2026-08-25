import { startAuthSession } from '../auth/authSession'
import { demoAccounts } from '../mocks/users'
import { getMockUserByRole } from '../mocks/api/auth'
import type { AuthUser, UserRole } from '../types/auth'

export async function seedAuthenticatedUser(role: UserRole, userOverride?: AuthUser) {
  const account = demoAccounts.find((item) => item.role === role)
  if (!account) throw new Error(`${role} rolü için demo hesabı bulunamadı.`)

  const mockUser = getMockUserByRole(role)
  if (userOverride) {
    Object.assign(mockUser, {
      id: userOverride.id,
      firstName: userOverride.firstName,
      lastName: userOverride.lastName,
      email: userOverride.email,
      role: userOverride.role,
      mustChangePassword: userOverride.mustChangePassword,
    })
  }

  const session = await startAuthSession(
    userOverride ? mockUser.email : account.email,
    account.password,
  )
  return session.user
}
