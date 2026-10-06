---
name: safe-java-rewrite
description: Safely perform scripted or bulk Java source rewrites, especially PowerShell import sorting and encoding repairs. Use when a transformation affects multiple Java files or changes decoding; ordinary single-file edits do not require this workflow.
---

# Safe Java Rewrite

Keep the approved file scope and preserve text, paths, and recoverability while applying a mechanical transformation.

## Before mutation

- Inventory explicit input paths and existing changes. Separate the intended transformation from unrelated edits.
- Prefer formatter or compiler-aware tooling for imports. Do not reconstruct imports with a heuristic that can silently remove required symbols.
- For a custom script, preview on representative files with Vietnamese/non-ASCII text, existing changes, and different package depths. Inspect the proposed diff before the bulk run.
- Read with strict, explicit encoding. For UTF-8, reject invalid bytes; do not silently replace characters or guess a code page. Preserve BOM and newline style where practical.
- Store byte-for-byte backups outside source/build discovery. Use the resolved source-root-relative path, preserving the basename and directory tree. Verify each resolved backup destination stays inside the backup root.
- Record a manifest mapping absolute source path to relative backup path and pre-edit SHA-256. Reject duplicate destinations, existing backup collisions, and paths outside the intended source root. Do not derive names using a fixed substring offset.

## Apply and verify

- Fail immediately on read, transformation, or write errors. In PowerShell use terminating errors and check native process exit codes; an unsupported option must stop the batch.
- Compute transformed content before replacing a file. For scripted writes, validate a temporary sibling and replace only after successful decoding and transformation. Never truncate the source before transformation succeeds.
- Use explicit UTF-8 writes; do not rely on shell default encoding or piping text through a different shell.
- Verify input/output file counts and manifest mappings, inspect the scoped diff, and scan for replacement characters and unexpected mojibake. Strict UTF-8 decoding alone cannot detect valid bytes representing corrupted text.
- Run compilation appropriate to the changed source set and repository quality checks required for that transformation. Import changes need compile verification; diff checks alone are insufficient.

## Failure and recovery

Stop the batch; inventory every touched file. Preserve the failed artifacts and distinguish pristine pre-edit backups from snapshots already containing damaged text. Restore only verified explicit paths, retaining unrelated work. Encoding repair must be based on known byte/encoding provenance and reviewed text, not repeated speculative conversions.

Report what failed, affected paths, backup provenance, recovery performed, and checks passed or pending. Do not call a backup safe merely because it decodes as UTF-8. Do not delete or blindly restore backups as part of this skill.

