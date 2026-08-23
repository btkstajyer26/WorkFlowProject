import { expect, test } from '@playwright/test'
import { e2eAdmin, loginAs } from './helpers'

const apiBaseURL = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'

test('Admin kullanıcı listesi arama, rol, aktiflik ve sunucu sayfalamasını birlikte uygular', async ({ page, request }) => {
  const login = await request.post(`${apiBaseURL}/api/auth/login`, { data: e2eAdmin })
  expect(login.ok(), await login.text()).toBeTruthy()
  const { accessToken } = await login.json() as { accessToken: string }
  const headers = { Authorization: `Bearer ${accessToken}` }
  const runId = `${Date.now()}`
  const createdUsers: Array<{ id: string; email: string }> = []

  for (let index = 1; index <= 7; index += 1) {
    const email = `adminliste.${runId}.${index}@workflow.test`
    const response = await request.post(`${apiBaseURL}/api/admin/users`, {
      headers,
      data: {
        firstName: 'Liste',
        lastName: `${runId} ${index}`,
        email,
        password: 'AdminList1!',
      },
    })
    expect(response.ok(), await response.text()).toBeTruthy()
    createdUsers.push(await response.json() as { id: string; email: string })
  }

  const deactivate = await request.patch(`${apiBaseURL}/api/admin/users/${createdUsers[0].id}/active`, {
    headers,
    data: { active: false },
  })
  expect(deactivate.ok(), await deactivate.text()).toBeTruthy()

  await loginAs(page, e2eAdmin.email, e2eAdmin.password)
  await page.goto('/admin/kullanicilar')
  const search = page.getByPlaceholder('Ad, soyad veya e-posta ara')
  await search.fill(runId)
  await expect.poll(() => new URL(page.url()).searchParams.get('q')).toBe(runId)
  await expect(page.getByText('7 kullanıcı bulundu')).toBeVisible()
  await expect(page.getByText('Sayfa 1 / 2')).toBeVisible()

  await page.getByRole('button', { name: 'Sonraki sayfa' }).click()
  await expect(page).toHaveURL(/sayfa=2/)
  await expect(page.getByText('Sayfa 2 / 2')).toBeVisible()

  await page.getByLabel('Role göre filtrele').selectOption('CALISAN')
  await expect(page).not.toHaveURL(/sayfa=2/)
  await expect(page.getByText('7 kullanıcı bulundu')).toBeVisible()

  await page.getByLabel('Hesap durumuna göre filtrele').selectOption('pasif')
  await expect(page.getByText('1 kullanıcı bulundu')).toBeVisible()
  const desktopTable = page.getByRole('table')
  await expect(desktopTable.getByText(createdUsers[0].email)).toBeVisible()
  await expect(desktopTable.getByText('Pasif', { exact: true })).toBeVisible()
})
