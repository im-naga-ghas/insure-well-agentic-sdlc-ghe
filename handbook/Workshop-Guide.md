# InsureWell — Workshop Guide

> Single reference for prerequisites, setup, agent delegation, and the demo walkthrough.

---

## 1. Prerequisites

| Requirement | Version |
|---|---|
| Java | 17+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| npm | 9+ |
| VS Code | Latest |
| GitHub Copilot + Chat extensions | Latest |
| Git | Configured (`user.name`, `user.email`) |

### Verify

```bash
java --version        # 17+
mvn --version         # 3.9+
node --version        # 18+
npm --version         # 9+
```

### Required VS Code Extensions

- GitHub Copilot
- GitHub Copilot Chat
- GitHub Pull Requests and Issues
- Extension Pack for Java
- Spring Boot Extension Pack
- ES7+ React/Redux/React-Native snippets

### Accounts & Access

- GitHub Enterprise organization with Copilot subscription
- GitHub PAT (scopes: `repo`, `read:org`, `workflow`)
- *(Optional)* Azure DevOps PAT (scopes: Work Items, Code, Build, Project) — only if using ADO integration

---

## 2. MCP Server Setup

### GitHub MCP Server

1. Open VS Code Command Palette (`Cmd+Shift+P`).
2. Run **"MCP: Add Server"** → select **GitHub**.
3. Authenticate when prompted.
4. Confirm green status indicator in the status bar.

Or add manually to `.vscode/mcp.json`:

```json
{
  "servers": {
    "github": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${input:github-pat}"
      }
    }
  }
}
```

### Playwright MCP Server (for browser testing)

Already configured in `.vscode/mcp.json`. Verify browser tools appear under the Tools (plug) icon in Copilot Chat. Install Chromium if needed:

```bash
npx playwright install chromium
```

### Azure DevOps MCP Server *(optional)*

See [Azure DevOps MCP setup](https://github.com/mcp/microsoft/azure-devops-mcp) for detailed instructions. Add to `.vscode/mcp.json` with your organization URL and PAT.

---

## 3. Clone, Build & Run

```bash
git clone https://github.com/im-naga-ghas/insure-well-agentic-sdlc-ghe.git
cd insure-well-agentic-sdlc-ghe/src
chmod +x run.sh
./run.sh
```

Open **http://localhost:3000**. The Spring Boot backend starts on **http://localhost:8080/api** with an in-memory H2 database seeded with sample policies and claims.

**Manual start (if needed):**

```bash
# Terminal 1 — Backend
cd src/backend && mvn spring-boot:run

# Terminal 2 — Frontend
cd src/frontend && npm install && npm start
```

**Verify:**

```bash
curl http://localhost:8080/api/policies   # JSON array
curl http://localhost:3000                # HTML page
```

---

## 4. Agent Delegation Modes

| Mode | When to use | How it works |
|---|---|---|
| **Local Agent** (Agent Mode in VS Code) | Interactive coding, debugging, refactoring | Sees workspace, terminal, uncommitted changes; you steer every step |
| **Cloud Agent** (from VS Code) | Async task handoff while you keep coding | Copilot works in background; results in a PR |
| **GitHub Issue → Copilot** | Tracked backlog work | Issue description drives implementation; visible to the whole team |

**Rule of thumb:** Local = interactive, now. Cloud = offload, async. GitHub Issue = track, team.

---

## 5. Custom Agents

Agents are `.agent.md` files in `.github/agents/`. Each encapsulates a focused SDLC role.

| Agent | Role |
|---|---|
| `1.SDLC BRD Agent` | Business requirements, epics, features |
| `2.SDLC HLD Agent` | High-level design, component boundaries, data flow |
| `5.SDLC Dev Agent` | Feature implementation (React + Spring Boot) |
| `6.SDLC Test Agent` | Test generation, CI triage, coverage |

Create agents with `/create-agent` in Copilot Chat or by adding `.agent.md` files directly.

---

## 6. Demo Flow

### Step 1 — Generate HLD

```
@2.SDLC HLD Agent Generate a High-Level Design for InsureWell.
Include: component diagram, data flow between React frontend and Spring Boot REST API,
JPA persistence layer, and the claims submission/update API surface.
```

### Step 2 — Generate Requirements

```
@1.SDLC BRD Agent Analyze the InsureWell application (src/backend, src/frontend, README.md).
Identify gaps and produce a prioritized backlog of business requirements, epics, and features.
```

### Step 3 — Create GitHub Issues

```
@github Create GitHub issues for the following user stories in
im-naga-ghas/insure-well-agentic-sdlc-ghe with labels (enhancement, frontend, backend):
[paste story list]
```

### Step 4 — Implement a Feature

```
@5.SDLC Dev Agent Implement user authentication for InsureWell.
- Spring Security login for the backend
- Users model with JPA/H2
- React sign-in/sign-out flows
- Protect claims and policy actions behind auth
- Seed a default admin user
- Add backend and frontend tests
```

### Step 5 — Add Tests

```
@6.SDLC Test Agent Review the PR for the authentication feature.
Generate tests covering login success/failure, protected routes, and logout behavior.
```

### Step 6 — Review & Merge

Use the review agent or GitHub's built-in Copilot code review on the PR. Merge after approval.

### Full Cycle

```
Clone & Run → MCP Setup → Custom Agents
  → HLD Agent → BRD Agent → Create Issues
  → Dev/Cloud Agent → QA Agent → Review → Merge
  → Repeat for next story
```

---

## 7. Agentic SDLC Workflow (Visual)

The workflow follows this sequence of automated steps:

1. **Request Flow** — Issue triggers agent processing and code generation
2. **Service Connection** — Secure link between GitHub and Azure DevOps
3. **Copilot Code Review** — Automated review on generated code
4. **Status Checks** — CI/CD pipeline validations
5. **Draft PR** → **Ready for Review** → **Fix & Commit** → **Merge**

Key outcomes: automated issue-to-PR workflow, continuous code review, self-fixing via Copilot delegation, reduced manual review cycles.

---

## 8. Playwright MCP Workshop

Use Playwright MCP browser tools to validate UI behavior interactively from Copilot Chat.

**Available tool categories:** Navigation (`browser_navigate`), Interaction (`browser_click`, `browser_fill_form`, `browser_type`), Inspection (`browser_snapshot`, `browser_take_screenshot`).

**Always use `data-testid` selectors.** See [playwright-mcp-setup-working.md](guides/6.playwright-mcp-setup-working.md) for the full selector reference.

**Workshop flow:**
1. Start the app (backend + frontend)
2. Smoke-test navigation
3. Test Dashboard: click policy tabs, open Add Policy modal, fill and save
4. Test Claims: navigate to Claims, submit a new claim, filter, update status

---

## Reference Links

- [GitHub MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/github)
- [VS Code Copilot Agent Mode](https://code.visualstudio.com/docs/copilot/chat/chat-agent-mode)
- [Copilot Custom Instructions](https://code.visualstudio.com/docs/copilot/copilot-customization)
- [InsureWell REST API](../src/README.md)
- [InsureWell Data Model](../docs/InsureWell_DataModel.md)
- [InsureWell HLD](../docs/InsureWell_HLD.md)
