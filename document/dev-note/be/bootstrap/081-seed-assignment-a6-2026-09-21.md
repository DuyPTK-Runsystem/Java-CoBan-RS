# Dev Note 081: Full assignment fixture và định mức tuần

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

- `document/dev-impl-plan/summary/081-seed-data-v2-v3-2026-09-18.md`: đồng bộ A.4, A.5, A.6, bảng tiết theo khối, số rows/tải mỗi học kỳ và ngoại lệ Công nghệ khối 9 HK2.
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`: thay mô tả Plan 081 bằng phạm vi full assignment và trạng thái review.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`: thay mô tả Plan 081 bằng amendment đã chốt và trạng thái review.

## Validation Result

| Gate | Status | Evidence |
|---|---|---|
| Documentation consistency | PASS | Plan 081 và hai summary dùng cùng số `168/172/340`, `376/388`, ngưỡng `[15,24]` / `[19,24]`, và loại `CONG_NGHE` khối 9 HK2. |
| Focused test sau amendment | PASS | `cleanTest test --tests DemoDataSeederIntegrationTest -x jacocoTestReport --no-daemon --max-workers=1`; XML ghi nhận `6 tests, 0 failures, 0 errors`. |
| JaCoCo sau amendment | PASS | `jacocoTestReport -x test --no-daemon --max-workers=1` hoàn tất thành công từ execution data của focused test. |
| Checkstyle | PASS | `checkstyleMain` PASS; `build` cũng chạy `checkstyleTest` với warning có sẵn, không có error. |
| PMD main | FAIL - baseline | Còn đúng 2 lỗi ngoài phạm vi: `DemoScorebookSeeder.java:44` và `LessonLogScheduleEntryFactory.java:30`; không còn lỗi ở các class/helper của fixture. |
| Full test | FAIL - baseline | `551 tests completed, 99 failed`; các context failure quy về `DemoIdentitySeeder.java:176` thiếu role seed `ACADEMIC_OFFICE`. |
| Build | FAIL - baseline | `build` dừng tại `pmdMain` do 2 lỗi baseline nêu trên; compile, bootJar và checkstyle đã chạy PASS. |

## Historical validation và rủi ro

- Focused test hậu-amendment xác nhận đủ `340` assignment, scope môn, skill độc quyền theo lớp, giới hạn tối đa 2 môn/GV, lower/upper bound tải và idempotency. Full suite vẫn bị chặn bởi lỗi seed identity nền; PMD còn 2 lỗi baseline ngoài phạm vi.
- Existing database chứa `class_subject`/assignment fixture cũ không tự được dọn; reconciliation hoặc clean demo database cần approval riêng.
- Constraint lower bound ở đây là hard bound của fixture seed đã được user chốt, không tự thay thế policy timetable có `source`, `effectiveFrom` và `version`.

## Next step

- Có thể tiếp tục review/commit theo approval riêng; không cần bổ sung GV cho fixture hiện tại vì 20 GV đáp ứng toàn bộ lower/upper bound đã chốt.
