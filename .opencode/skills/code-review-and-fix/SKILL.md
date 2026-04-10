---
name: code-review-and-fix
description: "Review a set of files or plan steps and fix all issues in one go. Do not use for trivial single-file fixes or when only a review without fixes is needed."
---

# Skill: code-review-and-fix

## Project Location

`/mnt/c/Users/PavelNikiforov/IdeaProjects/AiHelperBot`

## Parameters
- **target** — files, plan steps, or a description of what to review
- **report_file** — output filename for the written review report (e.g. `reviews/review_step2.md`)

## Decision Tree

1. **Single file with obvious fix?** → Don't use this skill. Fix directly.
2. **Review only, no fixes?** → Use `aihelperbot-review` skill instead.
3. **Multiple files or complex issue?** → Proceed with this skill.

## Steps

### 1. Run code review
Invoke a thorough code review on the target files/plan steps using the `aihelperbot-review` skill checklist.
Load `aihelperbot-review/references/checklist.md` for the full review criteria.
Produce the full structured review (Critical / Major / Minor table format).

### 2. Write report to file
Write the review output to `<project_root>/reviews/<report_file>`.
Use the standard review format with severity-based table from `aihelperbot-review`.

### 3. Fix all issues
Read the report file and apply every fix across all affected files.
Fix Critical issues first, then Major, then Minor.

### 4. Verify
Run `./mvnw test` after fixes to verify nothing broke.

## Notes
- Steps 1 and 2 must complete before step 3 starts (report must exist on disk)
- The report file serves as the contract between review and fix — don't skip writing it
- After fixing, the report file remains as an audit trail; do not delete it
