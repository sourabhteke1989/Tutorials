import { test, expect, APIRequestContext } from '@playwright/test';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:80';
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8081';

// Test accounts from DataStore
const TEST_USERS = {
  admin: { username: 'admin@acme.com', password: 'admin123', tenantId: 'acme' },
  jane: { username: 'jane@acme.com', password: 'jane123', tenantId: 'acme' },
  hans: { username: 'hans@globex.com', password: 'hans123', tenantId: 'globex' },
};

async function loginViaBackend(request: APIRequestContext, user = TEST_USERS.admin): Promise<string> {
  const response = await request.post(`${BACKEND_URL}/auth/internal-auth/login`, {
    data: { username: user.username, password: user.password, tenant_id: user.tenantId },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.access_token).toBeTruthy();
  return body.access_token;
}

test.describe('Backend Service Health', () => {
  test('should return health status', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/crud/health`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.status).toBe('UP');
  });

  test('should return version', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/version`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.version).toBe('1.0.0');
  });
});

test.describe('Authentication Flow', () => {
  test('should login successfully with valid credentials', async ({ request }) => {
    const response = await request.post(`${BACKEND_URL}/auth/internal-auth/login`, {
      data: { username: 'admin@acme.com', password: 'admin123', tenant_id: 'acme' },
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.access_token).toBeTruthy();
    expect(body.token_type).toBe('Bearer');
    expect(body.user.email).toBe('admin@acme.com');
  });

  test('should reject invalid password', async ({ request }) => {
    const response = await request.post(`${BACKEND_URL}/auth/internal-auth/login`, {
      data: { username: 'admin@acme.com', password: 'wrong', tenant_id: 'acme' },
    });
    expect(response.status()).toBe(401);
  });

  test('should reject unknown user', async ({ request }) => {
    const response = await request.post(`${BACKEND_URL}/auth/internal-auth/login`, {
      data: { username: 'nobody@test.com', password: 'pass', tenant_id: 'acme' },
    });
    expect(response.status()).toBe(401);
  });

  test('should reject tenant mismatch', async ({ request }) => {
    const response = await request.post(`${BACKEND_URL}/auth/internal-auth/login`, {
      data: { username: 'admin@acme.com', password: 'admin123', tenant_id: 'globex' },
    });
    expect(response.status()).toBe(401);
  });

  test('should initiate auth flow', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/auth/internal-auth/initiate`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.auth_mode).toBe('internal');
  });

  test('should identify existing user', async ({ request }) => {
    const response = await request.post(`${BACKEND_URL}/auth/internal-auth/identify`, {
      data: { email: 'admin@acme.com' },
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.identified).toBe(true);
  });
});

test.describe('User Endpoints', () => {
  test('should get current user with valid token', async ({ request }) => {
    const token = await loginViaBackend(request);
    const response = await request.get(`${BACKEND_URL}/user/me`, {
      headers: { 'X-User-ID': '550e8400-e29b-41d4-a716-446655440001' },
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.user_name).toBe('John Admin');
  });

  test('should list users by tenant', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/user/list`, {
      headers: { 'X-Tenant-ID': 'acme' },
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.length).toBe(2);
  });

  test('should find user by username', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/user/by-username?username=admin@acme.com`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.email).toBe('admin@acme.com');
  });
});

test.describe('Organization Endpoints', () => {
  test('should get organization details', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/organization/details?tenant_id=acme`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.name).toBe('Acme Corporation');
  });

  test('should check tenant exists', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/organization/exists/acme`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.exists).toBe(true);
  });

  test('should check unknown tenant does not exist', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/organization/exists/unknown`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.exists).toBe(false);
  });
});

test.describe('Misc Endpoints', () => {
  test('should return applications list', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/applications`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.length).toBe(3);
  });

  test('should return permissions', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/permissions`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.length).toBe(3);
  });

  test('should check user access', async ({ request }) => {
    const response = await request.get(
      `${BACKEND_URL}/user-access-control/has-access?user_id=u1&resource_id=r1&permission=READ`
    );
    expect(response.status()).toBe(200);
  });
});

test.describe('CRUD Endpoints', () => {
  test('should list entities', async ({ request }) => {
    const response = await request.get(`${BACKEND_URL}/crud/entities`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body).toContain('user');
  });

  test('should perform CRUD operation', async ({ request }) => {
    const response = await request.post(`${BACKEND_URL}/crud`, {
      data: { entity: 'user', action: 'create' },
    });
    expect(response.status()).toBe(200);
  });
});

test.describe('Frontend Login Flow', () => {
  test('should load login page', async ({ page }) => {
    await page.goto('http://localhost:3000/login');
    await expect(page.locator('h2')).toContainText('Login');
  });

  test('should show test account info', async ({ page }) => {
    await page.goto('http://localhost:3000/login');
    await expect(page.locator('body')).toContainText('admin@acme.com');
  });

  test('should login and navigate to dashboard', async ({ page }) => {
    await page.goto('http://localhost:3000/login');
    await page.selectOption('select', 'acme');
    await page.fill('input[type="email"]', 'admin@acme.com');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/dashboard/, { timeout: 10000 });
  });

  test('should reject invalid login', async ({ page }) => {
    await page.goto('http://localhost:3000/login');
    await page.selectOption('select', 'acme');
    await page.fill('input[type="email"]', 'admin@acme.com');
    await page.fill('input[type="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');
    await expect(page.locator('.error, [class*="error"]')).toBeVisible({ timeout: 5000 });
  });

  test('should redirect to login when not authenticated', async ({ page }) => {
    await page.goto('http://localhost:3000/dashboard');
    await expect(page).toHaveURL(/login/, { timeout: 5000 });
  });
});
