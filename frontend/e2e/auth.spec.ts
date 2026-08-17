import { expect, test } from '@playwright/test'
import { loginAs } from './helpers'

test('kullanıcı boş alanlardan giriş yapıp güvenli biçimde çıkış yapar', async ({ page }) => {
  await page.goto('/giris')

  await expect(page.getByLabel('E-posta adresi')).toHaveValue('')
  await expect(page.getByLabel('E-posta adresi')).toHaveAttribute('placeholder', 'ad.soyad@kurum.gov.tr')
  await expect(page.getByLabel('Şifre', { exact: true })).toHaveValue('')

  await loginAs(page)
  await expect(page.getByRole('heading', { name: /Hoş geldiniz/ })).toBeVisible()

  await page.getByRole('button', { name: 'Çıkış' }).click()
  await expect(page.getByRole('dialog', { name: /Çıkış yapmak istediğinize emin misiniz/ })).toBeVisible()
  await page.getByRole('button', { name: 'Çıkış Yap' }).click()
  await expect(page).toHaveURL(/\/giris$/)
})
