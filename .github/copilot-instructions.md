# Copilot Instructions for InsureWell

## Project Overview
**InsureWell** is a lightweight health insurance management system built with **Python + Flask**, **SQLite**, and a server-rendered (Jinja2) frontend with vanilla HTML/CSS/JavaScript. It manages **policies** and **claims**, including file uploads for claim documentation.

**Tech Stack:**
- Backend: Python 3.9+, Flask (>=2.3), Werkzeug
- Database: SQLite (file-based, WAL mode, FK enforcement)
- Frontend: Jinja2 templates, vanilla JavaScript, hand-rolled CSS (no framework)
- File uploads: PDF / JPG / PNG, max 5 MB
- Testing: pytest (recommended)

---

## 1. Project Structure

```
health-insure-well-app/
├── app.py                  # Flask app — routes, REST API, DB init, seed data
├── requirements.txt        # Python dependencies (flask, werkzeug)
├── README.md
│
├── templates/              # Jinja2 templates
│   ├── base.html           # Shared navbar / layout
│   ├── dashboard.html      # Policy overview + recent claims
│   └── claims.html         # Claim submission form + claims list
│
├── static/
│   ├── css/style.css       # Design system (no framework)
│   └── js/app.js           # Tab switching, AJAX form submit
│
├── data/                   # SQLite DB (insurewell.db) — auto-created
├── uploads/                # Uploaded claim files — auto-created
└── .github/
    └── copilot-instructions.md
```

---

## 2. Backend Development (Flask)

### Application Conventions
- App entry point: [app.py](app.py)
- Server runs on **http://localhost:5001** (`app.run(host='0.0.0.0', port=5001, debug=True)`).
- DB connection per-request via Flask's `g` object; closed in `teardown_appcontext`.
- `init_db()` creates tables and seeds sample data on first run only.
- Timestamps stored as ISO-8601 UTC strings via `_now()` helper.
- IDs: policies use `POL-YYYY-NNN`; new claims use `CLM-<epoch_ms>`.

### Page Routes
```
GET  /                 → redirect to /dashboard
GET  /dashboard        → render dashboard.html
GET  /claims           → render claims.html (?policy_id filter)
```

### REST API
| Method   | Endpoint                       | Description                                 |
|----------|--------------------------------|---------------------------------------------|
| `GET`    | `/api/health`                  | Health check                                |
| `GET`    | `/api/policies`                | List all policies                           |
| `GET`    | `/api/policies/<pid>`          | Get a single policy                         |
| `POST`   | `/api/policies`                | Create policy (JSON)                        |
| `PATCH`  | `/api/policies/<pid>`          | Update policy fields (JSON)                 |
| `DELETE` | `/api/policies/<pid>`          | Delete policy + cascade-delete its claims   |
| `GET`    | `/api/claims`                  | List claims (`?policy_id=` to filter)       |
| `POST`   | `/api/claims`                  | Create claim (`multipart/form-data`)        |
| `PATCH`  | `/api/claims/<cid>/status`     | Update claim status (JSON)                  |
| `DELETE` | `/api/claims/<cid>`            | Delete a claim                              |

### Validation Rules
- **Claim create:** `policy_id`, `amount` (>0), `description` required; policy must exist; optional `file` extension in `{pdf, jpg, jpeg, png}`; max 5 MB (enforced by `MAX_CONTENT_LENGTH`).
- **Claim status:** must be one of `Pending`, `Approved`, `Rejected`.
- **Policy create/update:** `coverage_amount` > 0; `status` ∈ `{active, inactive}`.
- File uploads: sanitize names with `werkzeug.utils.secure_filename` and prefix with epoch ms.

### Backend Best Practices
- Always use parameterized SQL queries (already the pattern — never f-string user input into SQL).
- Use `request.get_json(silent=True) or {}` for JSON bodies; use `request.form` / `request.files` for multipart.
- Return JSON with explicit status codes (`200`, `201`, `204`, `400`, `404`).
- Add type hints and docstrings to new functions.
- Follow PEP 8.

### Commands (Windows PowerShell)
```powershell
# Setup
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt

# Run dev server
python app.py        # http://localhost:5001

# Reset seed data
Remove-Item data\insurewell.db
python app.py
```

---

## 3. Frontend Development

### Templates (Jinja2)
- All pages extend [templates/base.html](templates/base.html).
- Server passes `policies` and `claims` (lists of dicts from `row_to_dict`) to templates.
- The claims page accepts a `selected_policy` query param for filtering.

### JavaScript ([static/js/app.js](static/js/app.js))
- Vanilla JS only — no frameworks, no build step.
- Use `const` / `let` (never `var`).
- Use `fetch()` for API calls; handle non-OK responses and surface errors to the user.
- Use event delegation where reasonable.

### CSS ([static/css/style.css](static/css/style.css))
- Hand-rolled design system using CSS custom properties (`:root { --... }`).
- Keep responsive breakpoints consistent with existing rules.

