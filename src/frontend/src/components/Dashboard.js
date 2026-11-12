import React, { useEffect, useState } from 'react';
import axios from 'axios';
import '../styles/Dashboard.css';

function Dashboard({ policies, claims, onRefresh, apiBase }) {
  const [selectedPolicyId, setSelectedPolicyId] = useState(
    new URLSearchParams(window.location.search).get('policyId') || policies[0]?.id || null
  );
  const [showPolicyModal, setShowPolicyModal] = useState(false);
  const [modalMode, setModalMode] = useState('add');
  const [billingData, setBillingData] = useState({ premiumAmount: '', ownerPrincipal: '' });
  const [formData, setFormData] = useState({
    holderName: '',
    planName: '',
    coverageAmount: '',
    status: 'active',
    startDate: '',
    endDate: '',
  });
  const [error, setError] = useState('');
  const [billingError, setBillingError] = useState('');
  const [payment, setPayment] = useState(null);
  const [paymentError, setPaymentError] = useState('');
  const [checkoutMessage] = useState(() => {
    const result = new URLSearchParams(window.location.search).get('payment');
    if (result === 'success') {
      return 'Checkout returned. Payment will be confirmed after Stripe verifies it.';
    }
    if (result === 'cancelled') {
      return 'Checkout was cancelled. No payment has been confirmed.';
    }
    return '';
  });

  const selectedPolicy = policies.find(p => p.id === selectedPolicyId);
  const policyClaims = claims.filter(c => c.policyId === selectedPolicyId);
  const pendingCount = policyClaims.filter(c => c.status === 'Pending').length;
  const approvedCount = policyClaims.filter(c => c.status === 'Approved').length;
  const totalClaimed = policyClaims.reduce((sum, c) => sum + c.amount, 0);

  useEffect(() => {
    if (!selectedPolicyId) {
      setPayment(null);
      return undefined;
    }
    let active = true;
    axios.get(`${apiBase}/policies/${selectedPolicyId}/payments/latest`)
      .then(response => {
        if (active) {
          setPayment(response.data);
          setPaymentError('');
        }
      })
      .catch(err => {
        if (!active) {
          return;
        }
        setPayment(null);
        setPaymentError(err.response?.status === 404 ? '' : 'Unable to load payment status.');
      });
    return () => { active = false; };
  }, [apiBase, selectedPolicyId]);

  const openAddModal = () => {
    setModalMode('add');
    setBillingData({ premiumAmount: '', ownerPrincipal: '' });
    setBillingError('');
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
    setBillingData({ premiumAmount: policy.premiumAmount ?? '', ownerPrincipal: '' });
    setBillingError('');
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

  const handleBillingInputChange = (e) => {
    const { name, value } = e.target;
    setBillingData(prev => ({ ...prev, [name]: value }));
  };

  const handleSaveBilling = async (e) => {
    e.preventDefault();
    if (billingData.premiumAmount === '' || Number(billingData.premiumAmount) < 0
        || !Number.isFinite(Number(billingData.premiumAmount))) {
      setBillingError('Enter a non-negative USD premium.');
      return;
    }
    try {
      await axios.put(`${apiBase}/policies/${selectedPolicyId}/billing`, {
        premiumAmount: billingData.premiumAmount,
        currency: 'USD',
        ownerPrincipal: billingData.ownerPrincipal.trim() || null,
      });
      setBillingError('');
      onRefresh();
    } catch (err) {
      setBillingError(err.response?.status === 403
        ? 'Only configured staff can update billing details.'
        : err.response?.data?.message || 'Failed to update billing details.');
    }
  };

  const handleStartCheckout = async () => {
    setPaymentError('');
    try {
      const response = await axios.post(`${apiBase}/policies/${selectedPolicyId}/payments/checkout`);
      window.location.assign(response.data.checkoutUrl);
    } catch (err) {
      setPaymentError(err.response?.data?.message || 'Unable to start checkout. Please try again.');
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
            <div className="payment-panel" data-testid="policy-payment">
              <div>
                <span className="label">Premium</span>
                <span className="value" data-testid="policy-premium">
                  {selectedPolicy.premiumAmount == null
                    ? 'Not configured'
                    : `${selectedPolicy.currency || 'USD'} ${Number(selectedPolicy.premiumAmount).toFixed(2)}`}
                </span>
              </div>
              <div className="payment-status" data-testid="payment-status">
                {payment?.status === 'SUCCEEDED' && 'Payment confirmed'}
                {payment?.status === 'PENDING' && 'Payment pending confirmation'}
                {payment?.status === 'FAILED' && 'Payment failed'}
                {payment?.status === 'CANCELLED' && 'Checkout expired or cancelled'}
                {!payment && !paymentError && 'No payment recorded'}
                {paymentError && paymentError}
              </div>
              {checkoutMessage && <p role="status" data-testid="checkout-message">{checkoutMessage}</p>}
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleStartCheckout}
                disabled={selectedPolicy.status !== 'active'
                  || !selectedPolicy.premiumAmount || Number(selectedPolicy.premiumAmount) <= 0}
                data-testid="pay-premium-btn"
              >
                Pay premium
              </button>
              {paymentError && <p role="alert" data-testid="checkout-error">{paymentError}</p>}
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
            {modalMode === 'edit' && (
              <form onSubmit={handleSaveBilling} data-testid="billing-form">
                <h3>Staff billing settings</h3>
                <p>Only configured staff can save the premium and policyholder identity.</p>
                {billingError && <div className="alert alert-error" data-testid="billing-form-error">{billingError}</div>}
                <div className="form-group">
                  <label htmlFor="premium-amount">Premium (USD)</label>
                  <input
                    id="premium-amount"
                    type="number"
                    name="premiumAmount"
                    min="0"
                    step="0.01"
                    value={billingData.premiumAmount}
                    onChange={handleBillingInputChange}
                    required
                    data-testid="input-premium-amount"
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="owner-principal">Policyholder sign-in identity (optional)</label>
                  <input
                    id="owner-principal"
                    type="text"
                    name="ownerPrincipal"
                    value={billingData.ownerPrincipal}
                    onChange={handleBillingInputChange}
                    data-testid="input-owner-principal"
                  />
                </div>
                <button type="submit" className="btn btn-primary" data-testid="save-billing-btn">
                  Save billing settings
                </button>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;
