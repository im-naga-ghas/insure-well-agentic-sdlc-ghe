# InsureWell Business Requirements Document

## 1. Purpose
InsureWell currently delivers a Phase 1 MVP for managing health insurance policies and claims through a React frontend and Spring Boot backend. The product supports policy CRUD, claim submission, claim status updates, and seeded demo data, but it does not yet provide the security, workflow controls, communications, reporting, and operational capabilities needed for a production-ready insurance experience.

This BRD defines the next set of business requirements to evolve InsureWell from a demo-oriented MVP into a secure, role-aware, auditable, and scalable insurance servicing platform.

## 2. Current-State Summary
The current application provides:
- Policy dashboard with policy selection, summary metrics, and recent claims
- Policy create, edit, and delete actions
- Claim submission with amount and description
- Claim status updates and claim deletion
- REST APIs for policy and claim operations
- Seeded sample data backed by an in-memory H2 database

The current application does not provide:
- Authentication or authorization
- Distinct user roles such as policyholder, claims adjuster, and admin
- True document upload handling and attachment lifecycle management
- Notifications for claim lifecycle changes
- Reporting and analytics views for operational users
- Audit trail, claim notes, or workflow history
- Search, advanced filtering, pagination, or export capabilities
- Production-grade validation, observability, and compliance controls

## 3. Business Problem
Health insurance servicing requires secure access, transparent claim workflows, timely communications, and operational visibility. Without these capabilities, InsureWell cannot safely support real users, demonstrate end-to-end servicing scenarios, or scale beyond a local MVP. The lack of role separation, auditability, and communication tooling also limits the system's usefulness for both customer self-service and internal claims operations.

## 4. Business Goals
| Goal ID | Goal | Success Measure |
|---|---|---|
| G1 | Secure access to insurance data | 100% of user-facing policy and claim actions require authenticated access appropriate to role |
| G2 | Improve claim transparency and turnaround | Users can view claim status, history, required actions, and notifications for 100% of submitted claims |
| G3 | Increase operational efficiency for administrators | Admin users can search, filter, review, and act on policies and claims from dedicated workflows |
| G4 | Improve customer self-service completeness | Policyholders can complete the most common servicing tasks without support intervention |
| G5 | Establish production-ready platform foundations | Core workflows meet planning-level expectations for auditability, availability, security, and maintainability |

## 5. Personas
- Policyholder: views personal policies, submits claims, uploads supporting documents, checks status, and receives updates
- Claims Adjuster/Admin: reviews claims, updates statuses, requests information, and monitors operational queues
- Operations Manager: monitors trends, approval rates, and workload across policies and claims
- Support Agent: assists customers with account access, policy lookup, and claim troubleshooting

## 6. In Scope
- Authentication and authorization for policyholder and admin experiences
- Policyholder self-service improvements around claims and policy documents
- Admin workflows for claim review, search, filtering, and reporting
- Notifications and status communication for claim lifecycle events
- Claim history, notes, and audit visibility
- Planning-level non-functional requirements covering security, performance, usability, observability, and compliance-readiness

## 7. Out of Scope
- Payment processing or premium billing
- Provider directory and external provider integrations
- Live adjudication rules engine or fraud scoring
- Full enterprise identity federation and SSO
- Mobile-native applications
- Production deployment topology design beyond planning-level requirements

## 8. Key Gaps Identified
- No user accounts, login flow, or role-based access
- Any user can currently create, update, or delete policies and claims
- Claim attachments are represented in data but not fully handled in the API/UI workflow
- No notifications for claim submission, approval, rejection, or requests for more information
- No reporting dashboard for administrators or operations stakeholders
- No search, sort, pagination, or saved filters for larger datasets
- No policy document generation or downloadable records
- No claim timeline, notes, or explanation of status changes
- Minimal validation and no global error handling model
- No audit trail, retention controls, or privacy/compliance guardrails

## 9. Prioritization Framework
Priority definitions:
- P1: Required for secure and credible core product usage
- P2: Strong value-add for usability and operations, important for next release
- P3: Strategic enhancements after core foundation is stable

