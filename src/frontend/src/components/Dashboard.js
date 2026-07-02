import React, { useState, useEffect } from 'react';
import axios from 'axios';
import '../styles/Dashboard.css';
import RenewalReminderBanner from './RenewalReminderBanner';

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
  const [expiringPolicies, setExpiringPolicies] = useState([]);
  const [bannerDismissed, setBannerDismissed] = useState(false);

  useEffect(() => {
    const fetchExpiringPolicies = async () => {
      try {
        const res = await axios.get(`${apiBase}/policies/expiring?days=30`);
        setExpiringPolicies(res.data);
      } catch (err) {
        // Banner is non-critical; ignore fetch errors
      }
    };
    fetchExpiringPolicies();
  }, [apiBase, policies]);

  const selectedPolicy = policies.find(p => p.id === selectedPolicyId);
  const policyClaims = claims.filter(c => c.policyId === selectedPolicyId);
  const pendingCount = policyClaims.filter(c => c.status === 'Pending').length;
  const approvedCount = policyClaims.filter(c => c.status === 'Approved').length;
  const totalClaimed = policyClaims.reduce((sum, c) => sum + c.amount, 0);

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

    if (!formData.holderName || !formData.planName || !formData.coverageAmount) {
      setError('All fields are required');
      return;
    }

    try {
      if (modalMode === 'add') {
        await axios.post(`${apiBase}/policies`, formData);
      } else {
        await axios.patch(`${apiBase}/policies/${selectedPolicyId}`, formData);
      }
      setShowPolicyModal(false);
      onRefresh();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save policy');
    }
  };

  const handleDeletePolicy = async (id, name) => {
    if (window.confirm(`Delete policy for "${name}"? All claims will be deleted.`)) {
      try {
        await axios.delete(`${apiBase}/policies/${id}`);
        onRefresh();
      } catch (err) {
        alert('Failed to delete policy');
      }
    }
  };

  const handleRenew = (policy) => {
    setSelectedPolicyId(policy.id);
    openEditModal(policy);
  };

  const handleBannerDismiss = () => {
    setBannerDismissed(true);
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

      {!bannerDismissed && (
        <RenewalReminderBanner
          expiringPolicies={expiringPolicies}
          onDismiss={handleBannerDismiss}
          onRenew={handleRenew}
        />
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
