# InsureWell

A lightweight health insurance management system built with **Python + Flask**, **SQLite**, HTML, CSS, and vanilla JavaScript.

---

## Features (Phase 1 MVP)

- **Policy Dashboard** — view policy details (ID, plan name, coverage amount, status, dates) with per-policy stats and recent claims
- **Multi-policy support** — clickable tabs to switch between policies without a page reload
- **Claims Module** — submit claims (amount, description, optional file upload), filter by policy, and track status (Pending / Approved / Rejected)
- **User Authentication** — login/logout with session timeout, lockout protection, and per-user data access
- **REST API** — JSON endpoints for all data operations
- **Persistent storage** — SQLite database, auto-seeded with sample data on first run

---

## Project Structure

```
InsureWell/
├── app.py                  # Flask app — routes, API, data store, seed data
├── requirements.txt        # Python dependencies
│
├── templates/              # Jinja2 HTML templates
│   ├── base.html           # Shared navbar layout
│   ├── dashboard.html      # Policy overview, stats, recent claims
│   └── claims.html         # Claim submission form and claims list
│
├── static/
│   ├── css/style.css       # Design system — no CSS framework
│   └── js/app.js           # Vanilla JS — tab switching, AJAX form submit
│
├── data/                   # SQLite database (auto-created; git-ignored)
├── uploads/                # Uploaded claim documents (auto-created; git-ignored)
│
├── .gitignore
└── README.md
```

---

## Prerequisites

- Python 3.9 or later

---

## Setup

```bash
# 1. Clone the repository
git clone <repo-url>
cd InsureWell

# 2. Create and activate a virtual environment
python3 -m venv .venv
source .venv/bin/activate      # macOS / Linux
# .venv\Scripts\activate       # Windows

# 3. Install dependencies
pip install -r requirements.txt

# 4. Start the development server
python app.py
```

Open **http://localhost:5001** in your browser.

> On first run, `data/insurewell.db` (SQLite) is created automatically and seeded with 3 sample policies and 7 claims.

### Default seeded users (for local development)

- `alex` / `InsureWell@123`
- `maria` / `InsureWell@123`
- `david` / `InsureWell@123`

---

## API Reference

| Method  | Endpoint                      | Description                        |
|---------|-------------------------------|------------------------------------|
| `GET`   | `/api/health`                 | Health check                       |
| `GET`   | `/api/policies`               | List all policies                  |
| `GET`   | `/api/policies/<id>`          | Get a single policy                |
| `GET`   | `/api/claims`                 | List all claims                    |
| `GET`   | `/api/claims?policy_id=<id>`  | Filter claims by policy            |
| `POST`  | `/api/claims`                 | Submit a new claim (multipart)     |
| `PATCH` | `/api/claims/<id>/status`     | Update claim status                |

### POST `/api/claims` — request body (`multipart/form-data`)

| Field         | Type    | Required | Notes                  |
|---------------|---------|----------|------------------------|
| `policy_id`   | string  | Yes      | e.g. `POL-2024-001`    |
| `amount`      | number  | Yes      | Positive decimal       |
| `description` | string  | Yes      | Free text              |
| `file`        | file    | No       | PDF / JPG / PNG ≤ 5 MB |

### PATCH `/api/claims/<id>/status` — request body (`application/json`)

```json
{ "status": "Approved" }
```

Valid values: `Pending`, `Approved`, `Rejected`.

---

## Sample Data

Auto-seeded on first run when no data store exists:

| Policy ID    | Holder       | Plan                           | Coverage | Status   |
|--------------|--------------|--------------------------------|----------|----------|
| POL-2024-001 | Alex Johnson | InsureWell Premium Health Plan | $250,000 | Active   |
| POL-2024-002 | Maria Garcia | InsureWell Essential Care Plan | $150,000 | Active   |
| POL-2023-009 | David Chen   | InsureWell Family Plus Plan    | $500,000 | Inactive |

Seven sample claims are seeded across the first two policies.

---

## Resetting Sample Data

Stop the server, delete the store file, then restart:

```bash
rm data/insurewell.db
python app.py
```

---

## Future Phases

The codebase is intentionally modular — new routes, templates, and data fields can be added without restructuring:

| Phase | Feature |
|-------|---------|
| 2 | Doctor / hospital provider search |
| 3 | Payment integration |
| 4 | Email / SMS notifications |
| 5 | Family member management |
| 6 | Admin panel with claim adjudication |
