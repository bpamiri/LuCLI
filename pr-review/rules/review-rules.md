Apply these review priorities in order:

1. Correctness and regressions
- Broken behavior, logic mistakes, race conditions, bad edge-case handling.

2. Security and secrets
- No hardcoded secrets.
- No accidental logging of sensitive values.
- Validate unsafe input paths and shell execution surfaces.

3. Reliability and operability
- Error handling is explicit and actionable.
- Changes are observable and debuggable.

4. Project consistency
- Follow existing command and naming patterns.
- Prefer minimal, focused changes over broad rewrites.

5. Testability
- Verify tests are updated when behavior changes.
- Call out missing tests when risk is medium+.
