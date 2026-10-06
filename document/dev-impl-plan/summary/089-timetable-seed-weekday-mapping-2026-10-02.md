# Developer Plan 089 — Sửa quy ước ngày của seed thời khóa biểu

> **SUPERSEDED — 2026-10-02:** [Plan 090](090-iso-weekday-2026-10-02.md) thống nhất ISO `1=Thứ Hai .. 7=Chủ Nhật`. Không triển khai chuyển seed sang `2..6` hoặc marker repair V3 theo thiết kế bên dưới. Nội dung cũ được giữ làm lịch sử đề xuất; code seed `1..5` đã đúng ISO. Repair dữ liệu DB cần audit riêng.

## 1. Mục tiêu và nguồn

- Ngày: 2026-10-02.
- Nền ứng dụng: v3, module [Timetable and Teaching Load](../../application-doc/v3/modules/02-TimetableAndTeachingLoad.md).
- Triệu chứng: lịch Plan 081 hiển thị lệch ngày, gồm cột Thứ Bảy, ở localtest và production.
- Nguyên nhân đã xác nhận trong code: seed gán `dayOfWeek=1..5`, trong khi calendar mặc định, FE grid và contract dùng `2=Thứ Hai` đến `7=Thứ Bảy`.
- Trạng thái: **UPDATED — chờ người dùng phê duyệt triển khai plan cụ thể**.

## 2. Phạm vi

### In-scope

- Chuẩn hóa toàn bộ 32 bảng `DemoTimetableScheduleCatalog` sang `2..6` cho Thứ Hai–Thứ Sáu; giữ nguyên thứ tự ngày, tiết, môn, giáo viên/phân công và phòng.
- Đổi guard/helper sang nhận diện Monday-Friday bằng `2..6`.
- Nâng revision seed marker từ `SEED_PLAN_081_FULL_V2` lên marker V3. Chỉ tự tạo revision V3 khi mọi revision có entry của học kỳ đã được chứng minh là fixture V2 bằng audit marker V2; mọi revision không marker hoặc có entry ngoài fixture khiến seeder no-op.
- Thêm một đường chạy riêng, opt-in và chỉ dành cho nâng cấp TKB; không phụ thuộc marker hoàn tất toàn bộ `DEMO_FIXTURE_PLAN_081`, do `DemoDataSeeder.run()` hiện return trước khi gọi các seeder khi marker này tồn tại.
- Cho phép vận hành cùng một đường chạy trong localtest và production theo một-shot activation: flag/operation riêng cho timetable repair, tắt ngay sau khi tác vụ kết thúc; không bật lại toàn bộ demo seeding trên DB đã có dữ liệu.
- Thêm preflight/readback cho từng semester: xác nhận head/current revision, marker V2, không có revision/entry không nhận diện được; postflight xác nhận marker V3, current revision V3, entry counts 376/388 và weekday index chỉ `2..6`.
- Giữ revision cũ V2 để audit/lesson-log references; revision mới thành current revision theo cơ chế head hiện có.
- Đồng bộ tài liệu thiết kế, fixture summary, Plan 081 và Dev Note cho quy ước `2..6`, marker mới, guard upgrade và giới hạn không tự ghi đè lịch không nhận diện được.

### Out-of-scope

- Không kết nối hoặc ghi dữ liệu vào localtest/production trong task code này; không bật bất kỳ cờ seed/repair nào trên môi trường nào.
- Không tự chạy/deploy one-shot repair lên localtest hoặc production trong task implementation; vận hành môi trường là bước riêng sau khi code được duyệt/phát hành.
- Không migrate/xóa/sửa trực tiếp revision không có marker fixture.
- Không thay lịch tuần mẫu, SHCN, phân công giáo viên, số tiết, phòng hoặc workflow.
- Không sửa phần FE đang thay đổi sẵn trong worktree.

## 3. Thiết kế

1. Catalog giữ weekday index ứng dụng `2..6`; assertion fail-fast nếu slot ngoài range.
2. Tạo use case/runner riêng cho timetable fixture repair với cờ chuyên biệt, mặc định tắt. `APP_SEED_DEMO_ENABLED` vẫn chỉ điều khiển bootstrap demo đầy đủ; cờ repair không gọi identity, academic, placement, scorebook hoặc historical seed.
3. Marker V3 ghi `dayOfWeek=2-6`. Preflight theo từng học kỳ: nếu V3 đã tồn tại thì no-op; nếu các revision có entry đều được nhận diện hoàn toàn là V2 fixture và không có entry ngoài fixture, append revision V3 và cập nhật current head; nếu phát hiện revision/entry không nhận diện được hoặc legacy fixture chưa nằm trong policy, fail-closed, không ghi gì.
4. Nếu head chưa có entry, không dùng repair path để tạo fixture đầy đủ; trường hợp đó thuộc bootstrap Plan 081 riêng. Repair path chỉ chuyển fixture V2 đã tồn tại.
5. Tài liệu lịch chi tiết thể hiện ngày theo quy ước ứng dụng, mapping với bảng lịch không đổi vì nội dung lịch chỉ gồm Thứ Hai–Thứ Sáu.

