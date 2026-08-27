# Dev Note: Frontend Documentation Refactor

## Related Developer Plan and approval

- Plan: `document/dev-impl-plan/fe/tooling/050-frontend-documentation-refactor-2026-08-27.md`.
- Approval: người dùng phê duyệt các đề xuất refactor 1–4 qua tin nhắn ngày
  2026-08-27.

## Actual scope completed

- Tách các quy tắc legacy Student profile UI/CRUD thành rule riêng.
- Bổ sung routing cho legacy Student UI và sửa wording account provisioning
  để không gọi endpoint v3 là flow v2.
- Bỏ numbering toàn cục khỏi các heading trong API guide v2.
- Gom TypeScript enum union về registry canonical
  `07-enums-and-known-drift.md`; các domain guide dùng cross-reference.

## Files changed

### FE rules

- `FE/AGENTS.override.md` — thêm route cho legacy Student UI.
- `FE/agent-rules/05-legacy-student-ui.md` — thêm rule flow/list/form/Storybook
  của màn hình Student hiện tại.
- `FE/agent-rules/02-domain-rules.md` — sửa terminology account provisioning.

### v2 API guide

- `document/application-doc/v2/frontend-api/02-academic-structure.md`.
- `document/application-doc/v2/frontend-api/03-teacher-assignment-enrollment.md`.
- `document/application-doc/v2/frontend-api/04-calendar-attendance.md`.
- `document/application-doc/v2/frontend-api/05-scorebook-change-audit.md`.
- `document/application-doc/v2/frontend-api/06-transcript-retake-calculation.md`.
- `document/application-doc/v2/frontend-api/07-enums-and-known-drift.md`.

Các file API còn lại được giữ nguyên nội dung; chỉ các file có heading/enum
duplication thuộc phạm vi refactor mới được chỉnh.

### Workflow documentation

- `document/dev-impl-plan/fe/tooling/050-frontend-documentation-refactor-2026-08-27.md`.
- `document/dev-note/fe/tooling/050-frontend-documentation-refactor-2026-08-27.md`.
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`.
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`.

## Important decisions

- Legacy v1 UI rules được route riêng để không trộn hành vi profile CRUD với
  business rules v2.
- `07-enums-and-known-drift.md` là nguồn canonical cho string union v2; domain
  guide vẫn giữ các ghi chú hành vi đặc thù nhưng không định nghĩa lại union.
- Không thay đổi backend, FE source code, API contract hoặc requirement.

## Validation

| Check | Result |
|---|---|
| `git diff --check` | PASS |
| Internal Markdown link target check | PASS |
| API guide global heading numbering check | PASS — không còn heading `##/###/####` đánh số trong `frontend-api/` |
| FE lint/test/build | NOT RUN — documentation-only change; không đổi FE source/config |

## Deviations and remaining risks

- Không deviation so với plan đã được phê duyệt.
- Các file mới vẫn đang ở trạng thái untracked trong working tree; cần commit
  cùng nhau để routing không trỏ tới file thiếu.
- API guide vẫn là tài liệu maintained thủ công; khi backend contract đổi cần
  cập nhật registry/domain guide cùng lần.

## Next steps

- Khi commit, kiểm tra toàn bộ file trong `FE/agent-rules/` và
  `document/application-doc/v2/frontend-api/` được đưa vào cùng changeset.
