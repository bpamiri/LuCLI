---
title: LuCLI Command Scripts (.lucli/.luc)
layout: docs
---
This page covers LuCLI command scripts (`.lucli` and `.luc`) for automating multi-step CLI workflows.

## What to call `.lucli` files

Use **LuCLI command scripts** as the primary term in docs and examples.

- Clear: they are scripts made of LuCLI commands
- Distinct from CFML script files (`.cfs`, `.cfm`, `.cfml`)
- Flexible enough for local workflows, CI tasks, and deploy scripts

You may still see “batch scripts” in older docs, but “LuCLI command scripts” is the recommended name.

## Basic syntax

```bash
lucli my-workflow.lucli
lucli my-workflow.luc
```

Each non-empty, non-comment line is executed as if typed in LuCLI.

## Example command script

```bash
#!/usr/bin/env lucli
# Build + validate workflow

echo "Starting checks..."
modules list
run scripts/validate.cfs target=staging
server status
echo "Done"
```

Comments start with `#`. Shebang lines are supported.

## Make scripts executable

```bash
chmod +x my-workflow.lucli
./my-workflow.lucli
```

## Running scripts with environments

LuCLI resolves the active environment in this order:

1. `--env` / `-e`
2. `LUCLI_ENV` environment variable
3. unset (`null`)

Examples:

```bash
lucli --env dev deploy.lucli
LUCLI_ENV=prod lucli deploy.lucli
```

## Environment blocks inside command scripts

You can conditionally run blocks based on the active environment:

```bash
#@env:dev
echo "Running dev-only setup"
server start --env dev
#@end

#@env:prod,staging
echo "Running shared prod/staging checks"
#@end

#@env:!prod
echo "Runs everywhere except prod"
#@end
```

Shorthand forms are also supported, such as `#@dev`, `#@prod`, `#@staging`, followed by `#@end`.

## Environment variables for scripts

You can populate script environment values using:

- `--envfile <path>` at invocation time
- `source <path>` inside the script
- `set KEY=value` inside the script

Examples:

```bash
lucli --envfile .env deploy.lucli
```

Inside script:

```bash
source .env.production
set RELEASE_TAG=2026.03.19
run scripts/release.cfs tag=${RELEASE_TAG}
```

## Command behavior and precedence

When you run `lucli something`, LuCLI resolves in this order:

1. Built-in subcommands (`server`, `modules`, `terminal`, `cfml`, `help`, etc.)
2. Existing `.lucli` / `.luc` files
3. Existing CFML files (`.cfs`, `.cfm`, `.cfc`, `.cfml`)
4. Module shortcuts

## Related pages

- [Running Scripts and Components](../running-scripts-and-components/)
- [Shortcuts & Direct CFML Execution](../shortcuts-and-direct-execution/)
- [Environments and Configuration Overrides](../../050_server-configuration/030_environments-and-overrides/)
