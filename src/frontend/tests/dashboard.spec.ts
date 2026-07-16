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

  test('selecting POL-2024-001 displays renewal reminder banner and download button in both banner and card', async ({ page }) => {
    await page.getByTestId('policy-tab-POL-2024-001').click();
    await expect(page.getByTestId('renewal-reminder-banner')).toBeVisible();
    
    // Download button in renewal reminder banner
    const bannerDownloadBtn = page.getByTestId('renewal-reminder-banner').getByTestId('download-reminder-btn-POL-2024-001');
    await expect(bannerDownloadBtn).toBeVisible();

    // Download button in policy card
    const cardDownloadBtn = page.getByTestId('policy-card').getByTestId('download-reminder-btn-POL-2024-001');
    await expect(cardDownloadBtn).toBeVisible();
  });

  test('selecting POL-2023-009 displays renewal reminder banner with expired message and download button in banner, but not in card', async ({ page }) => {
    await page.getByTestId('policy-tab-POL-2023-009').click();
    await expect(page.getByTestId('renewal-reminder-banner')).toBeVisible();
    await expect(page.getByTestId('renewal-reminder-banner')).toContainText('Policy Expired');
    await expect(page.getByTestId('renewal-reminder-banner')).toContainText('This policy is no longer active. Please renew immediately to remain covered.');

    // Download button in renewal reminder banner
    const bannerDownloadBtn = page.getByTestId('renewal-reminder-banner').getByTestId('download-reminder-btn-POL-2023-009');
    await expect(bannerDownloadBtn).toBeVisible();

    // Download button should NOT be in the policy card (since it's inactive)
    const cardDownloadBtn = page.getByTestId('policy-card').getByTestId('download-reminder-btn-POL-2023-009');
    await expect(cardDownloadBtn).not.toBeVisible();
  });

  test('selecting POL-2024-002 does not display renewal reminder banner, but displays download button inside the policy card', async ({ page }) => {
    await page.getByTestId('policy-tab-POL-2024-002').click();
    await expect(page.getByTestId('renewal-reminder-banner')).not.toBeVisible();

    // Download button inside the policy card
    const cardDownloadBtn = page.getByTestId('policy-card').getByTestId('download-reminder-btn-POL-2024-002');
    await expect(cardDownloadBtn).toBeVisible();
  });
});
