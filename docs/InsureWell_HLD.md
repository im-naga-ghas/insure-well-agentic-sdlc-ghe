# InsureWell High-Level Design

## 1. Overview
InsureWell is a Flask + SQLite web application that provides policy management and claim lifecycle capabilities through:
- Server-rendered pages for dashboard and claims workflows.
- REST APIs for policy and claim CRUD operations.
- File upload support for claim attachments.

This HLD is derived from the current codebase implementation and is intended to serve as the baseline architecture for future iterations.

## 2. Scope and Goals
### In scope
- Component boundaries and responsibilities.
- Data flow across browser, Flask routes, SQLite, and uploads storage.
- Claims upload pipeline.
- REST API surface and behavior expectations.
- Validation, error handling, observability, and testing strategy.

### Out of scope
- Authentication and authorization model (not currently implemented in code).
- External integrations (payments, notifications, external provider systems).
- Multi-service decomposition.

## 3. Architecture Summary
### Runtime stack
- Python Flask application process.
- SQLite database at `data/insurewell.db`.
- Local file storage for uploads at `uploads/`.
- Jinja templates and static assets served by Flask.

### Component diagram (logical)
```text
+-------------------+        HTTP(S)         +-------------------------------+
| Browser UI        | <--------------------> | Flask App (app.py)            |
| - dashboard.html  |                        |                               |
| - claims.html     |                        |  +-------------------------+  |
| - static/js/app.js|                        |  | Page Routes             |  |
+-------------------+                        |  | /dashboard, /claims     |  |
                                             |  +-------------------------+  |
                                             |  +-------------------------+  |
                                             |  | REST API Routes         |  |
                                             |  | /api/policies*          |  |
                                             |  | /api/claims*            |  |
                                             |  +-------------------------+  |
                                             |  +-------------------------+  |
                                             |  | Validation & Mapping    |  |
                                             |  | request -> SQL -> JSON  |  |
                                             |  +-------------------------+  |
                                             +-------------+-----------------+
                                                           |
                             +-----------------------------+------------------------------+
                             |                                                            |
                             v                                                            v
                 +----------------------------+                              +----------------------------+
                 | SQLite (data/insurewell.db)|                              | File Storage (uploads/)    |
                 | tables: policies, claims   |                              | claim attachment binaries   |
                 +----------------------------+                              +----------------------------+
```

## 4. Module Boundaries and Responsibilities
### 4.1 Web Presentation Module
Responsibilities:
- Render dashboard and claims pages.
- Provide initial data for templates.
- Support query-driven filtering on claims page.

Owned routes:
- GET /
- GET /dashboard
- GET /claims

### 4.2 Claims API Module
Responsibilities:
- List, create, update status, and delete claims.
- Validate claim payloads and enforce status state values.
- Process optional file uploads for claim creation.

Owned routes:
- GET /api/claims
- POST /api/claims
- PATCH /api/claims/<cid>/status
- DELETE /api/claims/<cid>

### 4.3 Policies API Module
Responsibilities:
- List, read, create, update, and delete policies.
- Enforce required fields and business validations.
- Cascade policy deletion to dependent claims at app layer.

Owned routes:
- GET /api/policies
- GET /api/policies/<pid>
- POST /api/policies
- PATCH /api/policies/<pid>
- DELETE /api/policies/<pid>

### 4.4 Persistence Module
Responsibilities:
- Manage SQLite connection lifecycle per request.
- Enforce SQLite pragmas:
  - WAL journal mode.
  - Foreign keys enabled.
- Seed initial sample data on first bootstrap.

Primary functions:
- get_db
- close_db
- init_db

### 4.5 Upload Storage Module
Responsibilities:
- Validate allowed file extensions.
- Sanitize file names via secure filename handling.
- Store uploads with timestamp-prefixed names to reduce collision risk.

## 5. Data Flow
### 5.1 Dashboard and Claims Page Flow
1. Browser requests page route.
2. Flask route queries SQLite for policies and claims.
3. Route maps rows to dict objects.
4. Template rendered with server-side data context.
5. Browser receives HTML and static assets.

### 5.2 API Request Flow
1. Client sends JSON or multipart request to REST endpoint.
2. Route validates required fields and data types.
3. Route executes SQL read/write transaction.
4. Route commits on success for mutating operations.
5. Route returns JSON payload and HTTP status.

### 5.3 Claims Upload Pipeline
1. Client submits multipart form to POST /api/claims.
2. Server checks required form fields:
   - policy_id
   - amount
   - description
3. Server validates:
   - policy exists
   - amount is numeric and > 0
4. If file exists:
   - extension must be one of pdf, jpg, jpeg, png
   - filename sanitized
   - binary saved to uploads directory with millisecond prefix
5. Server inserts claim record into SQLite with:
   - generated claim id
   - status default Pending
   - original uploaded file name (logical reference)
6. Server returns created claim JSON with status 201.

## 6. REST API Surface

