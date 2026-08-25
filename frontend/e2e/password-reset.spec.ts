import { expect, test, type APIRequestContext } from '@playwright/test'
import { e2eAdmin } from './helpers'

const apiBaseURL = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'
const mailpitBaseURL = process.env.E2E_MAILPIT_BASE_URL ?? 'http://127.0.0.1:18025'

type LoginResponse = { accessToken: string }
type MailpitAddress = { Address: string }
type MailpitMessage = {
  ID: string
  To: MailpitAddress[]
  Subject: string
}
type MailpitList = { messages: MailpitMessage[] }
type MailpitDetail = { Text: string; HTML: string }

async function createEmployee(request: APIRequestContext, email: string, password: string) {
  const login = await request.post(`${apiBaseURL}/api/auth/login`, {
    data: { email: e2eAdmin.email, password: e2eAdmin.password },
  })
  expect(login.ok(), await login.text()).toBeTruthy()
  const { accessToken } = (await login.json()) as LoginResponse

  const created = await request.post(`${apiBaseURL}/api/admin/users`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: {
      firstName: 'Parola',
      lastName: 'Sıfırlama',
      email,
      password,
    },
  })
  expect(created.ok(), await created.text()).toBeTruthy()
}

async function waitForResetCode(request: APIRequestContext, recipient: string) {
  await expect.poll(async () => {
    const response = await request.get(`${mailpitBaseURL}/api/v1/messages`)
    const body = (await response.json()) as MailpitList
    return body.messages.some((message) => (
      message.Subject.includes('Şifre Sıfırlama') &&
      message.To.some((address) => address.Address === recipient)
    ))
  }, { timeout: 10_000 }).toBeTruthy()

  const response = await request.get(`${mailpitBaseURL}/api/v1/messages`)
  const list = (await response.json()) as MailpitList
  const message = list.messages.find((candidate) => (
    candidate.Subject.includes('Şifre Sıfırlama') &&
    candidate.To.some((address) => address.Address === recipient)
  ))
  expect(message).toBeDefined()

  const detailResponse = await request.get(`${mailpitBaseURL}/api/v1/message/${message!.ID}`)
  expect(detailResponse.ok()).toBeTruthy()
  const detail = (await detailResponse.json()) as MailpitDetail
  const code = detail.Text.match(/DOĞRULAMA KODU\s+(\d{6})/)?.[1]
  expect(code).toMatch(/^\d{6}$/)
  return code!
}

test('şifremi unuttum akışı gerçek e-posta koduyla parolayı tek kullanımlık olarak sıfırlar', async ({ page, request }) => {
  const suffix = `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
  const email = `reset.${suffix}@workflow.test`
  const oldPassword = 'ResetInitial1!'
  const newPassword = 'ResetFinal2!'
  await createEmployee(request, email, oldPassword)

  await page.goto('/giris')
  await page.getByRole('link', { name: 'Şifremi unuttum' }).click()
  await expect(page).toHaveURL(/\/sifre-sifirla$/)
  await expect(page.getByRole('heading', { name: 'Şifrenizi sıfırlayın' })).toBeVisible()
  const emailInput = page.getByLabel('E-posta adresi', { exact: true })
  await emailInput.pressSequentially(email, { delay: 5 })
  await expect(emailInput).toHaveValue(email)
  await page.getByRole('button', { name: 'Doğrulama kodu gönder' }).click()
  await expect(page.getByRole('heading', { name: 'E-postanızı kontrol edin' })).toBeVisible()

  const code = await waitForResetCode(request, email)
  const invalidCode = code === '000000' ? '111111' : '000000'
  await page.getByLabel('Doğrulama kodu').fill(invalidCode)
  await page.getByRole('button', { name: 'Kodu doğrula' }).click()
  await expect(page.getByText('Kod geçersiz veya süresi dolmuş. Yeni bir kod isteyebilirsiniz.')).toBeVisible()

  await page.getByLabel('Doğrulama kodu').fill(code)
  const verifyResponsePromise = page.waitForResponse((response) => (
    response.url().endsWith('/api/auth/verify-reset-code') && response.ok()
  ))
  await page.getByRole('button', { name: 'Kodu doğrula' }).click()
  const verifyResponse = await verifyResponsePromise
  const { resetToken } = (await verifyResponse.json()) as { resetToken: string }
  await expect(page).toHaveURL(/\/sifre-degistir$/)
  expect(page.url()).not.toContain('token=')
  expect(resetToken).toBeTruthy()

  await page.getByLabel('Yeni şifre', { exact: true }).fill(newPassword)
  await page.getByLabel('Yeni şifre tekrar', { exact: true }).fill(newPassword)
  await page.getByRole('button', { name: 'Şifreyi sıfırla' }).click()
  await expect(page).toHaveURL(/\/giris\?reason=password-reset$/)
  await expect(page.getByText('Şifreniz sıfırlandı. Yeni şifrenizle giriş yapabilirsiniz.')).toBeVisible()

  const oldLogin = await request.post(`${apiBaseURL}/api/auth/login`, {
    data: { email, password: oldPassword },
  })
  expect(oldLogin.status()).toBe(401)

  const newLogin = await request.post(`${apiBaseURL}/api/auth/login`, {
    data: { email, password: newPassword },
  })
  expect(newLogin.ok(), await newLogin.text()).toBeTruthy()

  const tokenReuse = await request.post(`${apiBaseURL}/api/auth/reset-password`, {
    data: { token: resetToken, newPassword: 'ResetThird3!' },
  })
  expect(tokenReuse.status()).toBe(400)
})

test('bilinmeyen e-posta hesabın varlığını açıklamadan aynı ekranı gösterir ve e-posta üretmez', async ({ page, request }) => {
  const email = `missing.${Date.now()}@workflow.test`

  await page.goto('/sifre-sifirla')
  const emailInput = page.getByLabel('E-posta adresi', { exact: true })
  await emailInput.fill(email)
  await expect(emailInput).toHaveValue(email)
  const responsePromise = page.waitForResponse((response) => response.url().endsWith('/api/auth/forgot-password'))
  await page.getByRole('button', { name: 'Doğrulama kodu gönder' }).click()
  const response = await responsePromise
  expect(response.status()).toBe(202)
  await expect(page.getByRole('heading', { name: 'E-postanızı kontrol edin' })).toBeVisible()

  await page.waitForTimeout(1_000)
  const after = await request.get(`${mailpitBaseURL}/api/v1/messages`)
  const messages = ((await after.json()) as MailpitList).messages
  expect(messages.some((message) => message.To.some((recipient) => recipient.Address === email))).toBe(false)
})
