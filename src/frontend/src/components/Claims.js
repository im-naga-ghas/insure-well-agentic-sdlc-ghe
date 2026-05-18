import React, { useState } from 'react';
import axios from 'axios';
import '../styles/Claims.css';

function Claims({ policies, claims, onRefresh, apiBase }) {
  const [filterPolicyId, setFilterPolicyId] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    policy_id: policies[0]?.id || '',
    amount: '',
    description: '',
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const filteredClaims = filterPolicyId
    ? claims.filter(c => c.policyId === filterPolicyId)
    : claims;

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmitClaim = async (e) => {
    e.preventDefault();
    setError('');

    if (!formData.policy_id || !formData.amount || !formData.description) {
      setError('All fields are required');
      return;
    }

    if (parseFloat(formData.amount) <= 0) {
      setError('Amount must be greater than 0');
      return;
    }

    try {
      setSubmitting(true);
      const payload = new FormData();
      payload.append('policy_id', formData.policy_id);
      payload.append('amount', String(parseFloat(formData.amount)));
      payload.append('description', formData.description);

      await axios.post(`${apiBase}/claims`, payload);
      setShowForm(false);
      setFormData({
        policy_id: policies[0]?.id || '',
        amount: '',
        description: '',
      });
      onRefresh();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to submit claim');
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusChange = async (claimId, newStatus) => {
    try {
      await axios.patch(`${apiBase}/claims/${claimId}/status`, { status: newStatus });
      onRefresh();
    } catch (err) {
      alert('Failed to update claim status');
    }
  };

  const handleDeleteClaim = async (claimId) => {
    if (window.confirm('Delete this claim?')) {
      try {
        await axios.delete(`${apiBase}/claims/${claimId}`);
        onRefresh();
      } catch (err) {
        alert('Failed to delete claim');
      }
    }
  };

  return (
    <div className="claims-container" data-testid="claims">
      <div className="page-header">
        <div>
          <h1>Claims</h1>
          <p>Submit and track your insurance claims</p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => setShowForm(!showForm)}
          data-testid="new-claim-btn"
        >
          {showForm ? '✕ Cancel' : '+ New Claim'}
        </button>
      </div>

      {showForm && (
        <div className="claim-form-card">
          <h2>Submit New Claim</h2>
          {error && <div className="alert alert-error" data-testid="claim-form-error">{error}</div>}
          <form onSubmit={handleSubmitClaim} data-testid="claim-form">
            <div className="form-row">
              <div className="form-group">
                <label>Policy</label>
                <select
                  name="policy_id"
                  value={formData.policy_id}
                  onChange={handleInputChange}
                  required
                  data-testid="select-claim-policy"
                >
                  {policies.map(p => (
                    <option key={p.id} value={p.id}>
                      {p.id} — {p.holderName}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Claim Amount (USD)</label>
                <input
                  type="number"
                  name="amount"
                  value={formData.amount}
                  onChange={handleInputChange}
                  placeholder="e.g. 1500.00"
                  min="0.01"
                  step="0.01"
                  required
                  data-testid="input-claim-amount"
                />
              </div>
            </div>
            <div className="form-group">
              <label>Description</label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                placeholder="Describe the medical service or expense…"
                rows="3"
                required
                data-testid="input-claim-description"
              />
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={submitting} data-testid="submit-claim-btn">
                {submitting ? 'Submitting…' : 'Submit Claim'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="section">
        <div className="section-header">
          <h2>Submitted Claims</h2>
          <div className="filter-wrap">
            <label>Filter by policy:</label>
            <select value={filterPolicyId} onChange={e => setFilterPolicyId(e.target.value)} data-testid="filter-policy">
              <option value="">All Policies</option>
              {policies.map(p => (
                <option key={p.id} value={p.id}>
                  {p.id} — {p.holderName}
                </option>
              ))}
            </select>
          </div>
          <span className="claims-count" data-testid="claims-count">{filteredClaims.length} claim{filteredClaims.length !== 1 ? 's' : ''}</span>
        </div>

        {filteredClaims.length === 0 ? (
          <p className="empty">No claims</p>
        ) : (
          <table className="claims-table" data-testid="claims-table">
            <thead>
              <tr>
                <th>Claim ID</th>
                <th>Policy</th>
                <th>Description</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Submitted</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredClaims.map(claim => (
                <tr key={claim.id} data-testid={`claim-row-${claim.id}`}>
                  <td className="mono">{claim.id}</td>
                  <td className="mono">{claim.policyId}</td>
                  <td>{claim.description}</td>
                  <td>${claim.amount.toLocaleString()}</td>
                  <td>
                    <select
                      value={claim.status}
                      onChange={e => handleStatusChange(claim.id, e.target.value)}
                      className={`status-select status-${claim.status.toLowerCase()}`}
                      data-testid={`claim-status-${claim.id}`}
                    >
                      <option value="Pending">Pending</option>
                      <option value="Approved">Approved</option>
                      <option value="Rejected">Rejected</option>
                    </select>
                  </td>
                  <td>{new Date(claim.submittedAt).toLocaleDateString()}</td>
                  <td>
                    <button
                      className="delete-btn-small"
                      onClick={() => handleDeleteClaim(claim.id)}
                      data-testid={`delete-claim-${claim.id}`}
                    >
                      🗑️
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default Claims;
