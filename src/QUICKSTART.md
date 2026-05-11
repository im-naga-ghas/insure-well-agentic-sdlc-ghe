# Quick Start Guide — InsureWell React + Spring Boot

## One-Command Startup (macOS/Linux)

```bash
cd src
chmod +x run.sh
./run.sh
```

This script will:
1. Start Spring Boot backend on **port 8080**
2. Auto-wait for backend readiness
3. Start React frontend on **port 3000**
4. Display both URLs when ready

---

## Manual Startup (If Script Fails)

### Terminal 1: Start Backend

```bash
cd src/backend
mvn spring-boot:run
```

Wait for the log:
```
Tomcat started on port(s): 8080
Started InsureWellApplication
```

### Terminal 2: Start Frontend

```bash
cd src/frontend
npm install
npm start
```

The browser will auto-open to **http://localhost:3000**.

---

## Verify Everything Works

### 1. Check Backend Health
```bash
curl http://localhost:8080/api/claims/health
# Response: {"status":"ok"}
```

### 2. Get Sample Data
```bash
curl http://localhost:8080/api/policies | jq
```

Should return 3 sample policies.

### 3. Open Frontend
Navigate to **http://localhost:3000** in your browser.

---

## What's Implemented

✅ **Backend (Spring Boot)**
- REST API for policies and claims  
- H2 in-memory database with auto-seeded sample data
- CRUD operations with validation
- JPA repositories and DTOs

✅ **Frontend (React)**
- Dashboard with policy management
- Claims submission and tracking
- Multi-policy support with tabs
- Real-time status updates
- Responsive design

---

## File Locations

- **Backend Code:** `src/backend/src/main/java/com/insurewell/`
- **Frontend Code:** `src/frontend/src/`
- **Styles:** `src/frontend/src/styles/`
- **Configuration:** 
  - Backend: `src/backend/src/main/resources/application.properties`
  - Frontend: `src/frontend/package.json`

---

## Common Issues

### Backend won't start: "Port 8080 in use"
```bash
# Kill process on port 8080
lsof -ti :8080 | xargs kill -9
```

### Frontend won't connect to backend
- Make sure backend is running on `http://localhost:8080`
- Check browser console (F12) for CORS errors
- Verify firewall allows localhost connections

### Maven not found
```bash
brew install maven  # macOS
# or download from https://maven.apache.org/download.cgi
```

### npm modules missing
```bash
cd src/frontend
rm -rf node_modules package-lock.json
npm install
```

---

## Full Documentation

See [src/README.md](src/README.md) for:
- Complete API reference
- Architecture diagrams
- Data models
- Development notes
- Next steps for production

---

## Tech Stack Summary

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend   | Spring Boot | 3.1.5   |
| Runtime   | Java | 17+     |
| Frontend  | React | 18.2    |
| Build     | Maven / npm | Latest  |
| Database  | H2 (dev) | In-memory |
| HTTP     | REST | Axios  |

---

Ready to build? **Start the backend first, then frontend!** 🚀
