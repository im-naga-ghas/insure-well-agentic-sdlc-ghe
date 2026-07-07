import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import axios from 'axios';
import RenewalReminderBanner from './RenewalReminderBanner';

jest.mock('axios');

const futureDate = (days) => {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
};

describe('RenewalReminderBanner', () => {
  const apiBase = 'http://localhost:8080/api';

  beforeEach(() => {
    jest.clearAllMocks();
    window.sessionStorage.clear();
  });

  test('renders banner when API returns expiring policies', async () => {
    axios.get.mockResolvedValue({
      data: [{ id: 'POL-1', planName: 'Gold Plan', endDate: futureDate(10) }],
    });

    render(<RenewalReminderBanner apiBase={apiBase} onSelectPolicy={jest.fn()} />);

    expect(await screen.findByText('Gold Plan expires in 10 days')).toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledWith(`${apiBase}/policies/expiring?days=30`);
  });

  test('banner is hidden when API returns empty list', async () => {
    axios.get.mockResolvedValue({ data: [] });
    const { container } = render(<RenewalReminderBanner apiBase={apiBase} onSelectPolicy={jest.fn()} />);

    await waitFor(() => expect(axios.get).toHaveBeenCalledWith(`${apiBase}/policies/expiring?days=30`));
    expect(container.firstChild).toBeNull();
  });

  test('clicking dismiss hides the banner', async () => {
    axios.get.mockResolvedValue({
      data: [{ id: 'POL-1', planName: 'Gold Plan', endDate: futureDate(10) }],
    });

    render(<RenewalReminderBanner apiBase={apiBase} onSelectPolicy={jest.fn()} />);

    fireEvent.click(await screen.findByRole('button', { name: 'Dismiss renewal reminder for Gold Plan' }));

    await waitFor(() => expect(screen.queryByText('Gold Plan expires in 10 days')).not.toBeInTheDocument());
    expect(window.sessionStorage.getItem('renewal-banner-dismissed-POL-1')).toBe('true');
  });

  test('view policy click calls the onSelectPolicy callback with the correct policy ID', async () => {
    const onSelectPolicy = jest.fn();
    axios.get.mockResolvedValue({
      data: [{ id: 'POL-1', planName: 'Gold Plan', endDate: futureDate(10) }],
    });

    render(<RenewalReminderBanner apiBase={apiBase} onSelectPolicy={onSelectPolicy} />);

    fireEvent.click(await screen.findByRole('button', { name: 'View Policy' }));

    expect(onSelectPolicy).toHaveBeenCalledWith('POL-1');
  });
});
