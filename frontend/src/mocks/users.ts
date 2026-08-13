import type { AuthUser } from '../types/auth'

export type DemoAccount = AuthUser & {
  password: string
  summary: string
}

export const demoAccounts: DemoAccount[] = [
  {
    id: 'user-demo-001',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@kurum.gov.tr',
    password: 'demo123',
    role: 'CALISAN',
    mustChangePassword: false,
    summary: 'Kayıt oluşturur ve süreçlerini takip eder.',
  },
  {
    id: 'user-demo-002',
    firstName: 'Ayşe',
    lastName: 'Kaya',
    email: 'ayse.kaya@kurum.gov.tr',
    password: 'demo123',
    role: 'BASKAN_YARDIMCISI',
    mustChangePassword: false,
    summary: 'Gelen kayıtları inceler ve Başkana iletir.',
  },
  {
    id: 'user-demo-003',
    firstName: 'Mehmet',
    lastName: 'Demir',
    email: 'mehmet.demir@kurum.gov.tr',
    password: 'demo123',
    role: 'BASKAN',
    mustChangePassword: false,
    summary: 'Nihai onay, red ve geri gönderme işlemlerini yapar.',
  },
  {
    id: 'user-demo-admin',
    firstName: 'Zeynep',
    lastName: 'Yönetici',
    email: 'admin@kurum.gov.tr',
    password: 'demo123',
    role: 'ADMIN',
    mustChangePassword: false,
    summary: 'Kullanıcıları, rollerini ve denetim kayıtlarını yönetir.',
  },
  {
    id: 'user-demo-first-login',
    firstName: 'İlk',
    lastName: 'Giriş',
    email: 'ilk.giris@kurum.gov.tr',
    password: 'Gecici123',
    role: 'CALISAN',
    mustChangePassword: true,
    summary: 'Zorunlu şifre değiştirme akışını önizler.',
  },
]

export const defaultDemoAccount = demoAccounts[0]

export function getDemoUserByRole(role: AuthUser['role']): AuthUser {
  const account = demoAccounts.find((item) => item.role === role) ?? defaultDemoAccount
  return {
    id: account.id,
    firstName: account.firstName,
    lastName: account.lastName,
    email: account.email,
    role: account.role,
    mustChangePassword: account.mustChangePassword,
  }
}

export function getDemoUserById(userId: string) {
  return demoAccounts.find((item) => item.id === userId)
}

export function getDemoUserName(userId: string) {
  const user = getDemoUserById(userId)
  return user ? `${user.firstName} ${user.lastName}` : 'Bilinmeyen kullanıcı'
}
