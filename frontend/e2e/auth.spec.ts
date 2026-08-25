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

test('kullanıcı profilinden isteğe bağlı şifre değiştirme ekranına ulaşır', async ({ page }) => {
  await loginAs(page)
  await page.getByRole('link', { name: 'Profil' }).click()
  const passwordChangeLink = page.getByRole('link', { name: 'Şifreyi değiştir' })
  await passwordChangeLink.hover()
  await expect(passwordChangeLink.locator('span').first()).toHaveCSS('opacity', '1')
  await passwordChangeLink.click()

  await expect(page).toHaveURL(/\/sifre-degistir$/)
  await expect(page.getByText('Hesap güvenliği')).toBeVisible()
  await expect(page.getByLabel('Mevcut şifre', { exact: true })).toBeVisible()

  await page.getByRole('link', { name: 'Profile dön' }).click()
  await expect(page).toHaveURL(/\/profil$/)
})
