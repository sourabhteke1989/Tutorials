import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 30000,
  expect: { timeout: 5000 },
  retries: 1,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: 'http://localhost:80',
    extraHTTPHeaders: {},
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'api-gateway',
      testMatch: /.*\.spec\.ts/,
    },
  ],
});
