import { expect, test } from '@playwright/test'
import { e2eAdmin, loginAs } from './helpers'

test('Çalışan Admin ekranına giremez, Admin kullanıcı yönetimini açabilir', async ({ page }) => {
  await loginAs(page)
  await page.goto('/admin/kullanicilar')
  await expect(page).toHaveURL(/\/403$/)
  await expect(page.getByRole('heading', { name: 'Bu sayfayı görüntüleme yetkiniz yok' })).toBeVisible()

  await page.context().clearCookies()
  await page.evaluate(() => localStorage.clear())
  await page.goto('/giris')
  await loginAs(page, e2eAdmin.email, e2eAdmin.password)
  await expect(page).toHaveURL(/\/admin$/)
  await expect(page.getByRole('heading', { name: 'Yönetim Özeti' })).toBeVisible()

  await page.goto('/admin/kullanicilar')
  await expect(page.getByRole('heading', { name: 'Kullanıcılar' })).toBeVisible()
  await expect(page.getByRole('table').getByText('E2E Çalışan')).toBeVisible()
})
