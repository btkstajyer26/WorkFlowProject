import { request, type APIRequestContext, type APIResponse } from '@playwright/test'

type LoginResponse = {
  accessToken: string
  refreshToken: string
  mustChangePassword: boolean
}

type AdminUserResponse = {
  id: string
  email: string
  roleName: string
}

type AdminUserPage = {
  content: AdminUserResponse[]
}

const apiBaseURL = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'
const shouldProvision = process.env.E2E_PROVISION_USER === 'true'

const employee = {
  email: process.env.E2E_USER_EMAIL ?? 'e2e.calisan@workflow.test',
  initialPassword: process.env.E2E_USER_INITIAL_PASSWORD ?? 'E2eCalisanInitial1!',
  finalPassword: process.env.E2E_USER_PASSWORD ?? 'E2eCalisanFinal2!',
}

const deputy = {
  email: process.env.E2E_DEPUTY_EMAIL ?? 'e2e.yardimci@workflow.test',
  initialPassword: process.env.E2E_DEPUTY_INITIAL_PASSWORD ?? 'E2eYardimciInitial1!',
  finalPassword: process.env.E2E_DEPUTY_PASSWORD ?? 'E2eYardimciFinal2!',
}

const president = {
  email: process.env.E2E_PRESIDENT_EMAIL ?? 'e2e.baskan@workflow.test',
  initialPassword: process.env.E2E_PRESIDENT_INITIAL_PASSWORD ?? 'E2eBaskanInitial1!',
  finalPassword: process.env.E2E_PRESIDENT_PASSWORD ?? 'E2eBaskanFinal2!',
}

const admin = {
  email: process.env.E2E_ADMIN_EMAIL ?? 'e2e.admin@workflow.test',
  initialPassword: process.env.E2E_ADMIN_INITIAL_PASSWORD ?? 'E2eAdminInitial1!',
  finalPassword: process.env.E2E_ADMIN_PASSWORD ?? 'E2eAdminFinal2!',
}

async function responseSummary(response: APIResponse) {
  const body = await response.text()
  return `${response.status()} ${body.slice(0, 500)}`
}

async function login(api: APIRequestContext, email: string, password: string) {
  const response = await api.post('/api/auth/login', { data: { email, password } })
  if (!response.ok()) {
    return null
  }
  return response.json() as Promise<LoginResponse>
}

async function changePassword(
  api: APIRequestContext,
  accessToken: string,
  currentPassword: string,
  newPassword: string,
) {
  const response = await api.post('/api/auth/change-password', {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { currentPassword, newPassword },
  })
  if (!response.ok()) {
    throw new Error(`E2E parola hazırlığı başarısız: ${await responseSummary(response)}`)
  }
}

async function loginWithFinalPassword(
  api: APIRequestContext,
  account: { email: string; initialPassword: string; finalPassword: string },
) {
  const finalLogin = await login(api, account.email, account.finalPassword)
  if (finalLogin) {
    if (finalLogin.mustChangePassword) {
      throw new Error(`${account.email} hesabı final parolayla açılıyor fakat mustChangePassword hâlâ true.`)
    }
    return finalLogin
  }

  const initialLogin = await login(api, account.email, account.initialPassword)
  if (!initialLogin) {
    throw new Error(
      `${account.email} hesabına E2E başlangıç veya final parolasıyla giriş yapılamadı. ` +
        'İzole bir test veritabanı kullanın ya da E2E_* ortam değişkenlerini güncelleyin.',
    )
  }

  await changePassword(api, initialLogin.accessToken, account.initialPassword, account.finalPassword)
  const changedLogin = await login(api, account.email, account.finalPassword)
  if (!changedLogin || changedLogin.mustChangePassword) {
    throw new Error(`${account.email} hesabının zorunlu parola değişimi tamamlanamadı.`)
  }
  return changedLogin
}

async function findAdminUser(api: APIRequestContext, accessToken: string, email: string) {
  const response = await api.get('/api/admin/users', {
    headers: { Authorization: `Bearer ${accessToken}` },
    params: { q: email, page: 0, size: 10 },
  })
  if (!response.ok()) {
    throw new Error(`E2E kullanıcı araması başarısız: ${await responseSummary(response)}`)
  }
  const page = (await response.json()) as AdminUserPage
  return page.content.find((user) => user.email.toLocaleLowerCase('tr-TR') === email.toLocaleLowerCase('tr-TR'))
}

async function ensureAccount(
  api: APIRequestContext,
  adminAccessToken: string,
  account: { email: string; initialPassword: string; finalPassword: string },
  identity: { firstName: string; lastName: string },
  roleName: 'CALISAN' | 'BASKAN_YARDIMCISI' | 'BASKAN',
) {
  let user = await findAdminUser(api, adminAccessToken, account.email)
  if (!user) {
    const createResponse = await api.post('/api/admin/users', {
      headers: { Authorization: `Bearer ${adminAccessToken}` },
      data: {
        ...identity,
        email: account.email,
        password: account.initialPassword,
      },
    })
    if (!createResponse.ok()) {
      throw new Error(`E2E hesabı oluşturulamadı (${account.email}): ${await responseSummary(createResponse)}`)
    }
    user = (await createResponse.json()) as AdminUserResponse
  }

  await loginWithFinalPassword(api, account)
  if (user.roleName !== roleName) {
    const roleResponse = await api.patch(`/api/admin/users/${user.id}/role`, {
      headers: { Authorization: `Bearer ${adminAccessToken}` },
      data: { roleName },
    })
    if (!roleResponse.ok()) {
      throw new Error(`E2E rol ataması başarısız (${roleName}): ${await responseSummary(roleResponse)}`)
    }
  }
}

async function provisionAccounts(api: APIRequestContext) {
  const adminLogin = await loginWithFinalPassword(api, admin)
  await ensureAccount(api, adminLogin.accessToken, employee, { firstName: 'E2E', lastName: 'Çalışan' }, 'CALISAN')
  await ensureAccount(api, adminLogin.accessToken, deputy, { firstName: 'E2E', lastName: 'Başkan Yardımcısı' }, 'BASKAN_YARDIMCISI')
  await ensureAccount(api, adminLogin.accessToken, president, { firstName: 'E2E', lastName: 'Başkan' }, 'BASKAN')
}

async function verifyEmployee(api: APIRequestContext) {
  const employeeLogin = await login(api, employee.email, employee.finalPassword)
  if (!employeeLogin) {
    throw new Error(
      `E2E backend erişilebilir fakat ${employee.email} hesabıyla giriş yapılamadı. ` +
        'Hesabı hazırlamak için E2E_PROVISION_USER=true kullanın.',
    )
  }
  if (employeeLogin.mustChangePassword) {
    throw new Error(`${employee.email} hesabı zorunlu parola değişimi bekliyor.`)
  }
}

export default async function globalSetup() {
  const api = await request.newContext({ baseURL: apiBaseURL })
  try {
    let health: APIResponse
    try {
      health = await api.get('/actuator/health')
    } catch (error) {
      throw new Error(`E2E backend'e ulaşılamadı: ${apiBaseURL}. Önce izole E2E Docker ortamını başlatın.`, {
        cause: error,
      })
    }
    if (!health.ok()) {
      throw new Error(`E2E backend sağlık kontrolü başarısız: ${await responseSummary(health)}`)
    }

    if (shouldProvision) {
      await provisionAccounts(api)
    } else {
      await verifyEmployee(api)
    }
  } finally {
    await api.dispose()
  }
}
