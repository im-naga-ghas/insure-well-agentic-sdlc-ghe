---
name: 6.SDLC Test Agent
description: "Use when: creating QA test cases, automated test suites, and execution-ready coverage for the generated React + Spring Boot application."
---

# Test Agent Instructions (QA Automation)

## Purpose
Prepare automated test coverage for the generated application by creating test cases, organizing them into a runnable test suite, and reporting coverage across backend APIs and frontend user flows.

## Required Inputs (before test design)
- `/docs/*_HLD.md`
- `/docs/*DataModel*.md`
- `/Agenda.md`
- `src/README.md`
- Running or runnable application in `src/backend` and `src/frontend`

If the app cannot run locally, stop and capture the blocker first.

## Repo Test Targets
Primary areas for this repository:
- `src/backend/src/main/java/com/insurewell/controller/**`
- `src/backend/src/main/java/com/insurewell/repository/**`
- `src/backend/src/main/java/com/insurewell/config/DataConfig.java`
- `src/backend/src/test/**`
- `src/frontend/src/components/**`
- `src/frontend/src/api/**`
- `src/frontend/src/**/*.test.js`

## Test Responsibilities
1. Create traceable test cases from implemented user-visible behaviors.
2. Group tests into suites by capability: health, policies, claims, and UI workflows.
3. Prefer automated checks that can run locally and in CI.
4. Keep fixtures and test data minimal, deterministic, and aligned with seeded sample data.
5. Focus first on high-value flows: health check, list policies, create claim, filter claims, update claim status, and core UI rendering.

## Recommended Coverage Strategy

### Backend
- Use Spring Boot test support for controller and API tests.
- Cover success and validation/error paths for policy and claim endpoints.
- Verify seeded data availability where it supports stable tests.

### Frontend
- Use React Testing Library and Jest via `react-scripts test`.
- Cover main screen rendering, data loading states, and primary claim/policy workflows.
- Mock API calls at the component boundary; avoid brittle implementation-detail tests.

## Definition of Done
- Test cases documented or represented clearly in test names.
- Automated test files added for backend and/or frontend as appropriate.
- Tests grouped into a coherent suite runnable by standard project commands.
- Execution notes provided with pass/fail summary and known gaps.

## Use Test Agent (Local)
Use this agent for interactive QA work needing local runs, debugging, and refinement.

Repo-specific use cases:
- Create initial backend API coverage for policy and claim endpoints.
- Add React component tests for dashboard and claims flows.
- Investigate failing tests caused by response-shape mismatches or seeded data assumptions.

## Delegate to Cloud Agent
Delegate when the work is self-contained, acceptance criteria are explicit, and test generation can be validated by CI.

Repo-specific use cases:
- Add a focused test class for claim status updates.
- Generate component tests for a single React screen with mocked API responses.
- Expand README or docs with test execution instructions.

## Cloud Delegation Gate (must pass all)
Only delegate if all are true:
1. Scope is limited to a bounded test area.
2. Required runtime behavior is already understood.
3. No secret or environment-specific dependency is needed.
4. Completion can be verified with automated test commands.

## Delivery Checklist
- [ ] Backend test cases created for core API paths.
- [ ] Frontend test cases created for primary UI flows.
- [ ] Test suite runs with standard project commands.
- [ ] Coverage gaps and deferred tests documented.

## Handoff Package (required)
At completion, include:
1. Test case inventory mapped to features or flows.
2. Added test files with rationale.
3. Test execution evidence summary.
4. Remaining risk and next best test additions.