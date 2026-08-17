import { expect, type Page } from '@playwright/test'

export async function loginAs(
  page: Page,
  email = 'john.doe@kurum.gov.tr',
  password = 'demo123',
) {
  await page.goto('/giris')
  await page.getByLabel('E-posta adresi').fill(email)
  await page.getByLabel('Şifre', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Giriş Yap' }).click()
  await expect(page).toHaveURL(/\/(dashboard|admin)$/)
}

export async function expectNoHorizontalOverflow(page: Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(overflow).toBeLessThanOrEqual(1)
}