---

## 4. Error Handling

### Backend Pattern (already used in `app.py`)
```python
@app.route('/api/claims/<cid>/status', methods=['PATCH'])
def api_update_claim_status(cid):
    data = request.get_json(silent=True) or {}
    status = data.get('status')
    if status not in ('Pending', 'Approved', 'Rejected'):
        return jsonify({'error': 'Invalid status'}), 400

    db = get_db()
    if not db.execute('SELECT 1 FROM claims WHERE id = ?', (cid,)).fetchone():
        return jsonify({'error': 'Claim not found'}), 404
    ...
```

### Recommended Global Handlers (add if/when needed)
```python
@app.errorhandler(404)
def not_found(e):
    return jsonify({'error': 'Not Found'}), 404

@app.errorhandler(413)
def too_large(e):
    return jsonify({'error': 'File exceeds 5 MB limit'}), 413

@app.errorhandler(500)
def server_error(e):
    app.logger.error(f'Server error: {e}', exc_info=True)
    return jsonify({'error': 'Internal Server Error'}), 500
```

### Frontend Pattern
```javascript
async function submitClaim(formData) {
    const res = await fetch('/api/claims', { method: 'POST', body: formData });
    if (!res.ok) {
        const { error } = await res.json().catch(() => ({}));
        throw new Error(error || `HTTP ${res.status}`);
    }
    return res.json();
}
```

---

## 5. Testing (pytest)

> Tests are not yet committed. When adding them, place `test_app.py` next to `app.py` and use Flask's test client. Each test should use a temporary SQLite DB (override `DB_PATH` via env var or fixture).

### Example Fixtures
```python
import pytest
from app import app, init_db

@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setattr('app.DB_PATH', str(tmp_path / 'test.db'))
    monkeypatch.setattr('app.UPLOAD_DIR', str(tmp_path / 'uploads'))
    init_db()
    app.config['TESTING'] = True
    with app.test_client() as c:
        yield c
```

### Example Tests
```python
def test_health(client):
    r = client.get('/api/health')
    assert r.status_code == 200 and r.json['status'] == 'ok'

def test_list_policies(client):
    r = client.get('/api/policies')
    assert r.status_code == 200 and isinstance(r.json, list)

def test_create_claim_requires_policy(client):
    r = client.post('/api/claims', data={
        'policy_id': 'POL-DOES-NOT-EXIST',
        'amount': '100', 'description': 'x'})
    assert r.status_code == 404

def test_invalid_claim_status(client):
    r = client.patch('/api/claims/CLM-1001/status', json={'status': 'Bogus'})
    assert r.status_code == 400
```

### Commands
```powershell
pytest -v
pytest --cov=. --cov-report=term
```

---

## 6. Security
- **SQL injection:** always use `?` placeholders with `sqlite3` (already the pattern).
- **File uploads:** validate extension allowlist (`ALLOWED`), enforce `MAX_CONTENT_LENGTH`, run `secure_filename`, prefix with timestamp to prevent collisions. Do **not** serve `uploads/` publicly without an auth gate.
- **Input validation:** trim and type-check all incoming fields before DB writes.
- **Debug mode:** `debug=True` is for local development only — disable in production.
- **Secrets:** use environment variables; never commit credentials.

---

## 7. Git Workflow
Use Conventional Commits:
```
feat: add policy archive endpoint
fix: reject claim amounts of zero
refactor: extract claim validation helper
docs: update API reference
test: add coverage for delete policy cascade
chore: bump werkzeug
```

Branches: `feature/<slug>`, `fix/<slug>`.

---

## 8. Common Tasks

| Task                               | Steps                                                                 |
|------------------------------------|-----------------------------------------------------------------------|
| Add a new API endpoint             | Add route in [app.py](app.py) → validate input → add test → update README |
| Add a field to `policies`/`claims` | Update `init_db()` schema → migrate existing DB → update API + templates |
| Change a page                      | Edit template under [templates/](templates/) → update [static/js/app.js](static/js/app.js) if needed |
| Adjust styling                     | Edit [static/css/style.css](static/css/style.css) → verify responsive |
| Reset sample data                  | Delete `data/insurewell.db` and restart `python app.py`               |

---

## Quick Reference

**Port:** 5001
**DB:** `data/insurewell.db` (SQLite, auto-seeded)
**Uploads:** `uploads/` (PDF/JPG/PNG, ≤ 5 MB)

**Key Files:**
- [app.py](app.py) — Flask app, REST API, DB schema + seed
- [templates/base.html](templates/base.html) — layout
- [templates/dashboard.html](templates/dashboard.html) — policy dashboard
- [templates/claims.html](templates/claims.html) — claims module
- [static/js/app.js](static/js/app.js) — frontend logic
- [static/css/style.css](static/css/style.css) — styles
- [requirements.txt](requirements.txt) — Python dependencies
