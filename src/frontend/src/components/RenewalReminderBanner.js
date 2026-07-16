import React, { useState } from 'react';
import axios from 'axios';

function RenewalReminderBanner({ policy, apiBase }) {
  const [isDownloading, setIsDownloading] = useState(false);

  if (!policy) return null;

  const getRemainingDays = (endDateStr) => {
    if (!endDateStr) return 9999;
    const parts = endDateStr.split('-');
    if (parts.length !== 3) return 9999;
    const year = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1;
    const day = parseInt(parts[2], 10);
    
    const endDateUtc = Date.UTC(year, month, day);
    
    const now = new Date();
    const nowUtc = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
    
    const diffMs = endDateUtc - nowUtc;
    return Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  };

  const diffDays = getRemainingDays(policy.endDate);

  const isExpired = policy.status === 'inactive' || diffDays < 0;
  const isExpiringSoon = policy.status === 'active' && diffDays >= 0 && diffDays <= 180;

  if (!isExpired && !isExpiringSoon) {
    return null;
  }

  const handleRenew = () => {
    alert('Renewal form coming soon!');
  };

  const handleDownload = async () => {
    setIsDownloading(true);
    try {
      const response = await axios.get(`${apiBase}/policies/${policy.id}/renewal-reminder/pdf`, {
        responseType: 'blob',
      });
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `renewal-reminder-${policy.id}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading renewal reminder:', error);
      alert('Failed to download renewal reminder PDF');
    } finally {
      setIsDownloading(false);
    }
  };

  const className = `renewal-reminder-banner ${isExpired ? 'expired' : 'expiring'}`;
  const icon = isExpired ? '⚠️' : '🔔';
  const title = isExpired ? 'Policy Expired' : 'Policy Expiring Soon';
  const text = isExpired
    ? 'This policy is no longer active. Please renew immediately to remain covered.'
    : `This policy will expire in ${diffDays} day${diffDays === 1 ? '' : 's'}. Action required.`;

  return (
    <div className={className} data-testid="renewal-reminder-banner">
      <div className="banner-content">
        <span className="banner-icon">{icon}</span>
        <div className="banner-text-wrapper">
          <strong className="banner-title">{title}</strong>
          <span className="banner-text">{text}</span>
        </div>
      </div>
      <div className="banner-actions">
        <button onClick={handleRenew} className="btn-renew">
          Renew
        </button>
        <button
          onClick={handleDownload}
          disabled={isDownloading}
          className="btn-download-reminder"
          data-testid={`download-reminder-btn-${policy.id}`}
          aria-label={`Download renewal reminder for ${policy.holderName}`}
        >
          {isDownloading ? '⏳ Loading...' : '📄 Download Reminder'}
        </button>
      </div>
    </div>
  );
}

export default RenewalReminderBanner;
