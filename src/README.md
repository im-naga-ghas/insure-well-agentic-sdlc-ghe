# InsureWell Source Tree

This folder contains the runnable application code for the current InsureWell stack.

## Architecture

```
src/
├── backend/              # Spring Boot REST API (Java 17, Maven)
│   ├── pom.xml           # Maven configuration
│   └── src/main/
│       ├── java/com/insurewell/
│       │   ├── InsureWellApplication.java
│       │   ├── controller/
│       │   │   ├── PolicyController.java
│       │   │   └── ClaimController.java
│       │   ├── model/
│       │   │   ├── Policy.java
│       │   │   └── Claim.java
│       │   ├── dto/
│       │   │   ├── PolicyDTO.java
│       │   │   └── ClaimDTO.java
│       │   ├── repository/
│       │   │   ├── PolicyRepository.java
│       │   │   └── ClaimRepository.java
│       │   └── config/
│       │       └── DataConfig.java
│       └── resources/
│           └── application.properties
│
└── frontend/             # React UI Application
    ├── package.json      # NPM configuration
    ├── public/
    │   └── index.html
    └── src/
        ├── index.js
        ├── App.js        # Root component
        ├── App.css       # Global styles
        ├── components/
        │   ├── Navigation.js
        │   ├── Dashboard.js
        │   └── Claims.js
        ├── styles/
        │   ├── Navigation.css
        │   ├── Dashboard.css
        │   └── Claims.css
        └── api/
```

## Backend

**REST API Endpoints:**

| Method | Endpoint                     | Description                  |
|--------|------------------------------|------------------------------|
| GET    | `/api/policies`              | List all policies            |
| POST   | `/api/policies`              | Create a policy              |
| GET    | `/api/policies/{id}`         | Get a single policy          |
| PATCH  | `/api/policies/{id}`         | Update a policy              |
| DELETE | `/api/policies/{id}`         | Delete a policy              |
| GET    | `/api/claims`                | List claims (filter by policy) |
| POST   | `/api/claims`                | Submit a claim               |
| PATCH  | `/api/claims/{id}/status`    | Update claim status          |
| DELETE | `/api/claims/{id}`           | Delete a claim               |

**Database:**
- In-memory H2 database (development)
- JPA/Hibernate ORM
- Auto-seeded with sample data on startup
- Supports SQLite/PostgreSQL with config changes

**Technology Stack:**
- Java 17
- Spring Boot 3.1.5
- Spring Data JPA
- H2 Database
- Maven
- Lombok (for DTOs and models)

## Frontend

**Pages:**
1. **Dashboard** — View policies, manage policy lifecycle, see recent claims and statistics
2. **Claims** — Submit new claims, view and filter claims, update claim status

**Components:**
- Navigation bar with page switching
- Policy tabs for multi-policy support
- Policy management (add, edit, delete)
- Claim submission form
- Claims table with status updates
- Responsive grid layouts
- Modal dialogs for forms

**Technology Stack:**
- React 18
- Axios for HTTP requests
- Pure CSS (no frameworks) with modern design

## Local Development

### Prerequisites

- **Java 17+**
- **Node.js 18+**
- **Maven 3.9+**

### Run both services

```bash
cd src
./run.sh
```

Frontend: **http://localhost:3000**
Backend API: **http://localhost:8080/api**

### Backend only

```bash
cd src/backend

# Build with Maven
mvn clean package

# Run the application
mvn spring-boot:run
```

The backend will start on **http://localhost:8080/api**.

On startup, H2 runs in memory and seed data is loaded automatically. Health check: `http://localhost:8080/api/health`.

### Frontend only

