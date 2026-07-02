import React from 'react';

function RenewalReminderBanner({ expiringPolicies, onDismiss, onRenew }) {
  if (!expiringPolicies || expiringPolicies.length === 0) {
    return null;
  }

  return (
    <div className="renewal-banner" data-testid="renewal-banner">
      <div className="renewal-banner-header">
        <span className="renewal-banner-title">⚠️ Policy Renewal Reminder</span>
        <button
          className="renewal-banner-dismiss"
          onClick={onDismiss}
          data-testid="renewal-banner-dismiss"
        >
          ✕ Dismiss
        </button>
      </div>
      <div className="renewal-banner-list">
        {expiringPolicies.map(policy => (
          <div
            key={policy.id}
            className="renewal-banner-item"
            data-testid={`renewal-banner-item-${policy.id}`}
          >
            <span className="renewal-banner-info">
              {policy.holderName} · {policy.planName}
              <span className="renewal-banner-days">
                &nbsp;expires in {policy.daysUntilExpiry} day{policy.daysUntilExpiry === 1 ? '' : 's'}
              </span>
            </span>
            <button
              className="btn btn-warning renewal-renew-btn"
              onClick={() => onRenew(policy)}
              data-testid={`renew-btn-${policy.id}`}
            >
              Renew
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

export default RenewalReminderBanner;
