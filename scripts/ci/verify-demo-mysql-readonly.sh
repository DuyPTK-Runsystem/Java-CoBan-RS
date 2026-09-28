#!/usr/bin/env bash
set -euo pipefail

for name in MYSQL_HOST MYSQL_DATABASE MYSQL_USER MYSQL_PWD; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required MySQL verifier setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done
[[ "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || { echo 'MYSQL_DATABASE must use simple alphanumeric/underscore characters for the read-only grant check.' >&2; exit 2; }

grants="$(mysql --connect-timeout=10 --ssl-mode=VERIFY_IDENTITY \
  --host="$MYSQL_HOST" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" \
  --batch --raw --skip-column-names --execute='SHOW GRANTS FOR CURRENT_USER()')"
[[ -n "$grants" ]] || { echo 'MySQL verifier account returned no grants.' >&2; exit 1; }

MYSQL_GRANTS="$grants" python3 - "$MYSQL_DATABASE" <<'PYGRANTS'
import re
import sys

database = sys.argv[1]
lines = [line.strip() for line in __import__("os").environ.get("MYSQL_GRANTS", "").splitlines() if line.strip()]
allowed = re.compile(r"^GRANT USAGE ON \*\.\* TO `[^`]+`@`[^`]+`$")
select_only = re.compile(r"^GRANT SELECT ON `" + re.escape(database) + r"`\.\* TO `[^`]+`@`[^`]+`$")
if not lines or any(not (allowed.fullmatch(line) or select_only.fullmatch(line)) for line in lines):
    print('MySQL verifier identity must have only USAGE and SELECT on its configured database; grants were rejected.', file=sys.stderr)
    raise SystemExit(1)
if not any(select_only.fullmatch(line) for line in lines):
    print('MySQL verifier identity lacks SELECT on its configured database.', file=sys.stderr)
    raise SystemExit(1)
PYGRANTS

echo 'mysql_identity=SELECT_ONLY'
