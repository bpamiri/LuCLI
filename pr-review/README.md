# PR Review Workflow (LuCLI)

This folder contains a LuCLI-native multi-agent pending-code review flow.

## Files

- `review-pending-code.lucli` — main workflow script
- `prompts/reviewer-system.md` — per-reviewer prompt
- `prompts/synthesis-system.md` — synthesis prompt
- `rules/review-rules.md` — baseline review rules
- `out/pr_context.example.json` — example input payload shape

## Setup endpoints

Configure endpoint names you want to use (defaults expected by the script: `Claude`, `Gemini`, `Codex`, `OpenAI`):

```bash
lucli ai config add --name Claude --type openai --model claude-sonnet-4 --secret-key '#env:CLAUDE_API_KEY#'
lucli ai config add --name Gemini --type openai --model gemini-2.5-pro --secret-key '#env:GEMINI_API_KEY#'
lucli ai config add --name Codex --type openai --model codex-5.3 --secret-key '#env:OPENAI_API_KEY#'
lucli ai config add --name OpenAI --type openai --model gpt-4o --secret-key '#env:OPENAI_API_KEY#'
```

Adjust model names to whichever are valid for your provider account.

## Run

1. Create input context at `pr-review/out/pr_context.json` (use `out/pr_context.example.json` as a starting point).
2. Run:

```bash
lucli pr-review/review-pending-code.lucli
```

Outputs:

- Individual reviewer outputs in `pr-review/out/reviews/`
- Final synthesized report at `pr-review/out/review_summary.md`
