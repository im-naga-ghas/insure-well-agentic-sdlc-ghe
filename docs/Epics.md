# InsureWell Epics

## Epic Overview
The epics below translate the business goals in [BRD.md](BRD.md) into implementation-ready product themes for the React + Spring Boot platform.

| Epic ID | Epic Name | Priority | BRD Goal Mapping |
|---|---|---|---|
| E1 | Identity, Access, and Data Protection | P1 | G1, G5 |
| E2 | Policyholder Claims Self-Service | P1 | G2, G4 |
| E3 | Admin Claims Operations Workspace | P1 | G3 |
| E4 | Notifications, Timeline, and Communications | P1 | G2, G5 |
| E5 | Search, Reporting, and Operational Insights | P2 | G3, G5 |
| E6 | Policy Self-Service and Document Experience | P2 | G4 |
| E7 | Household and Extended Coverage Management | P3 | G4 |
| E8 | Provider Discovery and Guided Care Journey | P3 | G4 |

## E1. Identity, Access, and Data Protection
### Objective
Protect policy and claim data through secure authentication, role-based authorization, and foundational auditability.

### Business value
- Establishes trust and privacy safeguards
- Enables separate customer and admin workflows
- Creates the foundation for compliant expansion

### Current gap addressed
- All APIs and UI actions are currently unauthenticated and effectively public

## E2. Policyholder Claims Self-Service
### Objective
Enable policyholders to submit, review, and understand claims without relying on back-office support.

### Business value
- Improves customer experience and transparency
- Reduces support burden
- Increases product completeness for core insurance workflows

### Current gap addressed
- Current claims flow is limited to basic creation and status viewing, with no timeline, attachment workflow, or customer guidance

## E3. Admin Claims Operations Workspace
### Objective
Provide internal users a dedicated interface to review claims, take actions, record notes, and manage workload efficiently.

### Business value
- Improves review throughput and consistency
- Enables operational control over claim lifecycles
- Supports future scaling beyond demo datasets

### Current gap addressed
- Claim status can be changed from the general claims table with no role controls, workflow context, or internal notes

## E4. Notifications, Timeline, and Communications
### Objective
Keep policyholders and admins informed of meaningful status changes and create a visible history of claim actions.

### Business value
- Reduces uncertainty for customers
- Improves traceability for support and operations
- Supports audit-friendly lifecycle tracking

### Current gap addressed
- The application has no notifications, no event history, and no communication log

## E5. Search, Reporting, and Operational Insights
### Objective
Enable internal users to find records quickly and monitor policy and claim performance with actionable analytics.

### Business value
- Improves decision-making and prioritization
- Supports operational monitoring
- Makes the app usable with larger datasets

### Current gap addressed
- There is no search, advanced filtering, reporting dashboard, or export capability

## E6. Policy Self-Service and Document Experience
### Objective
Expand policyholder self-service with downloadable policy documents, clearer coverage views, and guided servicing actions.

### Business value
- Reduces manual support requests
- Improves value perception for policyholders
- Completes common customer tasks inside the product

### Current gap addressed
- Policies can be edited, but customers cannot access generated summaries or supporting coverage documents

## E7. Household and Extended Coverage Management
### Objective
Allow customers to manage dependents or family members under a policy.

### Business value
- Expands addressable insurance scenarios
- Supports more realistic policy structures

### Current gap addressed
- Current model only supports a single holder-centric policy representation

## E8. Provider Discovery and Guided Care Journey
### Objective
Help policyholders find in-network care options and connect coverage information to care-seeking decisions.

### Business value
- Extends the product beyond claims after-the-fact
- Creates stronger everyday engagement with the platform

### Current gap addressed
- The current product begins at policy and claims management with no pre-care guidance workflows