## 4. Test plan

- `DemoDataSeederIntegrationTest`: seed HK1/HK2 xác nhận tất cả period index thuộc `2..6`, không có `1`/`7`, vẫn đủ 376/388 entry và giữ môn/phòng/date/conflict assertions.
- Rerun sau V3: xác nhận không tạo revision trùng, head/current revision ổn định.
- Upgrade có điều kiện: V2 marker-only được thêm V3 revision; revision có entry không marker không bị thay đổi; mixed fixture/non-fixture cũng no-op.
- Repair path mặc định không chạy; bật flag riêng gọi đúng timetable use case trong khi `DEMO_FIXTURE_PLAN_081` đã complete; không gọi các seeder khác.
- Preflight fail-closed tạo zero revision/entry/audit writes; postflight chứng minh marker/head/count/day indexes.
- Các test của seeder/revision được mock/fake repositories theo pattern hiện hữu; assert revision/status/current head/audit/entry counts.
- Lệnh focused: `./gradlew --no-daemon --max-workers=1 test --tests com.JavaTraining.BaiTap_RS.bootstrap.DemoDataSeederIntegrationTest`.
- Backend validation sau code theo skill: `test`, `checkstyleMain`, `pmdMain`, `build`; coverage JaCoCo không có threshold mới.
- Static audit: parse 32 schedules, verify slot set/count, `git diff --check`, đối chiếu tài liệu.

## 5. File dự kiến

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoTimetableScheduleCatalog.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoTimetableSeeder.java`
- Một runner/configuration riêng cho timetable-only repair và kiểm soát flag mặc định false.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoDataSeederIntegrationTest.java`
- Test riêng cho runner/repair preflight, idempotency, fail-closed và không kích hoạt các seeder khác.
- `document/dev-note/be/timetable/081-full-timetable-design-2026-09-21.md`
- `document/dev-note/be/bootstrap/081-seed-grade7-enrollment-timetable-2026-09-21.md`
- `document/dev-note/summary/081-seed-data-v2-v3-2026-09-18.md`
- `document/dev-note/summary/081-seed-data-v2-v3-2026-09-18/07-unseeded-fixtures.md`
- Dev Note mới dưới `document/dev-note/be/timetable/` và index summaries.

## 6. Rủi ro và xác minh ngoài task

- Localtest: sau khi phát hành code sửa, dùng operation/flag riêng để chạy timetable-only repair một lần trên đúng DB localtest; không dùng `APP_SEED_DEMO_ENABLED=true` trên DB đã có dữ liệu vì nó bật toàn bộ demo seed và marker completion có thể làm runner tổng return sớm. Đọc lại marker/revision/count/day trước khi tắt cờ.
- Production: thêm operation được kiểm soát vào workflow deploy/maintenance (hoặc vận hành qua runner one-shot tương đương), preflight trên đúng DB, giới hạn một replica/runner, repair flag chỉ bật cho tác vụ, hậu kiểm marker V3/head/count/day rồi tắt flag và xác minh app thường chạy với seed false. Normal deploy/bootstrap hiện tại không được giả định tự repair DB đã có marker.
- Task này chỉ thêm code và tài liệu; không thực thi remote. Khi vận hành production, cần xác nhận đúng target/DB và một cửa sổ bảo trì phù hợp; không reset DB hoặc xóa revisions.
- Dữ liệu có entry không marker giữ nguyên để bảo toàn lịch người dùng; nếu đó là dữ liệu seed cũ nhưng mất audit marker, cần audit riêng bằng snapshot DB trước khi có phương án repair có điều kiện.
- Giữ V2 revisions có nghĩa lesson log đã trỏ đến chúng không bị mất lịch sử; FE xem revision current sẽ lấy lịch V3 sau khi bootstrap thành công.

## 7. Approval

- Chờ người dùng duyệt phạm vi cập nhật, đặc biệt thêm one-shot timetable-only repair path và policy fail-closed khi không xác nhận được toàn bộ fixture V2 qua audit marker.
