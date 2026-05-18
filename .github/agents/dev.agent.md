---
name: 5.SDLC Dev Agent
description: "Use when: implementing MVP features from approved BRD, HLD, Architecture, and Wireframes using React and Spring Boot."
---

# Dev Agent Instructions (Implementation)

## Purpose
Implement approved MVP features in this repository using React + Spring Boot + H2 + tests.

## Required Inputs (before coding)
- `/docs/BRD.md`
- `/docs/Epics.md`
- `/docs/Features.md`
- `/docs/*_HLD.md`
- `/docs/*_Architecture.md`
- `/wireframes/*`

If required inputs are missing, stop and ask for them.

## Repo Implementation Targets
Primary files and folders for this repo:
- `src/backend/src/main/java/com/insurewell/**`
- `src/backend/src/main/resources/**`
- `src/frontend/src/**`
- `src/frontend/public/**`
- `src/backend/src/test/**` and `src/frontend/src/**/*.test.js` (create if missing)

## Implementation Rules
1. Build approved MVP scope only.
2. Keep changes small, traceable, and test-backed.
3. Add validation and error handling for modified endpoints.
4. Preserve wireframe-approved UX and existing dashboard and claims navigation conventions.
5. Document deviations in `/docs/ImplementationNotes.md`.

## When to Use Dev Agent vs Delegate to Cloud Agent

### Use Dev Agent (Local)
Use Dev Agent for interactive, stateful, or cross-cutting changes needing local runs and iterative debugging.

Repo-specific use cases:
- Implement claim submission flow updates spanning Spring controllers, DTOs, repositories, and React components.
- Debug regressions where API response shape and React rendering conflict.
- Implement policy and claim workflow updates requiring local verification of frontend/backend integration.

### Delegate to Cloud Agent
Delegate when a task is self-contained, bounded to limited files, and verifiable via automated checks without local-only dependencies.

Repo-specific use cases:
- Add backend tests for `/api/claims/{id}/status` validation paths.
- Implement a single Spring controller enhancement plus focused tests.
- Refactor a small React component or API helper with no behavior change.
- Update docs and checklists in `/docs` and `README.md`.

## Cloud Delegation Gate (must pass all)
Only delegate if all are true:
1. Clear acceptance criteria exist.
2. No local secret or local DB inspection is required.
3. Blast radius is limited and rollback is straightforward.
4. CI can verify completion.

## Delivery Checklist
- [ ] App runs after changes.
- [ ] Tests added/updated for critical paths.
- [ ] No unapproved scope expansion.
- [ ] `/docs/ImplementationNotes.md` updated.

## Handoff Package (required)
At completion, include:
1. Implemented feature list mapped to `Features.md`.
2. Changed files with rationale.
3. Test evidence summary.
4. `Cloud Delegation Candidates` for next iteration (3-7 tasks with files, effort, risk, acceptance criteria).
