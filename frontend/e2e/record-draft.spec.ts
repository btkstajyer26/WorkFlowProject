import { expect, test } from '@playwright/test'
import { e2eUser, loginAs } from './helpers'

const apiBaseURL = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'

test('çalışan taslak ekini yükler, indirir, siler; gönderilen kaydın ekini değiştiremez', async ({ page, request }) => {
  await loginAs(page)
  await page.getByRole('button', { name: 'Yeni Kayıt' }).click()

  const composer = page.getByRole('dialog', { name: 'Yeni Kayıt' })
  const title = `E2E donanım talebi ${Date.now()}`
  await composer.getByLabel(/Başlık/).fill(title)
  await composer.getByLabel(/Kategori/).selectOption({ label: 'Bilgi İşlem' })
  await composer.getByLabel(/Açıklama/).fill('Tarayıcı testi için oluşturulan geçici taslak.')

  await composer.locator('input[type="file"]').setInputFiles({
    name: 'e2e-belge.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF'),
  })

  const createResponsePromise = page.waitForResponse((response) =>
    response.request().method() === 'POST' && response.url().endsWith('/api/records'),
  )
  const uploadResponsePromise = page.waitForResponse((response) =>
    response.request().method() === 'POST' && /\/api\/records\/[^/]+\/files$/.test(response.url()),
  )
  await composer.getByRole('button', { name: 'Taslak Kaydet' }).click()

  const createResponse = await createResponsePromise
  expect(createResponse.ok()).toBe(true)
  const createdRecord = await createResponse.json() as { id: string }
  const uploadResponse = await uploadResponsePromise
  expect(uploadResponse.ok()).toBe(true)

  await expect(composer.getByText("Taslaklarım'a kaydedildi.")).toBeVisible()
  await composer.getByRole('button', { name: 'Yeni Kayıt Oluştur' }).click()
  await expect(composer.getByLabel(/Başlık/)).toHaveValue('')
  await expect(composer.getByLabel(/Kategori/)).toHaveValue('0')
  await expect(composer.getByLabel(/Açıklama/)).toHaveValue('')

  await page.goto(`/kayitlar/${createdRecord.id}`)
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
  await expect(page.getByText('e2e-belge.pdf')).toBeVisible()

  const downloadResponsePromise = page.waitForResponse((response) =>
    response.request().method() === 'GET' && /\/api\/files\/[^/]+\/download$/.test(response.url()),
  )
  await page.getByRole('button', { name: 'e2e-belge.pdf dosyasını indir' }).click()
  expect((await downloadResponsePromise).ok()).toBe(true)

  await page.goto(`/kayitlar/${createdRecord.id}/duzenle`)
  const deleteResponsePromise = page.waitForResponse((response) =>
    response.request().method() === 'DELETE' && /\/api\/files\/[^/]+$/.test(response.url()),
  )
  await page.getByRole('button', { name: 'e2e-belge.pdf dosyasını sil' }).click()
  expect((await deleteResponsePromise).status()).toBe(204)
  await expect(page.getByText('e2e-belge.pdf')).not.toBeVisible()

  const replacementName = 'e2e-gonderilen-belge.pdf'
  const replacementUploadPromise = page.waitForResponse((response) =>
    response.request().method() === 'POST' && /\/api\/records\/[^/]+\/files$/.test(response.url()),
  )
  await page.locator('input[type="file"]').setInputFiles({
    name: replacementName,
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF'),
  })
  const replacementUpload = await replacementUploadPromise
  expect(replacementUpload.ok(), await replacementUpload.text()).toBeTruthy()
  const [replacementFile] = await replacementUpload.json() as Array<{ id: string }>
  await expect(page.getByText(replacementName)).toBeVisible()

  await page.getByRole('button', { name: 'İncelemeye Gönder' }).click()
  const submitDialog = page.getByRole('dialog', { name: 'Başkan Yardımcısına gönder' })
  const submitResponsePromise = page.waitForResponse((response) =>
    response.request().method() === 'POST' && /\/api\/records\/[^/]+\/workflow\/actions$/.test(response.url()),
  )
  await submitDialog.getByRole('button', { name: 'İncelemeye Gönder' }).click()
  expect((await submitResponsePromise).ok()).toBeTruthy()
  await expect(page).toHaveURL(new RegExp(`/kayitlar/${createdRecord.id}$`))
  await expect(page.getByRole('button', { name: `${replacementName} dosyasını sil` })).toHaveCount(0)

  const login = await request.post(`${apiBaseURL}/api/auth/login`, { data: e2eUser })
  const { accessToken } = await login.json() as { accessToken: string }
  const forbiddenDelete = await request.delete(`${apiBaseURL}/api/files/${replacementFile.id}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  expect(forbiddenDelete.status()).toBe(400)
})
