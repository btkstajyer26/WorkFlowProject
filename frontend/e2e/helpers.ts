import { expect, type Page } from '@playwright/test'

export const e2eUser = {
  email: process.env.E2E_USER_EMAIL ?? 'e2e.calisan@workflow.test',
  password: process.env.E2E_USER_PASSWORD ?? 'E2eCalisanFinal2!',
}

export const e2eDeputy = {
  email: process.env.E2E_DEPUTY_EMAIL ?? 'e2e.yardimci@workflow.test',
  password: process.env.E2E_DEPUTY_PASSWORD ?? 'E2eYardimciFinal2!',
}

export const e2ePresident = {
  email: process.env.E2E_PRESIDENT_EMAIL ?? 'e2e.baskan@workflow.test',
  password: process.env.E2E_PRESIDENT_PASSWORD ?? 'E2eBaskanFinal2!',
}

export const e2eAdmin = {
  email: process.env.E2E_ADMIN_EMAIL ?? 'e2e.admin@workflow.test',
  password: process.env.E2E_ADMIN_PASSWORD ?? 'E2eAdminFinal2!',
}

export async function loginAs(
  page: Page,
  email = e2eUser.email,
  password = e2eUser.password,
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
