import { test, expect, Page } from '@playwright/test';

const policy = {
  id: 'POL-PAY-1',
  holderName: 'Test Holder',
  planName: 'Test Plan',
  coverageAmount: 10000,
  premiumAmount: 25.5,
  currency: 'USD',
  status: 'active',
  startDate: '2026-01-01',
  endDate: '2026-12-31',
  createdAt: '2026-01-01T00:00:00Z',
};

async function mockPaymentApi(page: Page, currentStatus: () => string | null) {
  await page.route('**/api/policies', route => route.fulfill({ json: [policy] }));
  await page.route('**/api/claims', route => route.fulfill({ json: [] }));
  await page.route('**/api/policies/POL-PAY-1/payments/latest', route => {
    const status = currentStatus();
    return status
      ? route.fulfill({ json: { id: 'payment-1', amount: 25.5, currency: 'USD', status } })
      : route.fulfill({ status: 404 });
  });
  await page.route('**/api/policies/POL-PAY-1/payments/checkout', async route => {
    expect(route.request().method()).toBe('POST');
    expect(route.request().postData()).toBeFalsy();
    await route.fulfill({
      status: 201,
      json: { paymentId: 'payment-1', checkoutUrl: 'https://checkout.stripe.com/c/pay/cs_test_123' },
    });
  });
  await page.route('https://checkout.stripe.com/**', route => route.fulfill({
    contentType: 'text/html',
    body: '<a href="http://localhost:3000/?payment=cancelled&policyId=POL-PAY-1">Cancel checkout</a>',
  }));
}

test.describe('Policy premium payments', () => {
  test('checkout redirect alone does not display a confirmed payment', async ({ page }) => {
    await mockPaymentApi(page, () => null);
    await page.goto('/');
    await page.getByTestId('pay-premium-btn').click();
    await expect(page).toHaveURL(/checkout\.stripe\.com/);
    await page.getByRole('link', { name: 'Cancel checkout' }).click();
    await expect(page.getByTestId('checkout-message')).toContainText('No payment has been confirmed');
    await expect(page.getByTestId('payment-status')).toContainText('No payment recorded');
    await expect(page.getByTestId('payment-status')).not.toContainText('Payment confirmed');
  });

  test('shows confirmation only when the payment API reports success', async ({ page }) => {
    await mockPaymentApi(page, () => 'SUCCEEDED');
    await page.goto('/');
    await expect(page.getByTestId('payment-status')).toContainText('Payment confirmed');
  });
});
