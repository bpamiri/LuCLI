# LEARNINGS.md

Codebase-specific patterns, gotchas, and useful knowledge accumulated over time.
Append new entries at the bottom under the appropriate date/session.

---

## 2026-03-18

- `CHANGELOG.md` uses an `## Unreleased` section at the top (no version/date) for in-progress work. Versioned releases follow below it (e.g. `## 0.2.23`). Each entry is a bullet with a **Bold Title:** followed by a short description.
- The test suite entry point is `./tests/test.sh`. A lightweight alternative is `./tests/test-simple.sh`. Always run the full suite before and after making changes.
- `./dev-lucli.sh` is the fast development loop — it runs LuCLI via `mvn exec:java` without rebuilding the JAR, so changes to source are picked up quickly for manual testing.
- The `#env:VAR#` syntax (not `${VAR}`) is the preferred variable substitution in `lucee.json`. `${VAR}` is reserved for Lucee/JVM runtime resolution and is intentionally left untouched in protected zones (`configuration`, `jvm.additionalArgs`).
