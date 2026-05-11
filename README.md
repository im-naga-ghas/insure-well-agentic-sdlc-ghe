# InsureWell

A lightweight health insurance management system built with a **React** frontend and a **Java Spring Boot** backend.

---

## Features (Phase 1 MVP)

- **Policy Dashboard** — view policy details (ID, plan name, coverage amount, status, dates) with per-policy stats and recent claims
- **Multi-policy support** — clickable tabs to switch between policies without a page reload
- **Claims Module** — submit claims (amount, description, optional file upload), filter by policy, and track status (Pending / Approved / Rejected)
- **REST API** — JSON endpoints for policy and claim operations
- **Seeded sample data** — H2-backed backend starts with sample policies and claims for local development

---

## Project Structure

```
InsureWell/
├── src/
│   ├── backend/            # Spring Boot API, entities, repositories, seed data
│   ├── frontend/           # React UI
│   ├── run.sh              # Starts backend and frontend together
│   └── README.md           # Short source-tree note
├── docs/                   # Architecture and data model docs
├── handbook/               # Workflow and setup guides
├── images/                 # Supporting images and demo assets
└── README.md
```

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 18+
- npm 9+

---

## Setup

```bash
# 1. Clone the repository
git clone <repo-url>
cd insure-well-agentic-sdlc-ghe

# 2. Start the backend and frontend
cd src
chmod +x run.sh
./run.sh
```

Open **http://localhost:3000** in your browser.

The backend API runs on **http://localhost:8080/api** and is seeded with sample policies and claims on startup. The script installs frontend dependencies automatically if `node_modules` is missing.

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

The backend uses an in-memory H2 database in local development, so sample data is recreated on every restart. To reset the app state, stop the running services and start them again:

```bash
cd src
./run.sh
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
