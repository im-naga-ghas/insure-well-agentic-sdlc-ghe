# InsureWell Workspace Organization

## 📁 Project Structure

```
insure-well-agentic-sdlc-ghe/
│
├── src/                             ← React + Spring Boot Stack
│   ├── backend/                     (Spring Boot 3, Java 17, REST API)
│   │   ├── pom.xml
│   │   └── src/main/java/com/insurewell/
│   │       ├── controller/          (REST endpoints)
│   │       ├── model/               (JPA entities)
│   │       ├── repository/          (Data access)
│   │       ├── dto/                 (Data transfer objects)
│   │       ├── config/              (App configuration & seed data)
│   │       └── InsureWellApplication.java
│   │
│   ├── frontend/                    (React 18 SPA)
│   │   ├── package.json
│   │   ├── public/
│   │   └── src/
│   │       ├── components/          (Dashboard, Claims, Navigation)
│   │       ├── styles/              (CSS modules)
│   │       └── App.js
│   │
│   ├── README.md                    (Full setup & API reference)
│   ├── QUICKSTART.md                (Fast startup guide)
│   ├── run.sh                       (Startup script)
│   └── .gitignore
│
├── docs/                            (Project documentation)
│   ├── InsureWell_DataModel.md
│   └── InsureWell_HLD.md
│
├── handbook/                        (Setup & workflow guides)
│   ├── guides/
│   │   ├── 3.Understand_Workflow.md
│   │   ├── 4.Copilot-Agent-Delegation-Guide.md
│   │   └── 5.Demo-Flow.md
│   └── setup/
│       ├── 1.Prerequisites.md
│       └── 2.Azure-DevOps-Setup.md
│
├── images/                          (Project screenshots & diagrams)
│   └── *.png, *.html
│
├── README.md                        (Main project README)
├── Agenda.md                        (Project agenda)
└── .gitignore
```

---

## 🚀 Running the Application

```bash
cd src
./run.sh              # Starts both backend (8080) & frontend (3000)
```

**Or manually:**
```bash
# Terminal 1 - Backend
cd src/backend
mvn spring-boot:run   # Runs on http://localhost:8080/api

# Terminal 2 - Frontend
cd src/frontend
npm install
npm start             # Runs on http://localhost:3000
```

---

## 🛠 Technology Stack

| Aspect | Details |
|--------|---------|
| **Backend** | Java Spring Boot 3 |
| **Frontend** | React 18 |
| **Database** | H2 (in-memory, dev) |
| **API Style** | REST (JSON) |
| **Package Manager** | Maven + npm |
| **Ports** | Backend: 8080, Frontend: 3000 |

---

## 🔗 Documentation

- **src/README.md** — Full architecture, API docs, setup instructions
- **src/QUICKSTART.md** — Fast startup guide
- **docs/** — Data models and architecture diagrams
- **handbook/** — Setup guides and workflow documentation

---

## 📦 Dependencies

- **Backend:** Java 17+, Maven 3.9+, Spring Boot 3.1.5
- **Frontend:** Node.js 18+, npm 9+, React 18

---

**Last Updated:** May 19, 2026
