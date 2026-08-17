import { expect, test } from '@playwright/test'
import { loginAs } from './helpers'

test('çalışan yeni kaydı taslak olarak oluşturur ve yeni form temiz açılır', async ({ page }) => {
  await loginAs(page)
  await page.getByRole('button', { name: 'Yeni Kayıt' }).click()

  const composer = page.getByRole('dialog', { name: 'Yeni Kayıt' })
  await composer.getByLabel(/Başlık/).fill('E2E donanım talebi')
  await composer.getByLabel(/Kategori/).selectOption({ label: 'Bilgi İşlem' })
  await composer.getByLabel(/Açıklama/).fill('Tarayıcı testi için oluşturulan geçici taslak.')
  await composer.getByRole('button', { name: 'Taslak Kaydet' }).click()

  await expect(composer.getByText("Taslaklarım'a kaydedildi.")).toBeVisible()
  await composer.getByRole('button', { name: 'Yeni Kayıt Oluştur' }).click()
  await expect(composer.getByLabel(/Başlık/)).toHaveValue('')
  await expect(composer.getByLabel(/Kategori/)).toHaveValue('0')
  await expect(composer.getByLabel(/Açıklama/)).toHaveValue('')
})
