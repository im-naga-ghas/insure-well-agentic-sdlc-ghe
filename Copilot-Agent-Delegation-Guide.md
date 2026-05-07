# Copilot Agent Delegation Guide

## Overview

Pick the right delegation mode based on **where the work starts** and **how interactive you want it to be**.

---

## Quick Comparison

| Agent | Location | Best For | Style |
|---|---|---|---|
| **VS Code Local Agent** (Agent Mode) | In your editor | Interactive coding, debugging, refactoring | Live edits in workspace |
| **VS Code Cloud Agent** | In your editor | Async work delegation | Background PR creation |
| **GitHub Issue → Copilot** | GitHub.com | Tracked backlog work | Issue-to-PR automation |

---

## Three Modes Explained

### 1️⃣ VS Code Local Agent (Agent Mode) 🛠️

**When:** You want tight, interactive back-and-forth while coding.

- Sees your workspace, files, terminal state, uncommitted changes
- Best for debugging, refactoring, explaining code
- You steer every step

**Examples:** "Why is this Flask route failing?" | "Refactor this function" | "Trace this claim update"

---

### 2️⃣ VS Code Cloud Agent ☁️

**When:** You spot a task while coding and want to hand it off async.

- Self-contained work discovered during your coding session
- You don't implement it yourself; Copilot does in background
- Results in a proposed change or PR

**Examples:** "Add tests for this module" | "Clean up this component" | "Implement this helper"

---

### 3️⃣ GitHub Issue → Copilot Coding Agent 📌

**When:** The work is already a tracked GitHub issue.

- Work is part of your team's backlog
- Visible to the whole team from start
- Issue description is enough context to implement

**Examples:** "Issue #42 is ready to implement" | "Assign this bug to Copilot" | "Implement this feature"

---

## Quick Decision Guide

| You need... | Use... |
|---|---|
| Live, interactive help right now | **Local Agent** |
| To delegate a task I found while coding | **Cloud Agent** |
| To track and execute backlog work | **GitHub Issue → Copilot** |
| Local context (local DB, dev server, unpublished files) | **Local Agent** |
| Team visibility and workflow integration | **GitHub Issue → Copilot** |

---

## Capability Snapshot

| Feature | Local Agent | Cloud Agent | GitHub Issue |
|---|---|---|---|
| Interactive | ✅ Excellent | ❌ Limited | ❌ Limited |
| Local debugging | ✅ Excellent | ❌ Weak | ❌ Weak |
| Async delegation | ⚠️ Moderate | ✅ Excellent | ✅ Excellent |
| Team visibility | ❌ No | ⚠️ Sometimes | ✅ Excellent |
| Backlog execution | ⚠️ Moderate | ✅ Good | ✅ Excellent |

---

## Key Terminology

- **Local Agent** = `VS Code Local Agent (Agent Mode)` = Copilot in Agent Mode inside VS Code
- **Cloud Agent** = Async delegation from VS Code (work happens in background)
- **GitHub Issue Delegation** = Assign issue to Copilot on GitHub.com

---

## Final Rule of Thumb

🎯 **Local = interactive, now.** | ☁️ **Cloud = offload, async.** | 📌 **GitHub = track, team.**

If work is local and interactive → **Local Agent**  
If work is discovered while coding → **Cloud Agent**  
If work is already tracked as an issue → **GitHub Issue → Copilot**