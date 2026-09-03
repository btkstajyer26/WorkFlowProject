import { expect, test, type APIRequestContext } from '@playwright/test'
import { e2eUser, loginAs } from './helpers'

const apiBaseURL = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'

type LoginResponse = { accessToken: string }
type Category = { id: number; name: string }
type CreatedRecord = { createdAt: string }

async function loginApi(request: APIRequestContext) {
  const response = await request.post(`${apiBaseURL}/api/auth/login`, { data: e2eUser })
  expect(response.ok(), await response.text()).toBeTruthy()
  return ((await response.json()) as LoginResponse).accessToken
}

test('kayıt listesi arama, kategori, durum, tarih, oluşturan ve sunucu sayfalamasını birlikte uygular', async ({ page, request }) => {
  test.setTimeout(90_000)
  const accessToken = await loginApi(request)
  const headers = { Authorization: `Bearer ${accessToken}` }
  const categoriesResponse = await request.get(`${apiBaseURL}/api/categories`, { headers })
  expect(categoriesResponse.ok(), await categoriesResponse.text()).toBeTruthy()
  const categories = (await categoriesResponse.json()) as Category[]
  expect(categories.length).toBeGreaterThanOrEqual(2)

  const runId = `${Date.now()}`
  const prefix = `E2E sayfalama ${runId}`
  const createdDates = new Set<string>()
  for (let index = 1; index <= 12; index += 1) {
    const response = await request.post(`${apiBaseURL}/api/records`, {
      headers,
      data: {
        title: `${prefix} ${String(index).padStart(2, '0')}`,
        description: index === 7 ? `Özel içerik iğnesi ${runId}` : `Filtre testi ${index}`,
        categoryId: categories[index % 2].id,
      },
    })
    expect(response.status(), await response.text()).toBe(201)
    createdDates.add(((await response.json()) as CreatedRecord).createdAt.slice(0, 10))
  }

  await loginAs(page)
  await page.goto('/kayitlar')
  const search = page.getByLabel('Başlık veya içerikle ara')
  await search.fill(prefix)
  await expect(page).toHaveURL(/q=E2E(?:\+|%20)sayfalama/)
  await expect(page.getByText(/1–10 \/ 12 kayıt/)).toBeVisible()
  await expect(page.getByRole('link', { name: `${prefix} 12`, exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Sonraki sayfa' }).click()
  await expect(page).toHaveURL(/sayfa=2/)
  await expect(page.getByText(/11–12 \/ 12 kayıt/)).toBeVisible()
  await expect(page.getByRole('link', { name: `${prefix} 01`, exact: true })).toBeVisible()

  await page.getByLabel('Sayfa başına').selectOption('5')
  await expect(page).toHaveURL(/boyut=5/)
  await expect(page).not.toHaveURL(/sayfa=2/)
  await expect(page.getByText(/1–5 \/ 12 kayıt/)).toBeVisible()

  await page.getByLabel('Kategori').selectOption(String(categories[1].id))
  await expect(page).toHaveURL(new RegExp(`kategori=${categories[1].id}`))
  await expect(page.getByText(/1–5 \/ 6 kayıt/)).toBeVisible()

  await page.getByLabel('Durum').selectOption('TASLAK')
  await expect(page).toHaveURL(/durum=TASLAK/)
  await expect(page.getByText(/1–5 \/ 6 kayıt/)).toBeVisible()

  // Tarih filtresi sunucunun YEREL gününe göre çalışır: `created_at` bir `LocalDateTime`
  // olarak `LocalDateTime.now()` ile, yani backend konteynerinin saat diliminde yazılır
  // (`docker-compose.e2e.yml` içinde `TZ=Europe/Istanbul`). Koşucunun UTC tarihi ise
  // 21:00–00:00 UTC aralığında sunucununkinden bir gün geride kalır; o pencerede filtre
  // yeni oluşturulan kayıtları dışarıda bırakır ve test saate bağlı olarak kırılırdı.
  // Bu yüzden gün, sunucunun kendi döndürdüğü `createdAt` değerinden alınıyor.
  expect(createdDates.size, `kayıtlar gün sınırını aştı: ${[...createdDates].join(', ')}`).toBe(1)
  const today = [...createdDates][0]
  await page.getByLabel('Oluşturulma başlangıcı').fill(today)
  await page.getByLabel('Oluşturulma bitişi').fill(today)
  await expect(page).toHaveURL(new RegExp(`baslangic=${today}`))
  await expect(page).toHaveURL(new RegExp(`bitis=${today}`))
  await expect(page.getByText(/1–5 \/ 6 kayıt/)).toBeVisible()

  await page.getByLabel('Oluşturan kişi').fill('E2E Çalışan')
  await expect.poll(() => new URL(page.url()).searchParams.get('olusturan')).toBe('E2E Çalışan')
  await expect(page.getByText(/1–5 \/ 6 kayıt/)).toBeVisible()

  await page.getByRole('button', { name: 'Temizle' }).click()
  await expect.poll(() => new URL(page.url()).search).toBe('?boyut=5')

  await search.fill(`Özel içerik iğnesi ${runId}`)
  await expect(page.getByRole('link', { name: `${prefix} 07`, exact: true })).toBeVisible()
  await expect(page.getByText(/1–1 \/ 1 kayıt/)).toBeVisible()
})
