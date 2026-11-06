import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
    testDir: './tests',
    timeout: 90_000,
    expect: { timeout: 20_000 },
    // flows share server state (links, models), so keep them ordered
    workers: 1,
    fullyParallel: false,
    reporter: [['list']],
    use: {
        baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:3000',
        trace: 'retain-on-failure',
        acceptDownloads: true
    },
    projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], channel: process.env.E2E_CHANNEL || 'chrome' } }]
})
