# InsureWell Legacy (Flask + SQLite)

This folder contains the **original Python Flask application** from the first phase of the InsureWell project.

## 📂 Contents

```
legacy/
├── app.py                    # Flask application (main entry point)
├── requirements.txt          # Python dependencies
├── templates/                # Jinja2 HTML templates
│   ├── base.html            # Base layout with navbar
│   ├── dashboard.html       # Policy dashboard page
│   └── claims.html          # Claims management page
├── static/                   # Frontend assets
│   ├── css/style.css        # Global styles (no framework)
│   └── js/app.js            # Vanilla JavaScript (AJAX, DOM manipulation)
├── data/                     # Database storage
│   └── insurewell.db        # SQLite database (auto-created)
└── uploads/                  # Uploaded claim documents
    └── [uploaded files]
```

## 🚀 Quick Start

### 1. Activate Virtual Environment
```bash
cd ../
source .venv/bin/activate
```

### 2. Install Dependencies
```bash
cd legacy
pip install -r requirements.txt
```

### 3. Run the Server
```bash
python app.py
```

The app will start on **http://localhost:5001**

### 4. First Run
- SQLite database (`data/insurewell.db`) is auto-created
- Sample data (3 policies, 7 claims) is auto-seeded
- Open browser to http://localhost:5001

## 📋 Features

✅ **Policy Management**
- View policies with tabs for multi-policy switching
- Add, edit, delete policies
- Policy details: holder name, plan, coverage, dates, status

✅ **Claims Management**
- Submit new claims with optional file upload
- View all claims or filter by policy
- Update claim status (Pending → Approved / Rejected)
- Delete claims

✅ **Data Persistence**
- SQLite database (`data/insurewell.db`)
- Auto-seeded with sample data
- Persists across restarts (until deleted)

✅ **REST API**
- JSON endpoints for all CRUD operations
- Supports multipart file uploads
- Full data validation

## 🛠 API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/policies` | List all policies |
| POST | `/api/policies` | Create policy |
| PATCH | `/api/policies/<id>` | Update policy |
| DELETE | `/api/policies/<id>` | Delete policy |
| GET | `/api/claims` | List claims (filter by `?policy_id=<id>`) |
| POST | `/api/claims` | Submit claim (multipart form data) |
| PATCH | `/api/claims/<id>/status` | Update claim status |
| DELETE | `/api/claims/<id>` | Delete claim |

### Example: Create a Claim
```bash
curl -X POST http://localhost:5001/api/claims \
  -F "policy_id=POL-2024-001" \
  -F "amount=500" \
  -F "description=Doctor visit" \
  -F "file=@receipt.pdf"
```

## 📝 Database Schema

### policies table
```sql
CREATE TABLE policies (
    id TEXT PRIMARY KEY,
    holder_name TEXT NOT NULL,
    plan_name TEXT NOT NULL,
    coverage_amount REAL NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    created_at TEXT NOT NULL
);
```

### claims table
```sql
CREATE TABLE claims (
    id TEXT PRIMARY KEY,
    policy_id TEXT NOT NULL REFERENCES policies(id),
    amount REAL NOT NULL,
    description TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Pending',
    file_name TEXT,
    submitted_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
```

## 🔄 Reset Sample Data

To reset to initial sample data:

```bash
rm data/insurewell.db
python app.py
```

This will recreate the database and re-seed sample data.

## 🎨 UI/UX

- **Framework-free design** — Pure CSS, no Bootstrap or Tailwind
- **Vanilla JavaScript** — AJAX requests, DOM manipulation, form validation
- **Responsive layout** — Works on desktop and tablet
- **Color scheme** — Professional blue and neutral tones
- **Accessible forms** — Proper labels, error messages, disabled states

## 📦 Dependencies

See `requirements.txt`:
- **Flask** — Web framework
- **Werkzeug** — WSGI utilities
- **Jinja2** — Template engine

## ⚠️ Status

This is the **original MVP implementation** and is now **archived**. 

For new features and development, use the **modern React + Spring Boot stack** in the `src/` folder.

---

**Last Updated:** May 11, 2026  
**Status:** Legacy (stable, archived)
