import { expect, test, type APIRequestContext } from '@playwright/test'
import { e2eAdmin, e2eDeputy, e2ePresident, e2eUser } from './helpers'

const apiBaseURL = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'

type Credentials = { email: string; password: string }
type LoginResponse = { accessToken: string; mustChangePassword: boolean }
type UserResponse = { id: string; email: string; roleName: string; active: boolean }
type UserPage = { content: UserResponse[] }
type Category = { id: number }
type RecordResponse = { id: string; status: string }

async function login(request: APIRequestContext, credentials: Credentials) {
  const response = await request.post(`${apiBaseURL}/api/auth/login`, { data: credentials })
  expect(response.ok(), await response.text()).toBeTruthy()
  return response.json() as Promise<LoginResponse>
}

function headers(token: string) {
  return { Authorization: `Bearer ${token}` }
}

async function findUser(request: APIRequestContext, adminToken: string, email: string) {
  const response = await request.get(`${apiBaseURL}/api/admin/users`, {
    headers: headers(adminToken),
    params: { q: email, page: 0, size: 10 },
  })
  expect(response.ok(), await response.text()).toBeTruthy()
  const body = (await response.json()) as UserPage
  const user = body.content.find((candidate) => candidate.email === email)
  expect(user).toBeDefined()
  return user!
}

async function createEmployee(request: APIRequestContext, adminToken: string, email: string, password: string) {
  const response = await request.post(`${apiBaseURL}/api/admin/users`, {
    headers: headers(adminToken),
    data: { firstName: 'Devir', lastName: 'Adayı', email, password },
  })
  expect(response.ok(), await response.text()).toBeTruthy()
  return response.json() as Promise<UserResponse>
}

test('tekil Başkan Yardımcısı devri atomik yapılır, bekleyen işler ve son yardımcı referansı yeni kişiye taşınır', async ({ request }) => {
  const runId = `${Date.now()}`
  const candidate = {
    email: `devir.${runId}@workflow.test`,
    initialPassword: 'DevirInitial1!',
    finalPassword: 'DevirFinal2!',
  }
  const secondCandidate = {
    email: `devir.ikinci.${runId}@workflow.test`,
    password: 'DevirSecond1!',
  }

  const admin = await login(request, e2eAdmin)
  const employee = await login(request, e2eUser)
  const deputy = await login(request, e2eDeputy)
  const president = await login(request, e2ePresident)
  const oldDeputy = await findUser(request, admin.accessToken, e2eDeputy.email)
  const replacement = await createEmployee(request, admin.accessToken, candidate.email, candidate.initialPassword)
  const competingCandidate = await createEmployee(request, admin.accessToken, secondCandidate.email, secondCandidate.password)

  const categoriesResponse = await request.get(`${apiBaseURL}/api/categories`, { headers: headers(employee.accessToken) })
  const categories = (await categoriesResponse.json()) as Category[]
  const createRecord = await request.post(`${apiBaseURL}/api/records`, {
    headers: headers(employee.accessToken),
    data: {
      title: `E2E görev devri ${runId}`,
      description: 'Başkana ulaştıktan sonra yeni Başkan Yardımcısına dönecek kayıt.',
      categoryId: categories[0].id,
    },
  })
  expect(createRecord.status(), await createRecord.text()).toBe(201)
  const record = (await createRecord.json()) as RecordResponse

  const submit = await request.post(`${apiBaseURL}/api/records/${record.id}/workflow/actions`, {
    headers: headers(employee.accessToken),
    data: { action: 'GONDER' },
  })
  expect(submit.ok(), await submit.text()).toBeTruthy()
  const forward = await request.post(`${apiBaseURL}/api/records/${record.id}/workflow/actions`, {
    headers: headers(deputy.accessToken),
    data: { action: 'BASKANA_ILET', comment: 'Görev devri öncesi Başkana iletildi.' },
  })
  expect(forward.ok(), await forward.text()).toBeTruthy()

  const transfer = await request.patch(`${apiBaseURL}/api/admin/users/${oldDeputy.id}/role`, {
    headers: headers(admin.accessToken),
    data: {
      roleName: 'CALISAN',
      replacementBaskanYardimcisiId: replacement.id,
    },
  })
  expect(transfer.ok(), await transfer.text()).toBeTruthy()

  const demotedDeputy = await findUser(request, admin.accessToken, e2eDeputy.email)
  const promotedReplacement = await findUser(request, admin.accessToken, candidate.email)
  expect(demotedDeputy.roleName).toBe('CALISAN')
  expect(promotedReplacement.roleName).toBe('BASKAN_YARDIMCISI')

  const directDeactivate = await request.patch(`${apiBaseURL}/api/admin/users/${replacement.id}/active`, {
    headers: headers(admin.accessToken),
    data: { active: false },
  })
  expect(directDeactivate.status()).toBe(400)

  const duplicateDeputy = await request.patch(`${apiBaseURL}/api/admin/users/${competingCandidate.id}/role`, {
    headers: headers(admin.accessToken),
    data: { roleName: 'BASKAN_YARDIMCISI' },
  })
  expect(duplicateDeputy.status()).toBe(409)

  const returnToDeputy = await request.post(`${apiBaseURL}/api/records/${record.id}/workflow/actions`, {
    headers: headers(president.accessToken),
    data: {
      action: 'BASKAN_YARDIMCISINA_GERI_GONDER',
      comment: 'Yeni Başkan Yardımcısı tekrar değerlendirsin.',
    },
  })
  expect(returnToDeputy.ok(), await returnToDeputy.text()).toBeTruthy()

  const initialReplacementLogin = await login(request, {
    email: candidate.email,
    password: candidate.initialPassword,
  })
  expect(initialReplacementLogin.mustChangePassword).toBeTruthy()
  const changePassword = await request.post(`${apiBaseURL}/api/auth/change-password`, {
    headers: headers(initialReplacementLogin.accessToken),
    data: { currentPassword: candidate.initialPassword, newPassword: candidate.finalPassword },
  })
  expect(changePassword.ok(), await changePassword.text()).toBeTruthy()
  const replacementLogin = await login(request, { email: candidate.email, password: candidate.finalPassword })
  const recordAsReplacement = await request.get(`${apiBaseURL}/api/records/${record.id}`, {
    headers: headers(replacementLogin.accessToken),
  })
  expect(recordAsReplacement.ok(), await recordAsReplacement.text()).toBeTruthy()
  const returnedRecord = (await recordAsReplacement.json()) as RecordResponse
  expect(returnedRecord.status).toBe('BSK_YRD_INCELEMESINDE')

  const restoreDeputy = await request.patch(`${apiBaseURL}/api/admin/users/${replacement.id}/role`, {
    headers: headers(admin.accessToken),
    data: {
      roleName: 'CALISAN',
      replacementBaskanYardimcisiId: oldDeputy.id,
    },
  })
  expect(restoreDeputy.ok(), await restoreDeputy.text()).toBeTruthy()
})
