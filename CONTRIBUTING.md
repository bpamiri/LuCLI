# Contributing to LuCLI
Thanks for helping improve LuCLI.
This guide covers the expected workflow for proposing changes.

## Before You Start
- Open an issue or discussion for larger changes so implementation direction is clear.
- Keep changes focused and incremental when possible.
- Follow the LuCLI command pattern:
  - `lucli <action> <subcommand> [options] [parameters]`

## Local Setup
```bash
# Clone your fork/repository
git clone https://github.com/cybersonic/LuCLI.git
cd LuCLI

# Build project JAR
mvn clean package

# Fast development loop (runs via mvn exec:java)
./dev-lucli.sh
```

## Validate Your Changes
Run the standard test suites before opening a PR:
```bash
./tests/test.sh
./tests/test-bats.sh
```

Useful additional checks:
```bash
# Quick smoke cycle
./dev-lucli.sh --version

# Build self-executing binary
mvn clean package -Pbinary
```

## Documentation and Changelog
- Update docs when behavior or command usage changes.
- Add a short bullet to `CHANGELOG.md` under `## Unreleased` for user-facing changes.
- Keep examples aligned with current Java/LuCLI requirements.

## Pull Request Guidelines
- Use a clear title and description of what changed and why.
- Include example commands/output when relevant.
- Mention any known limitations or follow-up work.
- Keep PR scope narrow to simplify review.

## Release and Process References
- Release details: [RELEASE_PROCESS.md](RELEASE_PROCESS.md)
- Project conventions and architecture notes: [WARP.md](WARP.md)
