---
name: start-session
description: "Load accumulated project context and memory at the beginning of a working session. Do not use mid-session or for task-specific work."
---

# Skill: start-session

## Project Location

`/mnt/c/Users/PavelNikiforov/IdeaProjects/AiHelperBot`

## Purpose
Load accumulated project context so the agent starts with awareness of the project state.

## Steps

### 1. Load Memory
Read all files from `~/.config/opencode/skills/memory/`:
- `patterns.md` — patterns and workarounds
- `feedback.md` — user corrections
- `decisions.md` — key architectural decisions

If memory files don't exist — create empty structure.

### 2. Load Project Context
Read:
- `CLAUDE.md` in project root (AI assistant instructions, Windows-specific conventions)
- `specs/01-product-overview.md` (current state, known limitations)
- `specs/code-conventions.md` (coding standards)
- Last 5 commits: `git log --oneline -5`
- Current status: `git status`, `git branch`
- Check for pending plan files: `ls plans/`

### 3. Validate Memory Relevance
Quick pass through loaded entries:
- If an entry references a file/function that no longer exists — mark as deletion candidate
- If an entry contradicts current code — notify the user

### 4. Summary Report
Output to user:
```
Session initialized
- Patterns loaded: N
- Corrections loaded: N
- Decisions loaded: N
- Stale entries: N
- Current branch: <branch>
- Last commit: <message>
- Project: AiHelperBot (Spring Boot 3.2.5 / Java 17)
```

## Important
- Do NOT modify memory files on start — read only
- Keep the report short, don't recite all entries
