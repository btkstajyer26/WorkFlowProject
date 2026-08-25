import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { expectNoHorizontalOverflow, loginAs } from './helpers'

test('giriş ve ana uygulama kritik erişilebilirlik ihlali üretmez', async ({ page }) => {
  await page.goto('/giris')
  const loginResults = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  expect(loginResults.violations).toEqual([])

  await loginAs(page)
  const dashboardResults = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  expect(dashboardResults.violations).toEqual([])
})

test('giriş, dashboard ve kayıt listesi yatay taşma üretmez', async ({ page }) => {
  await page.goto('/giris')
  await expectNoHorizontalOverflow(page)

  await loginAs(page)
  await expectNoHorizontalOverflow(page)
  await page.goto('/kayitlar')
  await expect(page.getByRole('heading', { name: 'Tüm Kayıtlarım' })).toBeVisible()
  await expectNoHorizontalOverflow(page)
})
