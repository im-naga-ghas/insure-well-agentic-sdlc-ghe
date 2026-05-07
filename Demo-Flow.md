# InsureWell — Agentic SDLC Demo Flow

This walkthrough demonstrates a full Agentic Software Development Life Cycle (SDLC) using **GitHub Copilot agents** inside VS Code, starting from a fresh clone of the InsureWell repository.

---

## Step 1 — Clone, Build & Run the Application

```bash
# 1. Clone the repository
git clone https://github.com/im-naga-ghas/insure-well-agentic-sdlc-ghe.git
cd insure-well-agentic-sdlc-ghe

# 2. Create and activate a Python virtual environment
python3 -m venv .venv
source .venv/bin/activate        # macOS / Linux
# .venv\Scripts\activate         # Windows

# 3. Install dependencies
pip install -r requirements.txt

# 4. Start the development server
python app.py
```

Open **http://localhost:5001** in your browser.

On first run, the SQLite database is auto-created at `data/insurewell.db` and seeded with 3 sample policies and 7 claims.

**What you see:**
- **Dashboard** — policy cards with coverage amount, status, and recent claims
- **Claims** — claim submission form (amount, description, optional file upload) with filter-by-policy and status tracking (Pending / Approved / Rejected)

---

## Step 2 — Enable the GitHub MCP Server

The **GitHub MCP (Model Context Protocol) server** gives Copilot agents direct access to GitHub APIs — issues, pull requests, branches, and code search.

1. Open VS Code Command Palette (`Cmd+Shift+P` / `Ctrl+Shift+P`).
2. Run **"MCP: Add Server"** and select **GitHub**.
3. Authenticate when prompted (uses your GitHub token or OAuth flow).
4. Confirm the server is running: the MCP status indicator in the status bar turns green.

> Alternatively, add the server entry manually to `.vscode/mcp.json`:
> ```json
> {
>   "servers": {
>     "github": {
>       "type": "stdio",
>       "command": "npx",
>       "args": ["-y", "@modelcontextprotocol/server-github"],
>       "env": {
>         "GITHUB_PERSONAL_ACCESS_TOKEN": "<YOUR_PAT>"
>       }
>     }
>   }
> }
> ```
> Required token scopes: `repo`, `read:org`, `read:user`.

---

## Step 3 — Add Custom Agents for Agentic SDLC

Custom agents are defined as `.agent.md` files inside `.github/agents/` (or `.vscode/agents/`). Each agent encapsulates a focused role with tailored instructions and tool access.

**Recommended agent set for InsureWell:**

| Agent file | Role |
|---|---|
| `hld-agent.md` | High-Level Design — architecture diagrams, component boundaries |
| `brd-agent.md` | Business Requirements — BRD analysis, story creation |
| `coding-agent.md` | Feature implementation — code, unit tests, PR |
| `cloud-agent.md` | Cloud/infra — IaC, deployment configs, environment setup |
| `qa-agent.md` | Quality Assurance — test generation, CI triage, coverage |
| `review-agent.md` | Code Review — PR review, standards, security checks |

To create these agents, open the Copilot Chat panel, switch to **Agent mode**, and use:

```
@copilot /new-agent
```

Or create `.agent.md` files directly under `.github/agents/` with the appropriate `applyTo` patterns and system instructions.

---

## Step 4 — Create the HLD with the HLD Agent

Open Copilot Chat in **Agent** mode and invoke the HLD agent:

```
@hld-agent Generate a High-Level Design for the InsureWell application.
Include: component diagram, data flow between Flask routes and SQLite,
the claims upload pipeline, and REST API surface.
```

**Expected output:**
- Architecture overview (Flask app, SQLite DB, static assets, uploads folder)
- Component interaction diagram (Mermaid or ASCII)
- Data flow: browser → Flask route → SQLite → JSON response
- API surface map aligned with `app.py`

Review and refine the HLD, then save it as `docs/HLD.md` in the repository.

---

## Step 5 — Create Requirements with the BRD Agent

Use the BRD agent to analyze the current application and generate new user stories:

```
@brd-agent Analyze the InsureWell application (app.py, templates, README).
Identify gaps in the current feature set and produce a prioritized backlog
of user stories for extending the application.
```

**Expected output:**
- Summary of existing capabilities (Phase 1 MVP)
- Identified gaps (e.g., authentication, notifications, reporting, analytics)
- Prioritized user story backlog in standard format:
  > *As a [persona], I want [feature] so that [benefit].*

