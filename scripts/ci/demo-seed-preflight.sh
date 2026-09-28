#!/usr/bin/env bash
set -euo pipefail

for name in MYSQL_HOST MYSQL_DATABASE MYSQL_USER MYSQL_PWD SEED_TARGET_ID EXPECTED_IMAGE OPERATION; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Required preflight setting is missing: %s\n' "$name" >&2
    exit 2
  fi
done
[[ "$SEED_TARGET_ID" =~ ^[A-Za-z0-9._:-]+$ ]]
[[ "$EXPECTED_IMAGE" =~ ^[A-Za-z0-9._:/-]+:[0-9a-f]{40}$ ]]

mysql_query() {
  mysql --connect-timeout=10 --ssl-mode=VERIFY_IDENTITY \
    --host="$MYSQL_HOST" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" \
    --batch --raw --skip-column-names --execute="$1"
}

table_count="$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'app_demo_seed_completion'")"
marker_rows=''
if [[ "$table_count" == '1' ]]; then
  marker_rows="$(mysql_query "SELECT seed_key, fixture_version, target_id, deployment_ref FROM app_demo_seed_completion WHERE seed_key = 'DEMO_FIXTURE_PLAN_081'")"
fi

if [[ "$OPERATION" == 'deploy' || "$OPERATION" == 'rollback' ]]; then
  [[ -n "$marker_rows" ]] || { echo 'Deploy blocked: Plan 081 completion marker is absent; use bootstrap only after clean-target preflight.' >&2; exit 1; }
  IFS=$'\t' read -r marker_key fixture_version target_id deployment_ref <<< "$marker_rows"
  [[ "$marker_key" == 'DEMO_FIXTURE_PLAN_081' && "$fixture_version" == 'PLAN_081_V1' && "$target_id" == "$SEED_TARGET_ID" && -n "$deployment_ref" ]] || {
    echo 'Deploy blocked: completion marker does not match the configured fixture and DB target.' >&2
    exit 1
  }
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then echo 'seed_state=complete' >> "$GITHUB_OUTPUT"; else echo 'seed_state=complete'; fi
  exit 0
fi

if [[ "$OPERATION" != 'bootstrap' ]]; then
  echo 'Unsupported operation.' >&2
  exit 2
fi
if [[ -n "$marker_rows" ]]; then
  echo 'Bootstrap blocked: a completion marker already exists. Use normal deploy.' >&2
  exit 1
fi