```bash
cd src/frontend

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will open on **http://localhost:3000** and automatically connect to the backend.

## API Reference

| Method  | Endpoint                      | Description                        |
|---------|-------------------------------|------------------------------------|
| `GET`   | `/api/health`                 | Health check                       |
| `GET`   | `/api/policies`               | List all policies                  |
| `POST`  | `/api/policies`               | Create a policy                    |
| `GET`   | `/api/policies/{id}`          | Get a single policy                |
| `PATCH` | `/api/policies/{id}`          | Update a policy                    |
| `DELETE`| `/api/policies/{id}`          | Delete a policy                    |
| `GET`   | `/api/claims`                 | List all claims                    |
| `GET`   | `/api/claims?policy_id=<id>`  | Filter claims by policy            |
| `POST`  | `/api/claims`                 | Submit a new claim (multipart)     |
| `PATCH` | `/api/claims/{id}/status`     | Update claim status                |
| `DELETE`| `/api/claims/{id}`            | Delete a claim                     |

### Claim submission payload

`POST /api/claims` expects `multipart/form-data` with:

| Field         | Type    | Required | Notes                  |
|---------------|---------|----------|------------------------|
| `policy_id`   | string  | Yes      | e.g. `POL-2024-001`    |
| `amount`      | number  | Yes      | Positive decimal       |
| `description` | string  | Yes      | Free text              |
| `file`        | file    | No       | PDF / JPG / PNG ≤ 5 MB |

### Claim status payload

`PATCH /api/claims/{id}/status` expects:

```json
{ "status": "Approved" }
```

Valid values: `Pending`, `Approved`, `Rejected`.

## Data Model

### Policy
```json
{
  "id": "POL-2024-001",
  "holderName": "Alex Johnson",
  "planName": "InsureWell Premium Health Plan",
  "coverageAmount": 250000,
  "status": "active",
  "startDate": "2024-01-01",
  "endDate": "2026-12-31",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

### Claim
```json
{
  "id": "CLM-1715787000000",
  "policyId": "POL-2024-001",
  "amount": 1500.0,
  "description": "Doctor visit for annual checkup",
  "status": "Approved",
  "fileName": null,
  "submittedAt": "2024-05-15T14:30:00.000Z",
  "updatedAt": "2024-05-15T14:30:00.000Z"
}
```

## API Examples

### Get all policies
```bash
curl http://localhost:8080/api/policies
```

### Create a policy
```bash
curl -X POST http://localhost:8080/api/policies \
  -H "Content-Type: application/json" \
  -d '{
    "holderName": "Jane Doe",
    "planName": "Premium Plus",
    "coverageAmount": 300000,
    "status": "active",
    "startDate": "2024-01-01",
    "endDate": "2025-12-31"
  }'
```

### Submit a claim
```bash
curl -X POST http://localhost:8080/api/claims \
  -F "policy_id=POL-2024-001" \
  -F "amount=500" \
  -F "description=Urgent care visit"
```

### Update claim status
```bash
curl -X PATCH http://localhost:8080/api/claims/CLM-1715787000000/status \
  -H "Content-Type: application/json" \
  -d '{"status": "Approved"}'
```

## Development Notes

- **CORS:** Backend has CORS enabled for `*` origins (change in `ClaimController` and `PolicyController` for production)
- **Database:** Switch from H2 to PostgreSQL by adding the driver dependency and updating `application.properties`
- **File Uploads:** Currently stubbed in claims creation; implement by adding multipart file handling in `ClaimController`
- **Error Handling:** Global exception handler can be added via `@ControllerAdvice`
- **Testing:** Add unit tests in `src/test/` using JUnit 5 and Mockito

## Sample Data

On each local start, the backend seeds these policies:

| Policy ID    | Holder       | Plan                           | Coverage | Status   |
|--------------|--------------|--------------------------------|----------|----------|
| POL-2024-001 | Alex Johnson | InsureWell Premium Health Plan | $250,000 | Active   |
| POL-2024-002 | Maria Garcia | InsureWell Essential Care Plan | $150,000 | Active   |
| POL-2023-009 | David Chen   | InsureWell Family Plus Plan    | $500,000 | Inactive |

Seven sample claims are also seeded across the first two policies.

Because H2 is configured in memory for local development, restarting the backend resets the data back to this seeded state.

## Next Steps

1. Add comprehensive error handling and validation
2. Implement JWT authentication
3. Add file upload support for claim documents
4. Create unit and integration tests
5. Deploy backend to cloud (AWS/Azure/GCP)
6. Deploy frontend to CDN or static hosting
7. Add Redux for state management if app grows
8. Implement admin dashboard

## Support

For project overview and repo-level onboarding, refer to the main [README.md](../README.md) at the repository root.
