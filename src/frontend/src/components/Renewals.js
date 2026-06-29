import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import '../styles/Renewals.css';

function Renewals({ apiBase }) {
  const [renewals, setRenewals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  const fetchRenewals = useCallback(async () => {
    try {
      setLoading(true);
      const resp = await axios.get(`${apiBase}/renewals/upcoming`);
      setRenewals(resp.data);
      setError(null);
    } catch {
      setError('Failed to load upcoming renewals.');
    } finally {
      setLoading(false);
    }
  }, [apiBase]);

  useEffect(() => {
    fetchRenewals();
  }, [fetchRenewals]);

  const handleRemind = async (policyId) => {
    try {
      await axios.post(`${apiBase}/renewals/${policyId}/remind`);
      showToast(`Reminder sent for policy ${policyId}`, 'success');
      fetchRenewals();
    } catch {
      showToast('Failed to send reminder', 'error');
    }
  };

  const handleDownloadPdf = async (policyId) => {
    try {
      const resp = await axios.get(`${apiBase}/renewals/${policyId}/pdf`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `renewal-notice-${policyId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      showToast('Failed to download PDF', 'error');
    }
  };

  const showToast = (message, type) => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3500);
  };

  const getExpiryClass = (days) => {
    if (days <= 7) return 'expiry-critical';
    if (days <= 30) return 'expiry-warning';
    return '';
  };

  return (
    <div className="renewals-container" data-testid="renewals-page">
      <div className="renewals-header">
        <h1>Renewals</h1>
        {!loading && !error && (
          <p className="renewals-subtitle" data-testid="renewals-count">
            {renewals.length > 0
              ? `${renewals.length} polic${renewals.length === 1 ? 'y' : 'ies'} expiring within the next 30 days`
              : 'No policies are due for renewal within the next 30 days.'}
          </p>
        )}
      </div>

      {toast && (
        <div className={`renewals-toast renewals-toast--${toast.type}`} data-testid="toast">
          {toast.message}
        </div>
      )}

      {loading && <div className="renewals-loading">Loading...</div>}
      {error && <div className="renewals-error">{error}</div>}

      {!loading && !error && renewals.length === 0 && (
        <div className="renewals-empty" data-testid="renewals-empty">
          No policies are due for renewal within the next 30 days.
        </div>
      )}

      {!loading && !error && renewals.length > 0 && (
        <table className="renewals-table" data-testid="renewals-table">
          <thead>
            <tr>
              <th>Policy ID</th>
              <th>Holder Name</th>
              <th>Expires</th>
              <th>Days Left</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {renewals.map((r) => (
              <tr key={r.policyId} data-testid={`renewal-row-${r.policyId}`}>
                <td className="mono">{r.policyId}</td>
                <td>{r.holderName}</td>
                <td className={getExpiryClass(r.daysUntilExpiry)}>
                  {r.endDate}
                  {r.daysUntilExpiry <= 7 ? ' 🔴' : ' 🟡'}
                </td>
                <td className={getExpiryClass(r.daysUntilExpiry)}>
                  {r.daysUntilExpiry} day{r.daysUntilExpiry !== 1 ? 's' : ''}
                </td>
                <td className="renewals-actions">
                  <button
                    className="btn btn-sm btn-remind"
                    onClick={() => handleRemind(r.policyId)}
                    data-testid={`remind-btn-${r.policyId}`}
                  >
                    Remind
                  </button>
                  <button
                    className="btn btn-sm btn-pdf"
                    onClick={() => handleDownloadPdf(r.policyId)}
                    data-testid={`pdf-btn-${r.policyId}`}
                  >
                    ⬇ PDF
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default Renewals;
