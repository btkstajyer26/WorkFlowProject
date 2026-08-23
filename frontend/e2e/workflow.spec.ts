import { expect, test, type Browser } from '@playwright/test'
import { e2eDeputy, e2ePresident, e2eUser, loginAs } from './helpers'

async function openRolePage(browser: Browser, credentials: { email: string; password: string }) {
  const context = await browser.newContext()
  const page = await context.newPage()
  await loginAs(page, credentials.email, credentials.password)
  return { context, page }
}

test('kayıt Çalışandan Başkan Yardımcısına, Başkana ve onaya ilerler', async ({ browser }) => {
  const title = `E2E workflow ${Date.now()}`

  const employee = await openRolePage(browser, e2eUser)
  await employee.page.getByRole('button', { name: 'Yeni Kayıt' }).click()
  const composer = employee.page.getByRole('dialog', { name: 'Yeni Kayıt' })
  await composer.getByLabel(/Başlık/).fill(title)
  await composer.getByLabel(/Kategori/).selectOption({ label: 'Bilgi İşlem' })
  await composer.getByLabel(/Açıklama/).fill('Gerçek backend üzerinde uçtan uca workflow doğrulaması.')

  const createResponsePromise = employee.page.waitForResponse((response) =>
    response.request().method() === 'POST' && response.url().endsWith('/api/records'),
  )
  const submitResponsePromise = employee.page.waitForResponse((response) =>
    response.request().method() === 'POST' && /\/api\/records\/[^/]+\/workflow\/actions$/.test(response.url()),
  )
  await composer.getByRole('button', { name: 'İncelemeye Gönder' }).click()
  const createdRecord = await (await createResponsePromise).json() as { id: string }
  expect((await submitResponsePromise).ok()).toBe(true)
  await employee.context.close()

  const deputy = await openRolePage(browser, e2eDeputy)
  await deputy.page.goto(`/kayitlar/${createdRecord.id}`)
  await expect(deputy.page.getByRole('heading', { name: title })).toBeVisible()
  await expect(deputy.page.getByRole('button', { name: 'Başkana İlet' })).toBeVisible()
  await expect(deputy.page.getByRole('button', { name: 'Onayla' })).toHaveCount(0)
  await deputy.page.getByRole('button', { name: 'Başkana İlet' }).click()
  const forwardDialog = deputy.page.getByRole('dialog', { name: 'Başkana ilet' })
  await forwardDialog.getByLabel(/İletme açıklaması/).fill('E2E yardımcı değerlendirmesi.')
  const forwardResponsePromise = deputy.page.waitForResponse((response) =>
    response.request().method() === 'POST' && response.url().includes(`/api/records/${createdRecord.id}/workflow/actions`),
  )
  await forwardDialog.getByRole('button', { name: 'Başkana İlet' }).click()
  expect((await forwardResponsePromise).ok()).toBe(true)
  await deputy.context.close()

  const president = await openRolePage(browser, e2ePresident)
  await president.page.goto(`/kayitlar/${createdRecord.id}`)
  await expect(president.page.getByRole('heading', { name: title })).toBeVisible()
  await expect(president.page.getByRole('button', { name: 'Onayla' })).toBeVisible()
  await expect(president.page.getByRole('button', { name: 'Başkana İlet' })).toHaveCount(0)
  await president.page.getByRole('button', { name: 'Onayla' }).click()
  const approveDialog = president.page.getByRole('dialog', { name: 'Kaydı onayla' })
  const approveResponsePromise = president.page.waitForResponse((response) =>
    response.request().method() === 'POST' && response.url().includes(`/api/records/${createdRecord.id}/workflow/actions`),
  )
  await approveDialog.getByRole('button', { name: 'Onayla' }).click()
  expect((await approveResponsePromise).ok()).toBe(true)

  await president.page.goto(`/kayitlar/${createdRecord.id}`)
  await expect(president.page.getByText('Onaylandı', { exact: true })).toBeVisible()
  await president.context.close()
})
