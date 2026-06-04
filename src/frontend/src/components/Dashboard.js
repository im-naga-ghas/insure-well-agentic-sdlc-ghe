import React, { useEffect, useState } from 'react';
import axios from 'axios';
import '../styles/Dashboard.css';

function Dashboard({ policies, claims, onRefresh, apiBase }) {
  const [selectedPolicyId, setSelectedPolicyId] = useState(policies[0]?.id || null);
  const [showPolicyModal, setShowPolicyModal] = useState(false);
  const [modalMode, setModalMode] = useState('add');
  const [formData, setFormData] = useState({
    holderName: '',
    planName: '',
    coverageAmount: '',
    status: 'active',
    startDate: '',
    endDate: '',
  });
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  const selectedPolicy = policies.find(p => p.id === selectedPolicyId);
  const policyClaims = claims.filter(c => c.policyId === selectedPolicyId);
  const pendingCount = policyClaims.filter(c => c.status === 'Pending').length;
  const approvedCount = policyClaims.filter(c => c.status === 'Approved').length;
  const totalClaimed = policyClaims.reduce((sum, c) => sum + c.amount, 0);

  useEffect(() => {
    if (policies.length === 0) {
      setSelectedPolicyId(null);
      return;
    }

    if (!selectedPolicyId || !policies.some(p => p.id === selectedPolicyId)) {
      setSelectedPolicyId(policies[0].id);
    }
  }, [policies, selectedPolicyId]);

  const parseDate = (value) => {
    const datePattern = /^\d{4}-\d{2}-\d{2}$/;
    if (!datePattern.test(value)) {
      return null;
    }

    const parsed = new Date(`${value}T00:00:00Z`);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  };

  const getPolicyValidationError = (payload) => {
    if (!payload.holderName || !payload.planName || !payload.coverageAmount || !payload.startDate || !payload.endDate) {
      return 'All fields are required';
    }

    if (!Number.isFinite(payload.coverageAmount) || payload.coverageAmount <= 0) {
      return 'Coverage amount must be greater than 0';
    }

    if (!['active', 'inactive'].includes(payload.status)) {
      return 'Status must be active or inactive';
    }

    const start = parseDate(payload.startDate);
    const end = parseDate(payload.endDate);
    if (!start || !end) {
      return 'Dates must be in YYYY-MM-DD format';
    }

    if (end < start) {
      return 'End date must be on or after start date';
    }

    return '';
  };

  const openAddModal = () => {
    setModalMode('add');
    setFormData({
      holderName: '',
      planName: '',
      coverageAmount: '',
      status: 'active',
      startDate: '',
      endDate: '',
    });
    setError('');
    setShowPolicyModal(true);
  };

  const openEditModal = (policy) => {
    setModalMode('edit');
    setFormData({
      holderName: policy.holderName,
      planName: policy.planName,
      coverageAmount: policy.coverageAmount,
      status: policy.status,
      startDate: policy.startDate,
      endDate: policy.endDate,
    });
    setError('');
    setShowPolicyModal(true);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSavePolicy = async (e) => {
    e.preventDefault();
    setActionError('');

    const payload = {
      holderName: formData.holderName.trim(),
      planName: formData.planName.trim(),
      coverageAmount: Number(formData.coverageAmount),
      status: formData.status,
      startDate: formData.startDate.trim(),
      endDate: formData.endDate.trim(),
    };

    const validationError = getPolicyValidationError(payload);
    if (validationError) {
      setError(validationError);
      return;
    }

    try {
      if (modalMode === 'add') {
        await axios.post(`${apiBase}/policies`, payload);
      } else {
        await axios.patch(`${apiBase}/policies/${selectedPolicyId}`, payload);
      }
      setShowPolicyModal(false);
      setError('');
      onRefresh();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save policy');
    }
  };

  const handleDeletePolicy = async (id, name) => {
    setActionError('');
    if (window.confirm(`Delete policy for "${name}"? All claims will be deleted.`)) {
      try {
        await axios.delete(`${apiBase}/policies/${id}`);
        onRefresh();
      } catch (err) {
        setActionError(err.response?.data?.error || 'Failed to delete policy');
      }
    }
  };

  return (
    <div className="dashboard-container" data-testid="dashboard">
      <div className="page-header">
        <div>
          <h1>Policy Dashboard</h1>
          <p data-testid="policy-count">{policies.length} polic{policies.length === 1 ? 'y' : 'ies'} on your account</p>
        </div>
        <button className="btn btn-primary" onClick={openAddModal} data-testid="add-policy-btn">
          + Add Policy
        </button>
      </div>

      {actionError && <div className="alert alert-error" data-testid="policy-action-error">{actionError}</div>}

      {policies.length === 0 && (
        <div className="section" data-testid="dashboard-empty-state">
          <p className="empty">No policies yet. Add your first policy to start tracking coverage and claims.</p>
        </div>
      )}

      {selectedPolicy && (
        <>
          <div className="policy-tabs" data-testid="policy-tabs">
            {policies.map(policy => (
              <div
                key={policy.id}
                className={`policy-tab ${selectedPolicyId === policy.id ? 'active' : ''}`}
                onClick={() => setSelectedPolicyId(policy.id)}
                data-testid={`policy-tab-${policy.id}`}
              >
                <span>{policy.holderName}</span>
                <span className="policy-id">{policy.id}</span>
                <div className="tab-actions">
                  <button
                    className="edit-btn"
                    onClick={(e) => { e.stopPropagation(); openEditModal(policy); }}
                    data-testid={`edit-policy-btn-${policy.id}`}
                  >
                    ✏️
                  </button>
                  <button
                    className="delete-btn"
                    onClick={(e) => { e.stopPropagation(); handleDeletePolicy(policy.id, policy.holderName); }}
                    data-testid={`delete-policy-btn-${policy.id}`}
                  >
                    🗑️
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="policy-card" data-testid="policy-card">
            <div className="policy-header">
              <div>
                <p className="policy-plan">{selectedPolicy.planName}</p>
                <p className="policy-id">Policy ID: {selectedPolicy.id}</p>
              </div>
              <span className={`badge ${selectedPolicy.status === 'active' ? 'success' : 'neutral'}`}>
                {selectedPolicy.status.toUpperCase()}
              </span>
            </div>
            <div className="policy-details">
              <div className="detail">
                <span className="label">Coverage Amount</span>
                <span className="value">${selectedPolicy.coverageAmount.toLocaleString()}</span>
              </div>
              <div className="detail">
                <span className="label">Policy Holder</span>
                <span className="value">{selectedPolicy.holderName}</span>
              </div>
              <div className="detail">
                <span className="label">Start Date</span>
                <span className="value">{selectedPolicy.startDate}</span>
              </div>
              <div className="detail">
                <span className="label">End Date</span>
                <span className="value">{selectedPolicy.endDate}</span>
              </div>
            </div>
          </div>

          <div className="stats-row" data-testid="stats-row">
            <div className="stat-card" data-testid="stat-total-claims">
              <span className="stat-value">{policyClaims.length}</span>
              <span className="stat-label">Total Claims</span>
            </div>
            <div className="stat-card" data-testid="stat-pending">
              <span className="stat-value warning">{pendingCount}</span>
              <span className="stat-label">Pending</span>
            </div>
            <div className="stat-card" data-testid="stat-approved">
              <span className="stat-value success">{approvedCount}</span>
              <span className="stat-label">Approved</span>
            </div>
            <div className="stat-card" data-testid="stat-total-amount">
              <span className="stat-value">${totalClaimed.toLocaleString()}</span>
              <span className="stat-label">Total Claimed</span>
            </div>
          </div>

          <div className="section">
            <h2>Recent Claims</h2>
            {policyClaims.length === 0 ? (
              <p className="empty">No claims yet</p>
            ) : (
              <table className="claims-table" data-testid="recent-claims-table">
                <thead>
                  <tr>
                    <th>Claim ID</th>
                    <th>Description</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Submitted</th>
                  </tr>
                </thead>
                <tbody>
                  {policyClaims.slice(0, 5).map(claim => (
                    <tr key={claim.id}>
                      <td className="mono">{claim.id}</td>
                      <td>{claim.description}</td>
                      <td>${claim.amount.toLocaleString()}</td>
                      <td>
                        <span className={`status-badge ${claim.status.toLowerCase()}`}>
                          {claim.status}
                        </span>
                      </td>
                      <td>{new Date(claim.submittedAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {showPolicyModal && (
        <div className="modal-overlay" onClick={() => setShowPolicyModal(false)} data-testid="modal-overlay">
          <div className="modal-content" onClick={e => e.stopPropagation()} data-testid="policy-modal">
            <h2>{modalMode === 'add' ? 'Add Policy' : 'Edit Policy'}</h2>
            {error && <div className="alert alert-error" data-testid="policy-form-error">{error}</div>}
            <form onSubmit={handleSavePolicy} data-testid="policy-form">
              <div className="form-group">
                <label>Holder Name</label>
                <input
                  type="text"
                  name="holderName"
                  value={formData.holderName}
                  onChange={handleInputChange}
                  required
                  data-testid="input-holder-name"
                />
              </div>
              <div className="form-group">
                <label>Plan Name</label>
                <input
                  type="text"
                  name="planName"
                  value={formData.planName}
                  onChange={handleInputChange}
                  required
                  data-testid="input-plan-name"
                />
              </div>
              <div className="form-group">
                <label>Coverage Amount</label>
                <input
                  type="number"
                  name="coverageAmount"
                  value={formData.coverageAmount}
                  onChange={handleInputChange}
                  required
                  data-testid="input-coverage-amount"
                />
              </div>
              <div className="form-group">
                <label>Status</label>
                <select
                  name="status"
                  value={formData.status}
                  onChange={handleInputChange}
                  data-testid="select-policy-status"
                >
                  <option value="active">Active</option>
                  <option value="inactive">Inactive</option>
                </select>
              </div>
              <div className="form-group">
                <label>Start Date</label>
                <input
                  type="text"
                  name="startDate"
                  value={formData.startDate}
                  onChange={handleInputChange}
                  placeholder="YYYY-MM-DD"
                  required
                  data-testid="input-start-date"
                />
              </div>
              <div className="form-group">
                <label>End Date</label>
                <input
                  type="text"
                  name="endDate"
                  value={formData.endDate}
                  onChange={handleInputChange}
                  placeholder="YYYY-MM-DD"
                  required
                  data-testid="input-end-date"
                />
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowPolicyModal(false)} data-testid="cancel-policy-btn">
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" data-testid="save-policy-btn">
                  {modalMode === 'add' ? 'Add Policy' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;
