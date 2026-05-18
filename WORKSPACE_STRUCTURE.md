# InsureWell Workspace Organization

## 📁 Current Structure (after reorganization)

```
insure-well-agentic-sdlc-ghe/
│
├── 🆕 src/                          ← NEW: Modern React + Spring Boot Stack
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
├── 📦 legacy/                       ← MOVED: Flask Python Stack (original)
│   ├── app.py                       (Flask main app)
│   ├── requirements.txt             (Python dependencies)
│   ├── templates/                   (Jinja2 HTML templates)
│   │   ├── base.html
│   │   ├── dashboard.html
│   │   └── claims.html
│   ├── static/                      (CSS & JavaScript)
│   │   ├── css/style.css
│   │   └── js/app.js
│   ├── data/                        (SQLite database)
│   │   └── insurewell.db
│   └── uploads/                     (Uploaded documents)
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
├── azure-pipelines.yml              (CI/CD configuration)
└── .gitignore
```

---

## 🚀 Running the Applications

### Modern Stack (React + Spring Boot)
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

### Legacy Stack (Flask)
```bash
cd legacy
source ../.venv/bin/activate
pip install -r requirements.txt
python app.py         # Runs on http://localhost:5001
```

---

## 📊 Technology Comparison

| Aspect | Legacy (Flask) | Modern (React + Spring Boot) |
|--------|----------------|------------------------------|
| **Backend** | Python Flask | Java Spring Boot 3 |
| **Frontend** | Jinja2 + Vanilla JS | React 18 |
| **Database** | SQLite | H2 (in-memory, dev) |
| **API Style** | REST + HTML rendering | REST (JSON) |
| **Package Manager** | pip | Maven + npm |
| **Port** | 5001 | Backend: 8080, Frontend: 3000 |
| **Status** | Stable, archived | Active development |

---

## 📝 Key Changes

✅ **Created:** `src/` folder with complete React + Spring Boot stack  
✅ **Moved:** All Flask Python code to `legacy/` folder  
✅ **Preserved:** Project documentation in `docs/`, `handbook/`, `images/`  
✅ **Maintained:** CI/CD config (`azure-pipelines.yml`), git history

---

## 🔗 Documentation

- **src/README.md** — Full architecture, API docs, setup instructions for modern stack
- **src/QUICKSTART.md** — Fast startup guide for React + Spring Boot
- **legacy/README.md** — (If exists) Legacy Flask documentation
- **docs/** — Data models and architecture diagrams
- **handbook/** — Setup guides and workflow documentation

---

## 📦 Dependencies

### Modern Stack
- **Backend:** Java 17+, Maven 3.9+, Spring Boot 3.1.5
- **Frontend:** Node.js 18+, npm 9+, React 18

### Legacy Stack
- **Python:** 3.9+
- **Dependencies:** Flask, sqlite3 (see `legacy/requirements.txt`)

---

## 🎯 Next Steps

1. **Development:** Use `src/` for all new features (React + Spring Boot)
2. **Reference:** Consult `legacy/` if you need Flask implementation details
3. **Migration:** Gradually migrate legacy features to modern stack
4. **Testing:** Each stack can be tested independently

---

**Last Updated:** May 11, 2026  
**Status:** Workspace successfully reorganized with legacy code archived
