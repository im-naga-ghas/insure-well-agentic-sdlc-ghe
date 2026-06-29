import { test, expect } from '@playwright/test';

test.describe('Navigation', () => {
  test('navbar is visible with Dashboard and Claims links', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('navbar')).toBeVisible();
    await expect(page.getByTestId('nav-dashboard')).toBeVisible();
    await expect(page.getByTestId('nav-claims')).toBeVisible();
  });

  test('clicking Claims nav link switches to claims page', async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('nav-claims').click();
    await expect(page.getByTestId('claims')).toBeVisible();
  });

  test('clicking Dashboard nav link switches back to dashboard', async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('nav-claims').click();
    await page.getByTestId('nav-dashboard').click();
    await expect(page.getByTestId('dashboard')).toBeVisible();
  });
});

test.describe('Policy Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('dashboard')).toBeVisible();
  });

  test('dashboard loads with policy tabs and stat cards', async ({ page }) => {
    await expect(page.getByTestId('policy-tabs')).toBeVisible();
    await expect(page.getByTestId('stats-row')).toBeVisible();
    await expect(page.getByTestId('stat-total-claims')).toBeVisible();
    await expect(page.getByTestId('stat-pending')).toBeVisible();
    await expect(page.getByTestId('stat-approved')).toBeVisible();
    await expect(page.getByTestId('stat-total-amount')).toBeVisible();
  });

  test('Add Policy button opens the policy modal', async ({ page }) => {
    await page.getByTestId('add-policy-btn').click();
    await expect(page.getByTestId('policy-modal')).toBeVisible();
  });

  test('policy modal closes when Cancel is clicked', async ({ page }) => {
    await page.getByTestId('add-policy-btn').click();
    await expect(page.getByTestId('policy-modal')).toBeVisible();
    await page.getByTestId('cancel-policy-btn').click();
    await expect(page.getByTestId('policy-modal')).not.toBeVisible();
  });

  test('policy modal closes when overlay is clicked', async ({ page }) => {
    await page.getByTestId('add-policy-btn').click();
    await expect(page.getByTestId('policy-modal')).toBeVisible();
    await page.getByTestId('modal-overlay').click({ position: { x: 5, y: 5 } });
    await expect(page.getByTestId('policy-modal')).not.toBeVisible();
  });

  test('saving policy without required fields shows validation error', async ({ page }) => {
    await page.getByTestId('add-policy-btn').click();
    await page.getByTestId('save-policy-btn').click();
    await expect(page.getByTestId('policy-form-error')).toBeVisible();
  });

  test('can add a new policy through the form', async ({ page }) => {
    await page.getByTestId('add-policy-btn').click();
    await page.getByTestId('input-holder-name').fill('Jane Doe');
    await page.getByTestId('input-plan-name').fill('Gold Plan');
    await page.getByTestId('input-coverage-amount').fill('50000');
    await page.getByTestId('select-policy-status').selectOption('active');
    await page.getByTestId('input-start-date').fill('2026-01-01');
    await page.getByTestId('input-end-date').fill('2026-12-31');
    await page.getByTestId('save-policy-btn').click();
    await expect(page.getByTestId('policy-modal')).not.toBeVisible();
  });

  test('recent claims table is visible when a policy is selected', async ({ page }) => {
    await expect(page.getByTestId('recent-claims-table')).toBeVisible();
  });
});

test.describe('Renewal Reminder Banner', () => {
  const mockExpiringPolicy = [
    { id: 'POL-001', holderName: 'Jane Doe', planName: 'Gold Plan', daysUntilExpiry: 10 },
  ];

  test.beforeEach(async ({ page }) => {
    await page.route('**/api/policies/expiring*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockExpiringPolicy),
      });
    });
  });

  test('banner is visible when a policy is expiring soon', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('renewal-banner')).toBeVisible();
    await expect(page.getByTestId('renewal-banner')).toContainText('Jane Doe');
    await expect(page.getByTestId('renewal-banner')).toContainText('Gold Plan');
    await expect(page.getByTestId('renewal-banner')).toContainText('10 days');
  });

  test('banner is not rendered when no policies are expiring', async ({ page }) => {
    await page.route('**/api/policies/expiring*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });
    await page.goto('/');
    await expect(page.getByTestId('renewal-banner')).not.toBeVisible();
  });

  test('clicking dismiss hides the banner without a page reload', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('renewal-banner')).toBeVisible();
    await page.getByTestId('renewal-banner-dismiss').click();
    await expect(page.getByTestId('renewal-banner')).not.toBeVisible();
    // Dashboard is still intact after dismiss
    await expect(page.getByTestId('dashboard')).toBeVisible();
  });

  test('banner shows multiple expiring policies', async ({ page }) => {
    await page.route('**/api/policies/expiring*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: 'POL-001', holderName: 'Jane Doe', planName: 'Gold Plan', daysUntilExpiry: 5 },
          { id: 'POL-002', holderName: 'John Smith', planName: 'Silver Plan', daysUntilExpiry: 18 },
        ]),
      });
    });
    await page.goto('/');
    await expect(page.getByTestId('renewal-banner')).toBeVisible();
    await expect(page.getByTestId('renewal-banner')).toContainText('Jane Doe');
    await expect(page.getByTestId('renewal-banner')).toContainText('John Smith');
    await expect(page.getByTestId('renewal-banner-item-POL-001')).toBeVisible();
    await expect(page.getByTestId('renewal-banner-item-POL-002')).toBeVisible();
  });
});