---

## Step 6 — Create New User Stories to Extend the Application

From the BRD agent output, create GitHub Issues for the top stories. Example stories to seed the backlog:

| # | Story | Priority |
|---|---|---|
| 1 | As a policyholder, I want to log in with a username and password so that my policies are private | High |
| 2 | As a policyholder, I want to receive an email notification when a claim status changes so that I stay informed | High |
| 3 | As an admin, I want a reporting dashboard with claim trends and approval rates so that I can monitor operations | Medium |
| 4 | As a policyholder, I want to download a PDF summary of my policy so that I have an offline record | Medium |
| 5 | As an admin, I want to search and filter policies by status or plan type so that I can manage them efficiently | Medium |
| 6 | As a policyholder, I want to see a timeline of claim status changes so that I understand the review process | Low |

Create issues via the GitHub MCP server from Copilot Chat:

```
@github Create GitHub issues for the following user stories in
im-naga-ghas/insure-well-agentic-sdlc-ghe with appropriate labels
(enhancement, frontend, backend, auth):
[paste story list]
```

---

## Step 7 — Assign a Story to the Cloud Agent or Coding Agent

Pick a story from the backlog (e.g., *Story 1 — User Authentication*).

**Option A — Coding Agent (feature implementation):**

```
@coding-agent Implement user authentication for InsureWell (Story #1).
- Add a /login and /logout route to app.py using Flask session
- Add a SQLite users table with hashed passwords (werkzeug.security)
- Create a login.html template consistent with base.html
- Protect /dashboard and /claims with a login_required decorator
- Add a seed admin user (username: admin, password: changeme)
- Write unit tests for the auth routes
```

**Option B — Cloud Agent (infrastructure / deployment):**

```
@cloud-agent Create a deployment configuration for InsureWell.
- Dockerfile for the Flask application
- docker-compose.yml with the app and a volume for SQLite data
- GitHub Actions workflow (.github/workflows/deploy.yml) to build and
  push the Docker image to GitHub Container Registry on push to main
- Environment variable management for SECRET_KEY and DB_PATH
```

The agent will create a feature branch, implement the changes, and open a Pull Request.

---

## Step 8 — Use the QA Agent to Add Tests, Inspect CI Failures & Check Coverage

After the Coding or Cloud agent opens a PR, invoke the QA agent:

**Add tests:**
```
@qa-agent Review the PR for Story #1 (user authentication).
Generate pytest test cases covering:
- Successful login and session creation
- Failed login with wrong password
- Access to protected routes without authentication (expect redirect)
- Logout clears the session
Add the tests to tests/test_auth.py.
```

**Inspect CI failures:**
```
@qa-agent The CI pipeline for PR #<number> is failing.
Analyze the GitHub Actions logs, identify the root cause,
and suggest or apply a fix.
```

**Check code coverage:**
```
@qa-agent Run the test suite with coverage reporting and identify
any modules or branches below 80% coverage. Suggest additional
test cases to close the gaps.
```

---

## Step 9 — Use the Review Agent to Review the PR

Once tests pass, invoke the review agent on the open PR:

```
@review-agent Review PR #<number> for the user authentication feature.
Check for:
- Security issues (password hashing, session fixation, CSRF)
- Code style consistency with the existing Flask codebase
- Missing error handling or edge cases
- Documentation and inline comments
- Any OWASP Top 10 concerns
Provide inline comments on the PR and a summary verdict (Approve / Request Changes).
```

The review agent will post structured review comments directly to the Pull Request via the GitHub MCP server.

---

## Full Demo Flow Summary

```
Clone & Run  →  Enable GitHub MCP  →  Add Custom Agents
     ↓
HLD Agent (architecture)  →  BRD Agent (requirements & backlog)
     ↓
Create GitHub Issues (stories)  →  Assign to Coding / Cloud Agent
     ↓
QA Agent (tests, CI triage, coverage)  →  Review Agent (PR review)
     ↓
Merge PR  →  Repeat for next story
```

---

## Reference Links

- [GitHub MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/github)
- [VS Code Copilot Agent Mode](https://code.visualstudio.com/docs/copilot/chat/chat-agent-mode)
- [Copilot Custom Instructions](https://code.visualstudio.com/docs/copilot/copilot-customization)
- [InsureWell REST API](./README.md#api-reference)
