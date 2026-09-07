import { defineConfig } from '@playwright/test'
import process from 'node:process'
import { loadEnv } from 'vite'

const testEnv = loadEnv('test', import.meta.dirname, 'PLAYWRIGHT_')

export default defineConfig({
  testDir: './e2e',
  testMatch: ['reviews.spec.ts', 'support.spec.ts', 'reports.spec.ts'],
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  retries: 0,
  use: {
    baseURL: 'http://127.0.0.1:4179',
    channel: process.env.PLAYWRIGHT_CHANNEL || testEnv.PLAYWRIGHT_CHANNEL || undefined,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'mobile-320', use: { viewport: { width: 320, height: 812 }, isMobile: true, hasTouch: true } },
    { name: 'mobile-390', use: { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true } },
    { name: 'desktop', use: { viewport: { width: 1440, height: 1000 } } },
  ],
  webServer: {
    env: { VITE_API_URL: '/api' },
    command: 'npm run dev -- --host 127.0.0.1 --port 4179 --strictPort',
    url: 'http://127.0.0.1:4179',
    reuseExistingServer: false,
    timeout: 30000,
  },
})
