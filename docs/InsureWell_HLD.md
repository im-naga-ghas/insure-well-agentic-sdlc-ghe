# InsureWell High-Level Design

## 1. Overview
InsureWell is a React + Spring Boot application that provides policy management and claim lifecycle capabilities through:
- A React frontend for dashboard and claims workflows.
- REST APIs for policy and claim CRUD operations.
- File upload support for claim attachments.

This HLD is derived from the current codebase implementation and is intended to serve as the baseline architecture for future iterations.

## 2. Scope and Goals
### In scope
- Component boundaries and responsibilities.
- Data flow across browser, React UI, Spring Boot APIs, H2-backed persistence, and attachment handling.
- Claims upload pipeline.
- REST API surface and behavior expectations.
- Validation, error handling, observability, and testing strategy.

### Out of scope
- Authentication and authorization model (not currently implemented in code).
- External integrations (payments, notifications, external provider systems).
- Multi-service decomposition.

## 3. Architecture Summary
### Runtime stack
- React 18 frontend served by the development server during local development.
- Java 17 Spring Boot 3.1.5 backend process.
- H2 in-memory database for local development.
- Multipart attachment handling exposed through REST endpoints.

### Component diagram (logical)
```text
+----------------------+      HTTP(S)       +--------------------------------+
| React Frontend       | <----------------> | Spring Boot REST API           |
| - Dashboard view     |                    |                                |
| - Claims view        |                    |  +--------------------------+  |
| - Axios clients      |                    |  | PolicyController         |  |
+----------------------+                    |  | /api/policies*           |  |
                                            |  +--------------------------+  |
                                            |  +--------------------------+  |
                                            |  | ClaimController          |  |
                                            |  | /api/claims*             |  |
                                            |  +--------------------------+  |
                                            |  +--------------------------+  |
                                            |  | Services / Validation    |  |
                                            |  | DTO mapping / JPA        |  |
                                            |  +--------------------------+  |
                                            +---------------+----------------+
                                                            |
                                                            v
                                             +-------------------------------+
                                             | H2 In-Memory Database          |
                                             | tables: policies, claims       |
                                             +-------------------------------+
```

## 4. Module Boundaries and Responsibilities
### 4.1 Frontend Presentation Module
Responsibilities:
- Render dashboard and claims experiences in React.
- Fetch policy and claim data from REST endpoints.
- Support client-side policy switching and claims filtering.

Owned components:
- `Dashboard`
- `Claims`
- `Navigation`

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
- Cascade policy deletion to dependent claims through the persistence layer.

Owned routes:
- GET /api/policies
- GET /api/policies/<pid>
- POST /api/policies
- PATCH /api/policies/<pid>
- DELETE /api/policies/<pid>

### 4.4 Persistence Module
Responsibilities:
- Manage JPA repositories and entity persistence.
- Use H2 for local development data storage.
- Seed initial sample data on application startup.

Primary functions:
- `PolicyRepository`
- `ClaimRepository`
- `DataConfig`

### 4.5 Upload Storage Module
Responsibilities:
- Validate allowed file extensions.
- Accept multipart file uploads for claim creation.
- Persist attachment metadata with the claim record.

## 5. Data Flow
### 5.1 Dashboard and Claims Page Flow
1. Browser loads the React application.
2. React components request policies and claims from the backend API.
3. Spring Boot controllers fetch entities via repositories.
4. DTOs are returned as JSON payloads.
5. React updates the UI with policy and claim state.

### 5.2 API Request Flow
1. Client sends JSON or multipart request to REST endpoint.
2. Route validates required fields and data types.
3. Controller delegates persistence to JPA repositories.
4. Transaction commits on success for mutating operations.
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
5. Server inserts claim record into H2 with:
   - generated claim id
   - status default Pending
   - uploaded file name metadata when present
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
- Minimal explicit logging; relies primarily on Spring Boot default request and application logs.

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
- H2 is sufficient for local development and demos; a production database should replace it for shared environments.
- Query patterns are straightforward and index usage should be reviewed as data grows.

### Reliability
- Local file system uploads create dependency on host disk durability.
- For multi-node deployment, move uploads to object storage.

## 9. Migration Notes
Current schema is managed in-code through JPA entities and startup seed configuration.

Migration strategy recommendations:
1. Introduce explicit migration tooling (for example Flyway or Liquibase) before adding non-trivial schema changes.
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
1. Keep the React + Spring Boot split architecture for current scale and demo simplicity.
2. Keep H2-backed local development data for low operational overhead in MVP.
3. Preserve REST-first APIs so the frontend can evolve independently.

### Unresolved questions
1. Should claim attachments support secure download endpoint and virus scanning?
2. What are retention and deletion policies for uploaded files?
3. Is policy deletion expected to hard delete dependent claims or soft delete both entities?
4. What authentication model and roles are required for production readiness?

## 12. Traceability Matrix

| Feature | HLD module | Data entity |
|---|---|---|
| View dashboard and claims | Frontend Presentation Module | policies, claims |
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
   - Files: src/backend/src/test/java/com/insurewell/ClaimStatusApiTest.java
   - Effort: S
   - Risk: Low
   - Acceptance criteria: Tests cover valid status updates plus 400/404 negative paths.

3. Add request/response structured logging middleware.
   - Files: src/backend/src/main/java/com/insurewell/controller/ApiController.java, src/backend/src/main/java/com/insurewell/config/
   - Effort: M
   - Risk: Medium
   - Acceptance criteria: Every request logs method, path, status, latency, and request id.

4. Add upload hardening checks.
   - Files: src/backend/src/main/java/com/insurewell/controller/ClaimController.java, src/backend/src/test/
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
