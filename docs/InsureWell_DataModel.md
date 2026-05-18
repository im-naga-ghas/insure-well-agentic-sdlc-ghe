# InsureWell Data Model

## 1. Data Model Scope
This document defines the current persistent entities, relationships, constraints, and migration guidance for InsureWell.

## 2. Physical Storage
- Primary store: H2 in-memory database for local development.
- Auxiliary attachment metadata is stored with claim records; uploaded files are optional in the current API.

## 3. Entity Model

## 3.1 policies
Purpose:
- Stores health insurance policy records.

Columns:
- id (TEXT, PK)
- holder_name (TEXT, NOT NULL)
- plan_name (TEXT, NOT NULL)
- coverage_amount (REAL, NOT NULL)
- status (TEXT, NOT NULL, default active)
- start_date (TEXT, NOT NULL)
- end_date (TEXT, NOT NULL)
- created_at (TEXT, NOT NULL)

Business rules:
- status domain: active or inactive.
- coverage_amount must be positive.
- id generated as POL-<year>-<sequence>.

## 3.2 claims
Purpose:
- Stores claim submissions associated with policies.

Columns:
- id (TEXT, PK)
- policy_id (TEXT, NOT NULL, FK to policies.id)
- amount (REAL, NOT NULL)
- description (TEXT, NOT NULL)
- status (TEXT, NOT NULL, default Pending)
- file_name (TEXT, nullable)
- submitted_at (TEXT, NOT NULL)
- updated_at (TEXT, NOT NULL)

Business rules:
- policy_id must reference existing policy.
- amount must be positive.
- status domain: Pending, Approved, Rejected.
- id generated as CLM-<epoch-millis>.

## 4. Relationships
- policies (1) to claims (N)
  - claims.policy_id references policies.id.
  - Policy delete behavior is handled by application logic:
    - delete dependent claims first.
    - delete policy second.

## 5. Logical ER View
```text
+-------------------+             +-------------------+
| policies          | 1 ------ N  | claims            |
+-------------------+             +-------------------+
| id (PK)           |             | id (PK)           |
| holder_name       |             | policy_id (FK)    |
| plan_name         |             | amount            |
| coverage_amount   |             | description       |
| status            |             | status            |
| start_date        |             | file_name         |
| end_date          |             | submitted_at      |
| created_at        |             | updated_at        |
+-------------------+             +-------------------+
```

## 6. Data Integrity Constraints
Current enforced constraints:
- Primary key uniqueness on policy and claim ids.
- Foreign key integrity for claims.policy_id.
- NOT NULL constraints on core required fields.

Current app-layer validations:
- Positive numeric values for coverage and claim amounts.
- Allowed enum-like values for policy status and claim status.

Recommended DB-level hardening:
1. Add CHECK constraints for status columns.
2. Add CHECK constraints for positive numeric fields.
3. Add index on claims.policy_id for filtered claim retrieval.
4. Add index on claims.submitted_at for descending history queries.

## 7. Upload Metadata Model
Current model stores only original file_name in claims.
Physical file is persisted under uploads with a generated timestamp-prefixed path.

Recommendations:
1. Add stored_file_name column for deterministic retrieval.
2. Add file_size_bytes and mime_type columns for auditability.
3. Add created_by or source metadata once auth is introduced.

## 8. CRUD Behavior Matrix

| Entity | Create | Read | Update | Delete |
|---|---|---|---|---|
| policies | POST /api/policies | GET /api/policies, GET /api/policies/<pid> | PATCH /api/policies/<pid> | DELETE /api/policies/<pid> |
| claims | POST /api/claims | GET /api/claims | PATCH /api/claims/<cid>/status | DELETE /api/claims/<cid> |

## 9. Migration Notes
Current state:
- Schema bootstrap is derived from the Spring Boot JPA model and startup configuration.

Planned evolution:
1. Introduce explicit versioned migrations.
2. Add migration scripts for indexes and CHECK constraints.
3. Add forward-only migration policy for non-dev environments.
4. Add lightweight data quality checks as post-migration verification.

## 10. Data Lifecycle and Retention Considerations
- Claims and policy rows currently use hard delete semantics.
- Attachment files are not explicitly removed in delete flows today and should be evaluated for cleanup policy.

Recommended policy decisions:
1. Define retention period by regulatory needs.
2. Evaluate soft delete strategy for auditability.
3. Add periodic orphan-file cleanup job for uploads folder.

## 11. Validation and Test Expectations
Critical data-model test cases:
1. Reject claim creation when policy_id does not exist.
2. Reject negative and zero amounts.
3. Reject invalid status transitions outside allowed domains.
4. Ensure policy delete removes dependent claims.
5. Ensure upload metadata remains consistent with saved files.

## 12. Assumptions and Open Questions
Assumptions:
1. Single-tenant data model for current MVP.
2. Timestamps remain ISO-like text in UTC format.
3. Data volume remains small enough for an in-memory H2 development store.

Open questions:
1. Should claim status history be modeled as a separate table?
2. Should policy and claim ids move to UUIDs for distributed safety?
3. Is immutable audit logging required for compliance?
