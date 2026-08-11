import { persistAuthenticatedUser, startAuthSession } from '../auth/authSession'
import { demoAccounts, getDemoUserByRole } from '../mocks/users'
import type { AuthUser, UserRole } from '../types/auth'

export async function seedAuthenticatedUser(role: UserRole, userOverride?: AuthUser) {
  const account = demoAccounts.find((item) => item.role === role)
  if (!account) throw new Error(`${role} rolü için demo hesabı bulunamadı.`)

  await startAuthSession(account.email, account.password)
  const user = userOverride ?? getDemoUserByRole(role)
  persistAuthenticatedUser(user)
  return user
}
