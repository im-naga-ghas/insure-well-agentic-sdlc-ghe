import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import Dashboard from './Dashboard';

describe('Dashboard renewal reminder', () => {
  const policies = [
    {
      id: 'POL-1',
      holderName: 'Alex Johnson',
      planName: 'Premium',
      coverageAmount: 100000,
      status: 'active',
      startDate: '2026-01-01',
      endDate: '2026-08-01',
    },
    {
      id: 'POL-2',
      holderName: 'Maria Garcia',
      planName: 'Essential',
      coverageAmount: 80000,
      status: 'active',
      startDate: '2026-01-01',
      endDate: '2026-07-20',
    },
  ];

  const claims = [];

  test('shows reminder banner, links to policy tab, and can be dismissed', () => {
    render(
      <Dashboard
        policies={policies}
        claims={claims}
        expiringPolicies={[policies[1]]}
        onRefresh={() => {}}
        apiBase="http://localhost:8080/api"
      />
    );

    expect(screen.getByTestId('renewal-banner')).toBeTruthy();

    fireEvent.click(screen.getByTestId('renewal-link-POL-2'));
    expect(screen.getByTestId('policy-tab-POL-2').className).toContain('active');

    fireEvent.click(screen.getByTestId('dismiss-renewal-banner'));
    expect(screen.queryByTestId('renewal-banner')).toBeNull();
  });
});
