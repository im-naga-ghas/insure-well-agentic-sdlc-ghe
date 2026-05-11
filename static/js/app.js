/* InsureWell – vanilla JS for interactive behaviour */

// ── Helpers ──────────────────────────────────────────────────────────────────

function fmt(amount) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD', maximumFractionDigits: 0,
  }).format(amount);
}

function fmtDate(iso) {
  return iso ? iso.slice(0, 10) : '—';
}

function badgeClass(status) {
  if (status === 'Approved' || status === 'active') return 'badge-success';
  if (status === 'Pending')  return 'badge-warning';
  if (status === 'Rejected') return 'badge-danger';
  return 'badge-neutral';
}

function badge(status) {
  const label = status.charAt(0).toUpperCase() + status.slice(1);
  return `<span class="badge ${badgeClass(status)}">${label}</span>`;
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

function initDashboard() {
  const iw = window.__IW__;
  if (!iw) return;

  // Tabs are now <div> elements; ignore clicks on nested action buttons
  document.querySelectorAll('.policy-tab').forEach(tab => {
    tab.addEventListener('click', e => {
      if (e.target.closest('.tab-action-btn')) return;
      document.querySelectorAll('.policy-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      renderPolicyView(tab.dataset.policyId, iw.policies, iw.claims);
    });
  });

  // Close modal on Escape or overlay click
  const modal = document.getElementById('policy-modal');
  if (modal) {
    modal.addEventListener('click', e => { if (e.target === modal) closePolicyModal(); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closePolicyModal(); });
  }
}

function renderPolicyView(id, policies, claims) {
  const p       = policies.find(x => x.id === id);
  const pClaims = claims.filter(c => c.policy_id === id);

  if (!p) return;

  // Policy card fields
  document.getElementById('pc-plan').textContent     = p.plan_name;
  document.getElementById('pc-id').textContent       = 'Policy ID: ' + p.id;
  document.getElementById('pc-holder').textContent   = p.holder_name;
  document.getElementById('pc-coverage').textContent = fmt(p.coverage_amount);
  document.getElementById('pc-start').textContent    = p.start_date;
  document.getElementById('pc-end').textContent      = p.end_date;

  const statusEl = document.getElementById('pc-status');
  statusEl.className   = 'badge ' + badgeClass(p.status);
  statusEl.textContent = p.status.charAt(0).toUpperCase() + p.status.slice(1);

  // Stats
  const pending  = pClaims.filter(c => c.status === 'Pending').length;
  const approved = pClaims.filter(c => c.status === 'Approved').length;
  const total    = pClaims.reduce((s, c) => s + c.amount, 0);

  document.getElementById('stat-total').textContent    = pClaims.length;
  document.getElementById('stat-pending').textContent  = pending;
  document.getElementById('stat-approved').textContent = approved;
  document.getElementById('stat-amount').textContent   = fmt(total);

  // Recent claims table
  const wrap = document.getElementById('recent-claims-wrap');
  if (!wrap) return;

  if (pClaims.length === 0) {
    wrap.innerHTML = `
      <div class="empty-state">
        <span class="empty-icon">📋</span>
        <p>No claims submitted yet for this policy.</p>
        <a href="/claims" class="btn btn-primary">Submit your first claim</a>
      </div>`;
    return;
  }

  const rows = pClaims.slice(0, 5).map(c => `
    <tr>
      <td class="mono">${c.id}</td>
      <td>${c.description}</td>
      <td>${fmt(c.amount)}</td>
      <td>${badge(c.status)}</td>
      <td>${fmtDate(c.submitted_at)}</td>
    </tr>`).join('');

  wrap.innerHTML = `
    <div class="claims-table-wrap">
      <table class="claims-table">
        <thead>
          <tr>
            <th>Claim ID</th><th>Description</th><th>Amount</th>
            <th>Status</th><th>Submitted</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>`;
}

// ── Policy CRUD ───────────────────────────────────────────────────────────────

function openPolicyModal(mode, policyId) {
  const modal  = document.getElementById('policy-modal');
  const title  = document.getElementById('policy-modal-title');
  const errEl  = document.getElementById('policy-modal-error');
  const editId = document.getElementById('pm-edit-id');
  const form   = document.getElementById('policy-form');

  errEl.style.display = 'none';
  form.reset();

  if (mode === 'edit' && policyId) {
    const policy = (window.__IW__?.policies || []).find(p => p.id === policyId);
    if (!policy) return;
    title.textContent = 'Edit Policy';
    editId.value      = policyId;
    document.getElementById('pm-holder').value   = policy.holder_name;
    document.getElementById('pm-plan').value     = policy.plan_name;
    document.getElementById('pm-coverage').value = policy.coverage_amount;
    document.getElementById('pm-status').value   = policy.status;
    document.getElementById('pm-start').value    = policy.start_date;
    document.getElementById('pm-end').value      = policy.end_date;
    document.getElementById('pm-submit-btn').textContent = 'Save Changes';
  } else {
    title.textContent = 'Add Policy';
    editId.value      = '';
    document.getElementById('pm-submit-btn').textContent = 'Add Policy';
  }

  modal.removeAttribute('hidden');
  document.getElementById('pm-holder').focus();
}

function closePolicyModal() {
  document.getElementById('policy-modal').setAttribute('hidden', '');
}

async function savePolicyForm(e) {
  e.preventDefault();

  const errEl  = document.getElementById('policy-modal-error');
  const btn    = document.getElementById('pm-submit-btn');
  const editId = document.getElementById('pm-edit-id').value.trim();

  errEl.style.display = 'none';
  btn.disabled        = true;
  btn.textContent     = 'Saving…';

  const body = {
    holder_name:     document.getElementById('pm-holder').value.trim(),
    plan_name:       document.getElementById('pm-plan').value.trim(),
    coverage_amount: parseFloat(document.getElementById('pm-coverage').value),
    status:          document.getElementById('pm-status').value,
    start_date:      document.getElementById('pm-start').value,
    end_date:        document.getElementById('pm-end').value,
  };

  const url    = editId ? `/api/policies/${editId}` : '/api/policies';
  const method = editId ? 'PATCH' : 'POST';

  try {
    const res  = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(body),
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to save policy');
    window.location.reload();
  } catch (err) {
    errEl.textContent   = err.message;
    errEl.style.display = 'block';
    btn.disabled        = false;
    btn.textContent     = editId ? 'Save Changes' : 'Add Policy';
  }
}

async function deletePolicy(id, name) {
  if (!confirm(`Delete policy for "${name}"?\n\nAll associated claims will also be permanently deleted.`)) return;

  try {
    const res = await fetch(`/api/policies/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const d = await res.json().catch(() => ({}));
      alert(d.error || 'Failed to delete policy');
      return;
    }
    window.location.reload();
  } catch {
    alert('Network error. Please try again.');
  }
}

// ── Claims page ───────────────────────────────────────────────────────────────

function initClaims() {
  const form = document.getElementById('claim-form');
  if (!form) return;
  form.addEventListener('submit', handleClaimSubmit);
}

function toggleForm() {
  const card = document.getElementById('claim-form-card');
  const btn  = document.getElementById('toggle-form-btn');
  const open = card.style.display === 'none';
  card.style.display = open ? 'block' : 'none';
  btn.textContent    = open ? '✕ Cancel' : '+ New Claim';
  if (open) {
    document.getElementById('form-error').style.display = 'none';
    document.getElementById('claim-form').reset();
  }
}

async function downloadClaimDocument(id, policyId) {
  try {
    const res = await fetch(`/api/claims/${encodeURIComponent(id)}/document?policy_id=${encodeURIComponent(policyId)}`);
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || 'Failed to download document');
    }

    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = getDownloadFilename(res.headers.get('Content-Disposition'));
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (err) {
    alert(err.message);
  }
}

function getDownloadFilename(contentDisposition) {
  if (!contentDisposition) return 'claim-document';

  const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
  if (utf8Match) return decodeURIComponent(utf8Match[1]);

  const filenameMatch = contentDisposition.match(/filename\s*=\s*"((?:[^"\\]|\\.)*)"|filename\s*=\s*([^;]+)/i);
  const filename = filenameMatch ? (filenameMatch[1] || filenameMatch[2] || '').trim() : '';
  return filename ? filename.replace(/\\"/g, '"') : 'claim-document';
}

async function handleClaimSubmit(e) {
  e.preventDefault();

  const errEl     = document.getElementById('form-error');
  const submitBtn = document.getElementById('submit-btn');
  errEl.style.display = 'none';

  const amount = parseFloat(document.getElementById('f-amount').value);
  const desc   = document.getElementById('f-desc').value.trim();

  if (!amount || amount <= 0) {
    errEl.textContent   = 'Please enter a valid claim amount.';
    errEl.style.display = 'block';
    return;
  }
  if (!desc) {
    errEl.textContent   = 'Please provide a description.';
    errEl.style.display = 'block';
    return;
  }

  submitBtn.disabled    = true;
  submitBtn.textContent = 'Submitting…';

  try {
    const fd  = new FormData(document.getElementById('claim-form'));
    const res = await fetch('/api/claims', { method: 'POST', body: fd });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Submission failed');

    // Show success banner
    const alertArea = document.getElementById('alert-area');
    alertArea.innerHTML = '<div class="alert alert-success">✅ Claim submitted successfully! It is now under review.</div>';
    setTimeout(() => { alertArea.innerHTML = ''; }, 5000);

    // Prepend new row or reload if table not present
    const tbody = document.getElementById('claims-tbody');
    if (tbody) {
      const c  = data;
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="mono">${c.id}</td>
        <td class="mono">${c.policy_id}</td>
        <td class="desc-cell">${c.description}</td>
        <td>$${c.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
        <td>
          <select class="status-select status-pending"
                  onchange="updateClaimStatus('${c.id}', this.value, this)">
            <option value="Pending" selected>Pending</option>
            <option value="Approved">Approved</option>
            <option value="Rejected">Rejected</option>
          </select>
        </td>
        <td>${c.file_name ? `<span class="file-chip">📎 ${c.file_name}</span>` : '<span class="text-muted">—</span>'}</td>
        <td>${fmtDate(c.submitted_at)}</td>
        <td class="action-cell">
          ${c.file_name ? `<button class="btn btn-sm" onclick="downloadClaimDocument('${c.id}', '${c.policy_id}')">Download</button>` : ''}
          <button class="btn btn-sm btn-danger" onclick="deleteClaim('${c.id}', this)">Delete</button>
        </td>`;
      tbody.prepend(tr);

      const countEl = document.getElementById('claims-count');
      if (countEl) {
        const n = tbody.rows.length;
        countEl.textContent = `${n} claim${n !== 1 ? 's' : ''}`;
      }
    } else {
      window.location.reload();
      return;
    }

    document.getElementById('claim-form').reset();
    toggleForm();

  } catch (err) {
    errEl.textContent   = err.message;
    errEl.style.display = 'block';
  } finally {
    submitBtn.disabled    = false;
    submitBtn.textContent = 'Submit Claim';
  }
}

async function deleteClaim(id, btnEl) {
  if (!confirm('Delete this claim? This cannot be undone.')) return;

  btnEl.disabled = true;

  try {
    const res = await fetch(`/api/claims/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const d = await res.json().catch(() => ({}));
      alert(d.error || 'Failed to delete claim');
      btnEl.disabled = false;
      return;
    }
    const row     = btnEl.closest('tr');
    const tbody   = row?.parentElement;
    row?.remove();

    const countEl = document.getElementById('claims-count');
    if (countEl && tbody) {
      const n = tbody.rows.length;
      countEl.textContent = `${n} claim${n !== 1 ? 's' : ''}`;
    }
  } catch {
    alert('Network error. Please try again.');
    btnEl.disabled = false;
  }
}

async function updateClaimStatus(id, newStatus, selectEl) {
  const prev = selectEl.dataset.prev || selectEl.querySelector('[selected]')?.value || newStatus;

  try {
    const res  = await fetch(`/api/claims/${id}/status`, {
      method:  'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ status: newStatus }),
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Update failed');

    selectEl.className       = `status-select status-${newStatus.toLowerCase()}`;
    selectEl.dataset.prev    = newStatus;
  } catch (err) {
    alert(err.message);
    selectEl.value = prev;
  }
}

function filterByPolicy(policyId) {
  const url = new URL(window.location.href);
  if (policyId) url.searchParams.set('policy_id', policyId);
  else url.searchParams.delete('policy_id');
  window.location.href = url.toString();
}


// ── Helpers ──────────────────────────────────────────────────────────────────

function fmt(amount) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD', maximumFractionDigits: 0,
  }).format(amount);
}

function fmtDate(iso) {
  return iso ? iso.slice(0, 10) : '—';
}

function badgeClass(status) {
  if (status === 'Approved' || status === 'active') return 'badge-success';
  if (status === 'Pending')  return 'badge-warning';
  if (status === 'Rejected') return 'badge-danger';
  return 'badge-neutral';
}

function badge(status) {
  return `<span class="badge ${badgeClass(status)}">${status.charAt(0).toUpperCase() + status.slice(1)}</span>`;
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

function initDashboard() {
  const iw = window.__IW__;
  if (!iw) return;

  document.querySelectorAll('.policy-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.policy-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      renderPolicyView(tab.dataset.policyId, iw.policies, iw.claims);
    });
  });
}

function renderPolicyView(id, policies, claims) {
  const p       = policies.find(x => x.id === id);
  const pClaims = claims.filter(c => c.policy_id === id);

  if (!p) return;

  // Policy card fields
  document.getElementById('pc-plan').textContent     = p.plan_name;
  document.getElementById('pc-id').textContent       = 'Policy ID: ' + p.id;
  document.getElementById('pc-holder').textContent   = p.holder_name;
  document.getElementById('pc-coverage').textContent = fmt(p.coverage_amount);
  document.getElementById('pc-start').textContent    = p.start_date;
  document.getElementById('pc-end').textContent      = p.end_date;

  const statusEl = document.getElementById('pc-status');
  statusEl.className = 'badge ' + badgeClass(p.status);
  statusEl.textContent = p.status.charAt(0).toUpperCase() + p.status.slice(1);

  // Stats
  const pending  = pClaims.filter(c => c.status === 'Pending').length;
  const approved = pClaims.filter(c => c.status === 'Approved').length;
  const total    = pClaims.reduce((s, c) => s + c.amount, 0);

  document.getElementById('stat-total').textContent    = pClaims.length;
  document.getElementById('stat-pending').textContent  = pending;
  document.getElementById('stat-approved').textContent = approved;
  document.getElementById('stat-amount').textContent   = fmt(total);

  // Recent claims table
  const wrap = document.getElementById('recent-claims-wrap');
  if (pClaims.length === 0) {
    wrap.innerHTML = `
      <div class="empty-state">
        <span class="empty-icon">📋</span>
        <p>No claims submitted yet for this policy.</p>
        <a href="/claims" class="btn btn-primary">Submit your first claim</a>
      </div>`;
    return;
  }

  const rows = pClaims.slice(0, 5).map(c => `
    <tr>
      <td class="mono">${c.id}</td>
      <td>${c.description}</td>
      <td>${fmt(c.amount)}</td>
      <td>${badge(c.status)}</td>
      <td>${fmtDate(c.submitted_at)}</td>
    </tr>`).join('');

  wrap.innerHTML = `
    <div class="claims-table-wrap">
      <table class="claims-table">
        <thead>
          <tr>
            <th>Claim ID</th><th>Description</th><th>Amount</th>
            <th>Status</th><th>Submitted</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>`;
}

// ── Claims page ───────────────────────────────────────────────────────────────

function initClaims() {
  const form = document.getElementById('claim-form');
  if (!form) return;
  form.addEventListener('submit', handleClaimSubmit);
}

function toggleForm() {
  const card = document.getElementById('claim-form-card');
  const btn  = document.getElementById('toggle-form-btn');
  const open = card.style.display === 'none';
  card.style.display = open ? 'block' : 'none';
  btn.textContent    = open ? '✕ Cancel' : '+ New Claim';
  if (open) {
    document.getElementById('form-error').style.display = 'none';
    document.getElementById('claim-form').reset();
  }
}

async function handleClaimSubmit(e) {
  e.preventDefault();

  const errEl  = document.getElementById('form-error');
  const submitBtn = document.getElementById('submit-btn');
  errEl.style.display = 'none';

  const amount = parseFloat(document.getElementById('f-amount').value);
  const desc   = document.getElementById('f-desc').value.trim();

  if (!amount || amount <= 0) {
    errEl.textContent    = 'Please enter a valid claim amount.';
    errEl.style.display  = 'block';
    return;
  }
  if (!desc) {
    errEl.textContent    = 'Please provide a description.';
    errEl.style.display  = 'block';
    return;
  }

  submitBtn.disabled    = true;
  submitBtn.textContent = 'Submitting…';

  const fd = new FormData(document.getElementById('claim-form'));

  try {
    const res  = await fetch('/api/claims', { method: 'POST', body: fd });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Submission failed');

    // Show success banner
    const alertArea = document.getElementById('alert-area');
    alertArea.innerHTML = '<div class="alert alert-success">✅ Claim submitted successfully! It is now under review.</div>';
    setTimeout(() => { alertArea.innerHTML = ''; }, 5000);

    // Prepend new row to the claims table (or reload if table doesn't exist yet)
    const tbody = document.getElementById('claims-tbody');
    if (tbody) {
      const c = data;
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="mono">${c.id}</td>
        <td class="mono">${c.policy_id}</td>
        <td class="desc-cell">${c.description}</td>
        <td>$${c.amount.toLocaleString('en-US', {minimumFractionDigits:2})}</td>
        <td>${badge(c.status)}</td>
        <td><span class="text-muted">—</span></td>
        <td>${fmtDate(c.submitted_at)}</td>`;
      tbody.prepend(tr);

      // Update count
      const countEl = document.getElementById('claims-count');
      if (countEl) {
        const n = tbody.rows.length;
        countEl.textContent = `${n} claim${n !== 1 ? 's' : ''}`;
      }
    } else {
      // No table yet — reload to show it
      window.location.reload();
      return;
    }

    // Reset form and close it
    document.getElementById('claim-form').reset();
    toggleForm();

  } catch (err) {
    errEl.textContent   = err.message;
    errEl.style.display = 'block';
  } finally {
    submitBtn.disabled    = false;
    submitBtn.textContent = 'Submit Claim';
  }
}

function filterByPolicy(policyId) {
  const url = new URL(window.location.href);
  if (policyId) url.searchParams.set('policy_id', policyId);
  else url.searchParams.delete('policy_id');
  window.location.href = url.toString();
}
