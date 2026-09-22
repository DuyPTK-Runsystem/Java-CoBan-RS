# Dev Note 081: Full assignment fixture và định mức tuần

> Cập nhật 2026-09-22 theo Validation Result của culi TEST cuối; chỉ cập nhật tài liệu.

- Developer Plan: `document/dev-impl-plan/summary/081-seed-data-v2-v3-2026-09-18.md`.
- Approval: user approved mở rộng từ A.6 20 dòng sang fixture đủ bốn khối/hai học kỳ, sau đó chốt lại ngoại lệ `CONG_NGHE` không áp dụng khối 9 HK2 (2026-09-21).
- Vai trò: Dev, Test và tài liệu được tách phạm vi; tài liệu này chỉ ghi nhận amendment và bàn giao root review.

## Constraint đã chốt

- GVCN đủ 16 lớp `6A1`–`9A4`, mapping `GV001`–`GV016`.
- Mỗi giáo viên dạy tối đa 2 môn.
- `HOA_HOC` chỉ khối 8–9, HK1/HK2; `TIN_HOC` và `VAT_LY` luôn 2 tiết/tuần.
- `CONG_NGHE`: HK1 `[2,2,1,1]`; HK2 `[2,2,1,0]`, do đó khối 9 HK2 bị loại.
- Mỗi lớp khối 8–9 HK2 có đúng một nghề; fixture cân bằng 4 lớp Điện và 4 lớp Nông nghiệp.
- Quy mô: HK1 `168` rows / `376` tiết tuần; HK2 `172` rows / `388` tiết tuần; cả năm `340` rows.
- `24` là trần tải, không phải tải mục tiêu. Theo từng học kỳ, GVCN phải thuộc `[15,24]` (`19 - 4`), GVBM phải thuộc `[19,24]`; không để GV idle.

## Thay đổi tài liệu

- `document/dev-impl-plan/summary/081-seed-data-v2-v3-2026-09-18.md`: đồng bộ A.3.1, A.4, A.5, A.6, bảng tiết theo khối, số rows/tải mỗi học kỳ và ngoại lệ Công nghệ khối 9 HK2; A.3.1 dùng username hậu tố compact đúng với DB.
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`: thay mô tả Plan 081 bằng phạm vi full assignment và trạng thái review.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`: thay mô tả Plan 081 bằng amendment đã chốt và trạng thái review.

## Validation Result

| Gate | Status | Evidence |
|---|---|---|
| Focused test suite | PASS | Validation Result của culi TEST cuối. |
| CheckstyleMain | PASS | `975` warning baseline. |
| pmdMain | PASS | PASS. |
| git diff --check | PASS | PASS. |
| Full test | FAIL | `554 tests/99 failures`; `Missing seeded role: ACADEMIC_OFFICE` tại `DemoIdentitySeeder.java:185`. |
| pmdTest | FAIL | `313` baseline violations. |
| Build | FAIL | Do hai blocker full-test và pmdTest nêu trên. |

## Reconciled seed facts

- TKB đủ `16` lớp; HK1/HK2 `376/388` tiết; `day_of_week=1..5`.
- G7 có `40` unassigned và `120` enrollment.
- Canonical rooms trên DB sạch: `LAB-PHY-01`, `LAB-CHEM-01`, `LAB-BIO-01`, `TIN-1`, `TIN-2`, `NGHE-1`; `LAB-IT-01` không còn seed mới.
- Mapping môn–phòng, marker `SEED_PLAN_081_FULL_V2` và idempotency đã được ghi nhận.

## Historical validation và rủi ro

- Focused test suite PASS; full suite vẫn bị chặn bởi lỗi seed identity và pmdTest baseline. Không claim full backend xanh.
- Existing database chứa `class_subject`/assignment fixture cũ không tự được dọn; reconciliation hoặc clean demo database cần approval riêng.
- Constraint lower bound ở đây là hard bound của fixture seed đã được user chốt, không tự thay thế policy timetable có `source`, `effectiveFrom` và `version`.

## Next step

- `application.properties` có thay đổi sẵn ngoài scope tài liệu và được giữ nguyên.