## 10. Prioritized Requirements Backlog
| Priority | Requirement | Rationale |
|---|---|---|
| P1 | User authentication and role-based authorization | Required to protect policy and claim data |
| P1 | Policyholder claim history, status visibility, and timeline | Required for customer transparency |
| P1 | Admin claim review workspace with notes and action controls | Required for internal operations |
| P1 | Real document upload and attachment metadata management | Required for realistic claims workflow |
| P1 | Notification triggers for key claim events | Required for timely user communication |
| P2 | Search, filter, sort, and pagination across policies and claims | Needed for scale and admin efficiency |
| P2 | Reporting dashboard for claim volume, status mix, and approval trends | Needed for oversight and decisions |
| P2 | Downloadable policy summary and claim receipt documents | Improves self-service completeness |
| P2 | Improved validation, error handling, and empty-state UX | Reduces failure ambiguity and support effort |
| P3 | Family/member management under a policy | Extends household use cases |
| P3 | Provider search and referral support | Broadens product utility |
| P3 | Preferences center for communication channels and notification settings | Improves personalization |

## 11. Non-Functional Requirements
### Security
- All policy and claim data access must require authentication
- Authorization must enforce role-based restrictions for customer and admin actions
- Sensitive actions must be auditable
- File uploads must enforce allowed types, size limits, and malware-scanning integration points

### Performance
- Common dashboard and claims list views should load within 2 seconds for typical demo-scale datasets
- Search and filter actions should return results within 2 seconds under expected admin usage

### Usability
- Primary customer tasks must be completable without training
- Status language must be understandable to non-technical users
- Empty, loading, and error states must guide the user clearly

### Reliability
- Core workflows should fail gracefully with recoverable messaging
- Seed/demo data should remain available for local demonstration and testing

### Observability
- API requests, errors, and status transitions should be logged with enough detail for troubleshooting
- Operational metrics should support basic reporting and health monitoring

### Compliance Readiness
- Planning should support future HIPAA/privacy reviews through access controls, audit logs, and data retention decisions

## 12. Risks and Mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| Adding auth and roles may ripple through backend and frontend flows | High | Deliver identity and access in an early vertical slice before feature expansion |
| Claim attachments can introduce security and storage concerns | High | Define upload restrictions, scanning hooks, and metadata model before implementation |
| Analytics needs may outgrow current data model | Medium | Add reporting requirements and event history before building dashboards |
| Demo-oriented H2 storage may hide production constraints | Medium | Keep persistence abstractions clean and document migration assumptions |
| Broad backlog could dilute delivery focus | Medium | Sequence work by P1 foundation, then P2 operations, then P3 expansion |

## 13. Assumptions
- The current React frontend and Spring Boot backend remain the target implementation stack
- Initial releases can continue using local/demo-friendly infrastructure while keeping future migration paths open
- Admin and policyholder experiences can live within the same web application with role-based navigation
- Email is the first notification channel; SMS and push are future options

## 14. Traceability
| BRD Goal | Mapped Epic IDs |
|---|---|
| G1 | E1 |
| G2 | E2, E4 |
| G3 | E3, E5 |
| G4 | E2, E6 |
| G5 | E1, E4, E5 |

## 15. Handoff Package
### Completed artifacts list
- BRD.md
- Epics.md
- Features.md

### Open questions and assumptions
- Will the first authenticated release support self-registration, admin-provisioned users, or both?
- Should claim review roles be limited to admins, or do we need a separate adjuster persona?
- What retention policy should apply to uploaded claim documents?
- Are downloadable documents informational only, or do they need branded/legal formatting?

### Cloud Delegation Candidates
1. Task name: API contract test scaffold for claims and policies
   Files likely touched: `src/backend/src/test/`, `src/README.md`
   Effort: M
   Risk: Low
   Acceptance criteria: contract tests cover current policy and claim endpoints with positive and negative cases

2. Task name: Planning doc cross-link cleanup
   Files likely touched: `docs/BRD.md`, `docs/Epics.md`, `docs/Features.md`, `README.md`
   Effort: S
   Risk: Low
   Acceptance criteria: planning docs link to each other and the README documentation map references them clearly

3. Task name: Global validation and error response conventions
   Files likely touched: `src/backend/src/main/java/com/insurewell/controller/`, `src/backend/src/main/java/com/insurewell/dto/`
   Effort: M
   Risk: Medium
   Acceptance criteria: API returns consistent error payloads and validation failures across policies and claims

4. Task name: Claims workflow audit event design spike
   Files likely touched: `docs/InsureWell_DataModel.md`, `docs/Features.md`
   Effort: S
   Risk: Medium
   Acceptance criteria: audit event entities and lifecycle states are defined for future implementation

5. Task name: Local upload handling technical spike
   Files likely touched: `src/backend/src/main/java/com/insurewell/controller/ClaimController.java`, `src/README.md`
   Effort: M
   Risk: Medium
   Acceptance criteria: a technical proposal or implementation path exists for multipart upload handling with validation rules

### Recommended next agent
`2.SDLC HLD Agent`