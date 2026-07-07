import React, { useEffect, useState } from 'react';
import axios from 'axios';

const DISMISSED_PREFIX = 'renewal-banner-dismissed-';
const DAY_IN_MS = 24 * 60 * 60 * 1000;

const toUtcDate = (dateString) => {
  const [year, month, day] = dateString.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
};

const getDaysRemaining = (endDate) => {
  const today = new Date();
  const todayUtc = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
  return Math.round((toUtcDate(endDate) - todayUtc) / DAY_IN_MS);
};

function RenewalReminderBanner({ apiBase, onSelectPolicy }) {
  const [expiringPolicies, setExpiringPolicies] = useState([]);

  useEffect(() => {
    let mounted = true;

    const loadExpiringPolicies = async () => {
      try {
        const response = await axios.get(`${apiBase}/policies/expiring?days=30`);
        if (!mounted) {
          return;
        }

        setExpiringPolicies(
          response.data
            .filter(policy => !window.sessionStorage.getItem(`${DISMISSED_PREFIX}${policy.id}`))
            .map(policy => ({
              ...policy,
              daysRemaining: getDaysRemaining(policy.endDate),
            }))
        );
      } catch (err) {
        if (mounted) {
          setExpiringPolicies([]);
        }
      }
    };

    loadExpiringPolicies();

    return () => {
      mounted = false;
    };
  }, [apiBase]);

  const dismissPolicy = (policyId) => {
    window.sessionStorage.setItem(`${DISMISSED_PREFIX}${policyId}`, 'true');
    setExpiringPolicies(current => current.filter(policy => policy.id !== policyId));
  };

  if (expiringPolicies.length === 0) {
    return null;
  }

  return (
    <>
      {expiringPolicies.map(policy => (
        <div className="renewal-banner" key={policy.id}>
          <div className="renewal-banner-content">
            <span>⚠️</span>
            <span>{policy.planName} expires in {policy.daysRemaining} day{policy.daysRemaining === 1 ? '' : 's'}</span>
          </div>
          <div className="renewal-banner-actions">
            <button type="button" className="renewal-banner-link" onClick={() => onSelectPolicy(policy.id)}>
              View Policy
            </button>
            <button
              type="button"
              className="renewal-banner-dismiss"
              aria-label={`Dismiss renewal reminder for ${policy.planName}`}
              onClick={() => dismissPolicy(policy.id)}
            >
              ×
            </button>
          </div>
        </div>
      ))}
    </>
  );
}

export default RenewalReminderBanner;
