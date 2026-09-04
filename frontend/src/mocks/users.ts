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
    roleId: 1,
    systemKey: 'CALISAN',
    roleName: 'CALISAN',
    mustChangePassword: false,
    summary: 'Kayıt oluşturur ve süreçlerini takip eder.',
  },
  {
    id: 'user-demo-002',
    firstName: 'Ayşe',
    lastName: 'Kaya',
    email: 'ayse.kaya@kurum.gov.tr',
    password: 'demo123',
    roleId: 2,
    systemKey: 'BASKAN_YARDIMCISI',
    roleName: 'BASKAN_YARDIMCISI',
    mustChangePassword: false,
    summary: 'Gelen kayıtları inceler ve Başkana iletir.',
  },
  {
    id: 'user-demo-003',
    firstName: 'Mehmet',
    lastName: 'Demir',
    email: 'mehmet.demir@kurum.gov.tr',
    password: 'demo123',
    roleId: 3,
    systemKey: 'BASKAN',
    roleName: 'BASKAN',
    mustChangePassword: false,
    summary: 'Nihai onay, red ve geri gönderme işlemlerini yapar.',
  },
  {
    id: 'user-demo-admin',
    firstName: 'Zeynep',
    lastName: 'Yönetici',
    email: 'admin@kurum.gov.tr',
    password: 'demo123',
    roleId: 4,
    systemKey: 'ADMIN',
    roleName: 'ADMIN',
    mustChangePassword: false,
    summary: 'Kullanıcıları, rollerini ve denetim kayıtlarını yönetir.',
  },
  {
    id: 'user-demo-first-login',
    firstName: 'İlk',
    lastName: 'Giriş',
    email: 'ilk.giris@kurum.gov.tr',
    password: 'Gecici123',
    roleId: 1,
    systemKey: 'CALISAN',
    roleName: 'CALISAN',
    mustChangePassword: true,
    summary: 'Zorunlu şifre değiştirme akışını önizler.',
  },
]

export const defaultDemoAccount = demoAccounts[0]

export function getDemoUserByRole(systemKey: AuthUser['systemKey']): AuthUser {
  const account = demoAccounts.find((item) => item.systemKey === systemKey) ?? defaultDemoAccount
  return {
    id: account.id,
    firstName: account.firstName,
    lastName: account.lastName,
    email: account.email,
    roleId: account.roleId,
    systemKey: account.systemKey,
    roleName: account.roleName,
    mustChangePassword: account.mustChangePassword,
  }
}
