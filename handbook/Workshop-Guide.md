# InsureWell — Workshop Guide

> **Your single reference for the full workshop.** Follow the sections in order. Each section links to deeper guides when you need more detail.

---

## Table of Contents

1. [What Is This Workshop?](#1-what-is-this-workshop)
2. [Prerequisites](#2-prerequisites)
3. [Clone, Build & Run the App](#3-clone-build--run-the-app)
4. [MCP Server Setup](#4-mcp-server-setup)
5. [Custom Agents](#5-custom-agents)
6. [Agent Delegation Modes](#6-agent-delegation-modes)
7. [Demo Flow — Step by Step](#7-demo-flow--step-by-step)
8. [Agentic SDLC Workflow (Visual)](#8-agentic-sdlc-workflow-visual)
9. [Playwright MCP Workshop](#9-playwright-mcp-workshop)
10. [Reference Links](#10-reference-links)

---

## 1. What Is This Workshop?

This workshop shows you how to use **GitHub Copilot agents** to automate every phase of software development — from requirements through coding, testing, and code review — in a real React + Spring Boot application called **InsureWell**.

**What you will do:**

- Run a full-stack web app on your laptop
- Connect AI agents to GitHub via the Model Context Protocol (MCP)
- Use specialized agents (BRD, HLD, Dev, QA) to generate requirements, design, code, and tests
- Review and merge an AI-generated pull request

**You do not need prior AI or agent experience.** Each step is explained from scratch.

[↑ Back to top](#table-of-contents)

---

## 2. Prerequisites

Complete the [Prerequisites Checklist](setup/1.Prerequisites.md) **before** the session starts.

**Quick summary — you need:**

| Requirement | Version |
|---|---|
| Java (JDK) | 17+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| npm | 9+ |
| VS Code | Latest |
| GitHub Copilot + Chat extensions | Latest |
| Git | Configured (`user.name`, `user.email`) |

**Verify in your terminal:**

```bash
java --version        # 17+
mvn --version         # 3.9+
node --version        # 18+
npm --version         # 9+
```

**Required VS Code Extensions** (install from Marketplace `Cmd+Shift+X`):

- GitHub Copilot
- GitHub Copilot Chat
- GitHub Pull Requests and Issues
- Extension Pack for Java
- Spring Boot Extension Pack
- ES7+ React/Redux/React-Native snippets

**Accounts & Access:**

- GitHub Enterprise organization with Copilot subscription
- GitHub PAT (scopes: `repo`, `read:org`, `workflow`)
- *(Optional)* Azure DevOps PAT — only needed for the ADO integration segment

> **Stuck on prerequisites?** See the [detailed prerequisites guide](setup/1.Prerequisites.md) for step-by-step instructions including how to create a GitHub PAT.

[↑ Back to top](#table-of-contents)

---

## 3. Clone, Build & Run the App

> **Why this step?** You need the application running on your laptop so agents can interact with it and so you can see the results of generated code.

### Quick Start

```bash
git clone https://github.com/im-naga-ghas/insure-well-agentic-sdlc-ghe.git
cd insure-well-agentic-sdlc-ghe/src
chmod +x run.sh
./run.sh
```

Open **http://localhost:3000** in your browser. You should see the InsureWell dashboard.

> **What just started?**
> - **Backend** (`http://localhost:8080/api`) — Spring Boot REST API with an in-memory H2 database seeded with sample policies and claims.
> - **Frontend** (`http://localhost:3000`) — React web app that calls the backend API.

### Manual Start (if the script does not work)

```bash
# Terminal 1 — Backend
cd src/backend
mvn spring-boot:run

# Terminal 2 — Frontend
cd src/frontend
npm install   # first time only
npm start
```

### Verify Both Services Are Running

```bash
curl http://localhost:8080/api/policies   # should return a JSON array of policies
curl http://localhost:3000                # should return an HTML page
```

[↑ Back to top](#table-of-contents)

---

## 4. MCP Server Setup

> **What is MCP?** The Model Context Protocol (MCP) gives Copilot agents direct, structured access to external services like GitHub and Playwright. Without MCP, Copilot can only read and write files; with MCP, it can create issues, open PRs, navigate a browser, and more.

### GitHub MCP Server

**Option A — Using VS Code UI (recommended for beginners):**

1. Open VS Code Command Palette (`Cmd+Shift+P` / `Ctrl+Shift+P`).
2. Type **"MCP: Add Server"** and select it.
3. Choose **GitHub** from the list.
4. When prompted, enter your GitHub PAT (from the prerequisites step).
5. A green status indicator in the status bar confirms the server is running.

**Option B — Add manually to `.vscode/mcp.json`:**

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

> **Tip:** Using `${input:github-pat}` means VS Code will prompt you for your PAT each time it starts the server — your token is never stored in the file.

### Playwright MCP Server (for browser testing)

The Playwright MCP server is already pre-configured in `.vscode/mcp.json`. To verify it is working:

1. Open Copilot Chat.
2. Click the **Tools** (plug icon) in the chat input bar.
3. Confirm `browser_*` tools appear in the list.

If you need to install the Chromium browser for Playwright:

```bash
npx playwright install chromium
```

### Azure DevOps MCP Server *(optional)*

Only needed for the ADO integration segment. See [Azure DevOps MCP setup](https://github.com/mcp/microsoft/azure-devops-mcp) for instructions, and [2.Azure-DevOps-Setup.md](setup/2.Azure-DevOps-Setup.md) for connecting ADO to GitHub.

[↑ Back to top](#table-of-contents)

---

## 5. Custom Agents

> **What are custom agents?** Custom agents are `.agent.md` files in `.github/agents/`. Each agent has a focused role (like a specialist on your team) and tailored instructions. You invoke them by name in Copilot Chat.

| Agent | Role |
|---|---|
| `1.SDLC BRD Agent` | Business requirements — BRD, epics, features |
| `2.SDLC HLD Agent` | High-level design — architecture, component boundaries, data flow |
| `5.SDLC Dev Agent` | Feature implementation — React + Spring Boot |
| `6.SDLC Test Agent` | Test generation, CI triage, coverage |

**How to invoke an agent:**

1. Open Copilot Chat in **Agent mode**.
2. Type `@` followed by the agent name (e.g., `@2.SDLC HLD Agent`).
3. Write your prompt after the agent name.

**How to create a new agent:**

```
/create-agent
```

Or add a `.agent.md` file directly under `.github/agents/`.

[↑ Back to top](#table-of-contents)

---

## 6. Agent Delegation Modes

Choose the right mode depending on how you want to work:

| Mode | When to use | How it works |
|---|---|---|
| **Local Agent** (Agent Mode in VS Code) | Interactive coding, debugging, refactoring | Sees your workspace, terminal, and uncommitted changes; you steer every step |
| **Cloud Agent** (from VS Code) | Async task handoff while you keep coding | Copilot works in the background; results appear as a PR |
| **GitHub Issue → Copilot** | Tracked backlog work | Assign an issue to Copilot on GitHub.com; the issue description drives implementation |

**Rule of thumb:** Local = interactive, now. Cloud = offload, async. GitHub Issue = track, team.

> See [Copilot Agent Delegation Guide](guides/4.Copilot-Agent-Delegation-Guide.md) for detailed examples and a decision guide.

[↑ Back to top](#table-of-contents)

---

## 7. Demo Flow — Step by Step

Follow these steps in order during the workshop.

### Step 1 — Generate a High-Level Design

In Copilot Chat (Agent mode):

```
@2.SDLC HLD Agent Generate a High-Level Design for InsureWell.
Include: component diagram, data flow between React frontend and Spring Boot REST API,
JPA persistence layer, and the claims submission/update API surface.
```

**What to expect:** An architecture overview, component interaction diagram, and API surface map.

---

### Step 2 — Generate Requirements

```
@1.SDLC BRD Agent Analyze the InsureWell application (src/backend, src/frontend, README.md).
Identify gaps and produce a prioritized backlog of business requirements, epics, and features.
```

**What to expect:** A list of existing capabilities and a prioritized backlog of new features.

---

### Step 3 — Create GitHub Issues

```
@github Create GitHub issues for the following user stories in
im-naga-ghas/insure-well-agentic-sdlc-ghe with labels (enhancement, frontend, backend):
[paste story list from Step 2 output]
```

**What to expect:** New issues created in the GitHub repository, visible to the whole team.

---

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

**What to expect:** A feature branch and a Pull Request with the implementation.

---

### Step 5 — Add Tests

```
@6.SDLC Test Agent Review the PR for the authentication feature.
Generate tests covering login success/failure, protected routes, and logout behavior.
```

**What to expect:** Test files added to the PR covering the new feature.

---

### Step 6 — Review & Merge

Use Copilot's built-in code review on the PR (click **"Copilot review"** in the PR page), or invoke the review agent. Merge after human approval.

---

### Full Cycle Summary

```
Clone & Run → MCP Setup → Custom Agents
  → HLD Agent → BRD Agent → Create Issues
  → Dev/Cloud Agent → QA Agent → Review → Merge
  → Repeat for next story
```

> For a detailed walkthrough with exact prompts and expected outputs for each step, see [Demo Flow Guide](guides/5.Demo-Flow.md).

[↑ Back to top](#table-of-contents)

---

## 8. Agentic SDLC Workflow (Visual)

The following sequence shows how each automated step connects:

1. **Request Flow** — A GitHub issue triggers agent processing and code generation  
   ![Request Flow](../images/0_0.request%20flow.png)

2. **Service Connection** — Secure link between GitHub and Azure DevOps  
   ![Service Connection](../images/0_1.service_connection.png)

3. **Copilot Code Review** — Automated review begins on the generated PR  
   ![Copilot Code Review](../images/0_2_copilot%20code%20review_.png)

4. **Status Checks** — CI/CD pipeline validations run  
   ![Status Checks](../images/0_status%20checks.png)

5. **Draft PR → Ready for Review → Fix & Commit → Merge**  
   ![Draft PR](../images/3_draft%20pull%20request.png)

**Key outcomes:**
- Automated issue-to-PR workflow
- Continuous code review feedback
- Self-fixing via Copilot delegation
- Reduced manual review cycles

> See [Agentic SDLC Workflow Guide](guides/3.Understand_Workflow.md) for the full visual step-by-step breakdown.

[↑ Back to top](#table-of-contents)

---

## 9. Playwright MCP Workshop

> **What is this?** After the app is running, you can use Playwright MCP browser tools to validate UI behavior interactively from Copilot Chat — no need to write test code first.

**Available tool categories:**

| Category | Tools |
|---|---|
| Navigation | `browser_navigate`, `browser_navigate_back`, `browser_resize`, `browser_close` |
| Interaction | `browser_click`, `browser_fill_form`, `browser_type`, `browser_select_option` |
| Inspection | `browser_snapshot`, `browser_take_screenshot`, `browser_evaluate` |

**Always use `data-testid` selectors** — for example: `[data-testid='add-policy-btn']`

**Workshop flow:**

1. Start the app (backend port 8080 + frontend port 3000)
2. Smoke-test navigation with `browser_navigate` + `browser_snapshot`
3. Dashboard: click policy tabs, open Add Policy modal, fill and save
4. Claims: navigate to Claims, submit a new claim, filter, update status

> See [Playwright MCP Setup Guide](guides/6.playwright-mcp-setup-working.md) for the full selector reference, all tool names, and guided exercises.

[↑ Back to top](#table-of-contents)

---

## 10. Reference Links

| Resource | Link |
|---|---|
| GitHub MCP Server | https://github.com/modelcontextprotocol/servers/tree/main/src/github |
| VS Code Copilot Agent Mode | https://code.visualstudio.com/docs/copilot/chat/chat-agent-mode |
| Copilot Custom Instructions | https://code.visualstudio.com/docs/copilot/copilot-customization |
| Microsoft Playwright MCP | https://github.com/microsoft/playwright-mcp |
| InsureWell REST API | ../src/README.md |
| InsureWell Data Model | ../docs/InsureWell_DataModel.md |
| InsureWell HLD | ../docs/InsureWell_HLD.md |
| Prerequisites (detailed) | setup/1.Prerequisites.md |
| Azure DevOps Setup | setup/2.Azure-DevOps-Setup.md |
| Workflow Visual Guide | guides/3.Understand_Workflow.md |
| Agent Delegation Guide | guides/4.Copilot-Agent-Delegation-Guide.md |
| Demo Flow (detailed) | guides/5.Demo-Flow.md |
| Playwright MCP Guide | guides/6.playwright-mcp-setup-working.md |

[↑ Back to top](#table-of-contents)