| Method | Path | Purpose | Success | Key error responses |
|---|---|---|---|---|
| GET | /api/health | Liveness check | 200 | None |
| GET | /api/policies | List policies | 200 | None |
| GET | /api/policies/<pid> | Get policy | 200 | 404 policy not found |
| POST | /api/policies | Create policy | 201 | 400 validation errors |
| PATCH | /api/policies/<pid> | Update policy fields | 200 | 400 invalid/no fields, 404 not found |
| DELETE | /api/policies/<pid> | Delete policy and its claims | 204 | 404 not found |
| GET | /api/claims | List claims (optional policy filter) | 200 | None |
| POST | /api/claims | Create claim with optional file | 201 | 400 validation/file errors, 404 policy not found |
| PATCH | /api/claims/<cid>/status | Update claim status | 200 | 400 invalid status, 404 claim not found |
| DELETE | /api/claims/<cid> | Delete claim | 204 | 404 claim not found |

### Request/response notes
- POST /api/claims uses multipart form data.
- POST/PATCH policy endpoints use JSON payloads.
- Error response model is consistent JSON object with error field.

## 7. Validation, Error Handling, and Observability
### Validation rules
- Claim creation:
  - policy_id required and must exist.
  - amount required, numeric, positive.
  - description required.
  - upload extension restricted to allowed set.
- Claim status update:
  - status must be one of Pending, Approved, Rejected.
- Policy creation/update:
  - required fields validated for create.
  - coverage amount must be positive numeric.
  - status must be active or inactive.

### Error handling behavior
- 400 for malformed or invalid business input.
- 404 for missing policy or claim resources.
- 204 for successful delete operations with empty body.

### Observability expectations
Current state:
- Minimal explicit logging; relies primarily on Flask/Werkzeug default output.

Recommended baseline enhancements:
- Structured request logging with route, method, status, latency.
- Error event logging for validation and DB failures.
- Correlation id per request for troubleshooting.
- Basic metrics:
  - request count by route/status
  - p95 latency by route
  - claim upload success/failure counts

## 8. Non-Functional Considerations
### Security
- Input and extension validation implemented.
- Filename sanitization implemented.
- No authentication/authorization currently; all routes are publicly accessible.
- Future phase should enforce role-based access and CSRF protections for browser workflows.

### Performance
- SQLite with WAL mode is sufficient for low to moderate single-node load.
- Query patterns are straightforward and index usage should be reviewed as data grows.

### Reliability
- Local file system uploads create dependency on host disk durability.
- For multi-node deployment, move uploads to object storage.

## 9. Migration Notes
Current schema is managed in-code via init_db and CREATE TABLE IF NOT EXISTS statements.

Migration strategy recommendations:
1. Introduce explicit migration tool (for example Alembic or SQL migration scripts) before adding non-trivial schema changes.
2. Backfill scripts should be idempotent and environment-safe.
3. Add schema version table to track upgrade state.

## 10. Test Strategy for Critical Flows
### Critical flows
- Create claim (with and without file).
- Claim status transitions.
- Policy create/update/delete lifecycle.
- Claims listing with policy filter.
- Error paths for invalid input and missing resources.

### Test levels
- Unit tests for validation helpers and status constraints.
- API integration tests for each route with positive/negative cases.
- Upload tests:
  - allowed file types
  - disallowed extensions
  - oversized payload rejection behavior
- UI smoke tests for dashboard and claims render.

## 11. Design Decisions and Unresolved Questions
### Design decisions
1. Keep monolithic Flask architecture for current scale and demo simplicity.
2. Keep SQLite + local uploads for low operational overhead in MVP.
3. Preserve server-rendered templates while exposing REST APIs for progressive enhancement.

### Unresolved questions
1. Should claim attachments support secure download endpoint and virus scanning?
2. What are retention and deletion policies for uploaded files?
3. Is policy deletion expected to hard delete dependent claims or soft delete both entities?
4. What authentication model and roles are required for production readiness?

## 12. Traceability Matrix

| Feature | HLD module | Data entity |
|---|---|---|
| View dashboard and claims | Web Presentation Module | policies, claims |
| Submit claim with attachment | Claims API Module + Upload Storage Module | claims |
| Update claim status | Claims API Module | claims |
| Manage policy lifecycle | Policies API Module | policies, claims |
| Persist and seed baseline data | Persistence Module | policies, claims |

## 13. Cloud Delegation Candidates
1. Add OpenAPI document for all REST endpoints.
   - Files: docs/openapi.yaml, README.md
   - Effort: M
   - Risk: Low
   - Acceptance criteria: OpenAPI validates and covers all current API routes/status codes.

2. Add automated API contract tests for claims status transitions.
   - Files: tests/test_claim_status_api.py
   - Effort: S
   - Risk: Low
   - Acceptance criteria: Tests cover valid status updates plus 400/404 negative paths.

3. Add request/response structured logging middleware.
   - Files: app.py
   - Effort: M
   - Risk: Medium
   - Acceptance criteria: Every request logs method, path, status, latency, and request id.

4. Add upload hardening checks.
   - Files: app.py, tests/test_claim_uploads.py
   - Effort: M
   - Risk: Medium
   - Acceptance criteria: MIME validation and safe size checks are tested for pass/fail paths.

5. Add docs cross-links between BRD, HLD, architecture, and data model.
   - Files: docs/BRD.md, docs/Epics.md, docs/Features.md, docs/InsureWell_HLD.md, docs/InsureWell_DataModel.md
   - Effort: S
   - Risk: Low
   - Acceptance criteria: Navigable links and traceability references exist across all lifecycle docs.

## 14. Recommended Next Agent
- 3.SDLC Architecture Agent for architecture decision records and formal diagrams.
- 5.SDLC Dev Agent when implementing approved feature increments from this HLD.
