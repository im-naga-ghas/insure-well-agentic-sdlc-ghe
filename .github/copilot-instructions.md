# Output Rules
- Code only unless explanation is explicitly requested.
- No markdown preamble.
- No "Here is the code" intro.
- No closing summary.

# Context Discipline
- Max 3 editor tabs open at once.
- Open only files related to the current task.
- Close unused tabs before each session.

# Prompt Style
- Prefer terse, code-centric prompts.
- Keep prompts under 8 words when possible.

# Code Review Guidelines
- GlobalCounter issues: dead code, code smells, anti-patterns, design issues.
- Review for high-impact correctness, security, reliability, performance, and maintainability issues.
- Prioritize confirmed bugs, regressions, data loss, crashes, null/edge cases, and incorrect error handling.
- Check input validation, boundary conditions, exception handling, authentication, authorization, injection risks, secrets exposure, and unsafe data handling.
- Check for resource leaks, inefficient queries or loops, race conditions, and meaningful performance regressions.
- Verify tests cover changed logic, failure paths, security-sensitive behavior, and important edge cases.
- Check backward compatibility and impact on existing callers, APIs, data, and workflows.
- Flag duplicated or unnecessarily complex code only when it creates a meaningful maintenance or defect risk.
- Report only issues supported by evidence from the code.
- Prioritize findings by severity and include file and line references.
- Do not report formatting, naming, stylistic, or other low-impact issues.
- Suggest improvements only when they address a demonstrated risk or provide clear engineering value.
- If no significant issues are found, state that clearly and mention any remaining test gaps or residual risks.

# Tech Stack
- Use Java 17 with Spring Boot 3.1 for backend, React 18 for frontend.