# A missing marker alone is inconclusive. Any Plan 081 fixture key means the target
# needs manual inspection before bootstrap; partial data is never retried here.
fixture_count="$(mysql_query "
SELECT
  (SELECT COUNT(*) FROM academic_year WHERE code IN ('2026-2027','2025-2026')) +
  (SELECT COUNT(*) FROM app_user WHERE user_name = 'academic.office') +
  (SELECT COUNT(*) FROM student WHERE student_code BETWEEN 'STU2600001' AND 'STU2600160') +
  (SELECT COUNT(*) FROM teacher WHERE teacher_code BETWEEN 'GV001' AND 'GV020') +
  (SELECT COUNT(*) FROM placement_session WHERE JSON_UNQUOTE(JSON_EXTRACT(scope_snapshot, '$.seedKey')) IN ('PLACE-081-DRAFT','PLACE-081-SIM','PLACE-081-READY','PLACE-081-CONFIRMED','PLACE-081-CANCELLED')) +
  (SELECT COUNT(*) FROM notification WHERE idempotency_key IN ('NOTI-081-IND-01','NOTI-081-IND-02','NOTI-081-CLASS-01','NOTI-081-SCHOOL-01','NOTI-081-EMAIL-01','NOTI-081-EXPIRED','NOTI-081-CANCELLED','NOTI-081-DRAFT')) +
  (SELECT COUNT(*) FROM functional_room WHERE code IN ('LAB-PHY-01','LAB-CHEM-01','LAB-BIO-01','TIN-1','TIN-2','NGHE-1')) +
  (SELECT COUNT(*) FROM subject_functional_room sfr
    JOIN subject su ON su.subject_id = sfr.subject_id
    JOIN functional_room fr ON fr.room_id = sfr.functional_room_id
    WHERE (su.code = 'TIN_HOC' AND fr.code IN ('TIN-1','TIN-2'))
       OR (su.code = 'NGHE_DIEN' AND fr.code = 'NGHE-1')
       OR (su.code = 'NGHE_NONG_NGHIEP' AND fr.code = 'NGHE-1')) +
  (SELECT COUNT(*) FROM school_class sc JOIN academic_year ay ON ay.academic_year_id = sc.academic_year_id
    JOIN homeroom_assignment ha ON ha.class_id = sc.class_id
    WHERE ay.code = '2026-2027' AND sc.class_code IN ('6A1','6A2','6A3','6A4','7A1','7A2','7A3','7A4','8A1','8A2','8A3','8A4','9A1','9A2','9A3','9A4')) +
  (SELECT COUNT(*) FROM school_class sc JOIN academic_year ay ON ay.academic_year_id = sc.academic_year_id
    JOIN class_subject cs ON cs.class_id = sc.class_id
    JOIN semester sem ON sem.semester_id = cs.semester_id
    JOIN subject_teaching_assignment sta ON sta.class_subject_id = cs.class_subject_id
    WHERE ay.code = '2026-2027' AND sem.code IN ('HK1','HK2')
      AND sc.class_code IN ('6A1','6A2','6A3','6A4','7A1','7A2','7A3','7A4','8A1','8A2','8A3','8A4','9A1','9A2','9A3','9A4')) +
  (SELECT COUNT(*) FROM timetable_audit WHERE action IN ('SEED_PLAN_081_FULL_V2','SEED_PLAN_081_G7')) +
  (SELECT COUNT(*) FROM timetable_revision tr
    JOIN semester sem ON sem.semester_id = tr.semester_id
    JOIN academic_year ay ON ay.academic_year_id = sem.academic_year_id
    WHERE ay.code IN ('2026-2027','2025-2026') AND sem.code IN ('HK1','HK2')) +
  (SELECT COUNT(*) FROM student_year_enrollment e
    JOIN student st ON st.student_id = e.student_id
    JOIN academic_year ay ON ay.academic_year_id = e.academic_year_id
    WHERE ay.code = '2025-2026' AND st.student_code BETWEEN 'STU2600041' AND 'STU2600080') +
  (SELECT COUNT(*) FROM student_annual_transcript annual
    JOIN student st ON st.student_id = annual.student_id
    JOIN academic_year ay ON ay.academic_year_id = annual.academic_year_id
    WHERE ay.code = '2025-2026' AND st.student_code BETWEEN 'STU2600041' AND 'STU2600080') +
  (SELECT COUNT(*) FROM student_term_transcript tt
    JOIN student st ON st.student_id = tt.student_id
    JOIN student_annual_transcript annual ON annual.annual_transcript_id = tt.annual_transcript_id
    JOIN academic_year ay ON ay.academic_year_id = annual.academic_year_id
    WHERE ay.code = '2025-2026' AND st.student_code BETWEEN 'STU2600041' AND 'STU2600080') +
  (SELECT COUNT(*) FROM scorebook sb
    JOIN class_subject cs ON cs.class_subject_id = sb.class_subject_id
    JOIN school_class sc ON sc.class_id = cs.class_id
    JOIN academic_year ay ON ay.academic_year_id = sc.academic_year_id
    JOIN subject su ON su.subject_id = cs.subject_id
    JOIN semester sem ON sem.semester_id = cs.semester_id
    WHERE ay.code IN ('2026-2027','2025-2026')
      AND ((ay.code = '2026-2027' AND sc.class_code = '6A1' AND su.code = 'TOAN' AND sem.code = 'HK1')
        OR (ay.code = '2025-2026' AND sc.class_code IN ('6A1','6A2','6A3','6A4')))
)")"
if [[ "$fixture_count" != '0' ]]; then
  printf 'Bootstrap blocked: detected %s Plan 081 fixture business keys without a completion marker. Inspect this DB manually; do not rerun seed automatically.\n' "$fixture_count" >&2
  exit 1
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then echo 'seed_state=clean-for-plan-081-keys' >> "$GITHUB_OUTPUT"; else echo 'seed_state=clean-for-plan-081-keys'; fi
echo 'Read-only preflight found no Plan 081 marker or canonical fixture business keys across identity, academic year, assignments, functional rooms, timetable, historical academic data, placement, notifications, and scorebook.'
