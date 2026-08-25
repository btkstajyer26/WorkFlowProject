import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5174'
const apiBaseURL = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'

export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/global-setup.ts',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  // Gerçek E2E ortamı ortak PostgreSQL, Mailpit ve tekil rol koltuklarını
  // paylaşıyor. Dosyaları paralel koşturmak testlerin birbirinin durumunu
  // değiştirmesine ve sahte negatiflere yol açar.
  workers: 1,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'desktop-chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chromium', use: { ...devices['Pixel 7'] }, testMatch: /responsive-accessibility\.spec\.ts/ },
  ],
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 5174',
    url: `${baseURL}/giris`,
    env: { ...process.env, VITE_API_BASE_URL: apiBaseURL },
    reuseExistingServer: false,
    timeout: 120_000,
  },
})
