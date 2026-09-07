---
name: commit-writer
description: Writes conventional commit messages from staged changes. 
  Use when the user asks to commit, write a commit message, or says 
  "commit my changes."
---

# Commit Message Writer

When asked to write a commit message:

1. Run `git diff --staged` to read the staged changes
2. Identify the primary type of change:
   - feat: new feature
   - fix: bug fix
   - refactor: code restructuring without behavior change
   - docs: documentation only
   - chore: build, tooling, or dependency changes
   - test: adding or fixing tests
3. Identify the scope from the changed files (e.g., auth, api, ui)
4. Write a subject line under 72 characters: type(scope): description
5. If the change is complex, add a body explaining why (not what)
6. Flag any breaking changes with BREAKING CHANGE: in the footer

## Rules
- Subject line is imperative mood ("add feature" not "added feature")
- No period at the end of the subject line
- Body wraps at 72 characters
- If multiple logical changes are staged, suggest splitting into 
  separate commits