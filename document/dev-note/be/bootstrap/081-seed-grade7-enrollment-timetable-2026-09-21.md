# Plan 081 — Seed unassigned khối 7, học vụ lịch sử và TKB đủ 16 lớp

- Cập nhật thực tế: 2026-09-22
- Trạng thái: IMPLEMENTED PRODUCTION WRITE-SET; focused validation PASS; full backend gate chưa đạt.

## Phạm vi

- Giữ 40 học sinh `STU2600041`–`STU2600080` ở trạng thái `ACTIVE` nhưng không tạo enrollment trong năm học, để repository/API unassigned trả đúng 40; không tự xóa enrollment cũ.
- Seed TKB đủ 16 lớp `6A1`–`9A4` ở HK1 `2026-09-01`–`2026-12-31` và HK2 `2027-01-01`–`2027-05-31`, 376/388 tiết môn học, `day_of_week=1..5`, assignment ACTIVE.
- Seed `TIN-1`, `TIN-2`, `NGHE-1`, mapping đúng môn; entry resolve phòng theo code và giữ `DRAFT`.
- Có audit marker `SEED_PLAN_081_FULL_V2`; rerun no-op, chỉ upgrade revision G7 legacy khi không có entry ngoài fixture.
- Bổ sung seed học vụ lịch sử năm `2025-2026` ở trạng thái `CLOSED`: bốn lớp Khối 6, 40 học sinh `STU2600041`–`STU2600080` được enrollment và hoàn tất học kỳ/năm học.
- Bổ sung scorebook lịch sử cho 80 lớp–học kỳ, 320 assessment columns và 3.200 student scores; điểm deterministic trong dải `6.5..9.5` và toàn bộ ô điểm ở trạng thái `SCORED`.
- Bổ sung hồ sơ transcript dẫn xuất gồm 40 annual transcripts, 80 term transcripts, 800 term subject results và 400 annual subject results; transcript chính thức ở trạng thái `FINISH`, version nguồn/đã tính khớp.
- Canonical identity có đủ 160 tên duy nhất: 100 tên họ Nguyễn và 60 tên họ Lý; semantics 40 học sinh G7 chưa được xếp lớp trong năm `2026-2027` vẫn được giữ nguyên.

## Thay đổi

- `DemoIdentitySeeder`: bỏ qua việc tạo enrollment cho 40 học sinh G7, không overwrite/xóa enrollment cũ.
- `DemoFunctionalRoomSeeder`: canonical DB sạch giữ ba phòng LAB không phải Tin/Nghề, thêm đúng hai phòng Tin và một phòng Nghề; không seed mới mã legacy `LAB-IT-01`.
- `DemoSubjectFunctionalRoomSeeder`: mapping natural-key idempotent cho Tin/Nghề.
- `DemoTimetableSeeder`: fixture TKB HK1/HK2 đủ 16 lớp, room binding, date/assignment/conflict checks và legacy-upgrade guard.
- `DemoTimetableScheduleCatalog`: tách 32 bảng lịch ra khỏi orchestrator để loại PMD `GodClass` mà không đổi slot data.
- `DemoTimetableCatalogGateway` và `DemoTimetablePersistenceGateway`: gom repository dependencies, giảm constructor seeder xuống hai collaborator để loại PMD `ExcessiveParameterList`.
- Compile follow-up: bổ sung `scheduleKey(semesterCode, classCode)` vào `DemoTimetableScheduleCatalog`; helper tạo khóa `semesterCode + "|" + classCode` đúng với 32 call sites.
- `DemoDataSeeder`: wire room, mapping và timetable seed sau catalog/assignment theo đúng thứ tự.
- `DemoHistoricalAcademicSeeder`: tạo năm học lịch sử `2025-2026`, bốn lớp Khối 6, enrollment `COMPLETED` cho 40 học sinh, scorebook/điểm HK1–HK2 và transcript qua pipeline hiện hữu, theo natural key/idempotency.
- `DemoIdentitySeeder`: mở rộng canonical name catalog thành đúng 160 tên theo danh sách đã cung cấp (100 Nguyễn + 60 Lý), giữ username unique trong giới hạn schema.
- PMD refactor follow-up: đổi tên private repository fields trong hai gateway, bỏ suppression thừa, đưa schedule field lên đầu class và tách entry/period/conflict planning sang `DemoTimetableEntryPlanner`; không đổi accessor hoặc fixture behavior.

## Validation

- Focused historical test: PASS (`1m34s`) sau khi bổ sung assertion historical theo Validation Result cuối.
- `compileJava`: PASS.
- `checkstyleMain`: PASS, còn `978` warning baseline.
- `pmdMain`: FAIL với 7 vi phạm baseline tại `DemoIdentitySeeder.java`.
- `git diff --check`: PASS.
- Full `test`: FAIL/timeout sau `120s`, với nhiều lỗi context tại `DemoIdentitySeeder.java:288`.
- Full `build`: NOT RUN vì full test không hoàn tất.
- Objective-focused evidence vẫn PASS; không ghi nhận full backend xanh.
- Không claim rule hoặc record `PROMOTED`; v3 chưa định nghĩa quyết định “đủ lên lớp”.
- `application.properties` có thay đổi sẵn ngoài scope tài liệu và được giữ nguyên.
- Canonical room set trên DB sạch gồm `LAB-PHY-01`, `LAB-CHEM-01`, `LAB-BIO-01`, `TIN-1`, `TIN-2`, `NGHE-1`; `LAB-IT-01` không còn seed mới. Mapping môn–phòng, marker `SEED_PLAN_081_FULL_V2` và idempotency đã được TEST ghi nhận.
- Phần cập nhật tài liệu này chỉ sửa Dev Note/summary, không sửa Java, test, config hoặc ruleset; các thay đổi Java/test của implementation và TEST culi được ghi nhận ở các mục trên.

## Học vụ lịch sử và giới hạn nghiệp vụ

- Historical seed phục vụ dữ liệu đầu vào cho demo placement: 40 học sinh có enrollment Khối 6 năm `2025-2026`, điểm đủ cao trong dải `6.5..9.5` và hồ sơ transcript `FINISH` đầy đủ.
- Đây là bằng chứng dữ liệu điểm/học bạ chính thức đã tính xong (`FINISH`, version khớp), không phải quyết định nghiệp vụ “đủ lên lớp”. V3 không định nghĩa rule/record promotion; không tạo cờ hoặc bản ghi `PROMOTED` giả định.
- Sau khi historical seed hoàn tất, enrollment lịch sử ở trạng thái `COMPLETED`; semantics hiện tại của 40 học sinh G7 `ACTIVE` nhưng chưa có enrollment năm `2026-2027` vẫn không bị thay đổi.
