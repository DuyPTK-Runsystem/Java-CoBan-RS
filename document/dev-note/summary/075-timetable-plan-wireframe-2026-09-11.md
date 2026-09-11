# Dev Note 075 — Soạn plan và wireframe thời khóa biểu

- Ngày: 2026-09-11; application-document version: v3.
- Liên quan [Plan BE](../../dev-impl-plan/be/timetable/075-timetable-teacher-load-2026-09-11.md), [Plan FE](../../dev-impl-plan/fe/timetable/075-timetable-teacher-load-ui-2026-09-11.md), [wireframe](../../wireframes/fe/timetable/075-timetable-teacher-load/README.md).
- Authorization: người dùng yêu cầu “viết plan + wireframe cho plan 75”, sau đó xác nhận “v3”. Bổ sung ngày 2026-09-11: người dùng chốt quyền đăng ký lịch bận cần duyệt trước, 2 × 4 tiết, phòng chức năng/gán môn và Giáo vụ ngang Admin. Chỉ soạn tài liệu/prototype review; implementation approval **PENDING**.

## Đã làm

- Soạn hai Developer Plan BE/FE: requirement, baseline mã nguồn, phạm vi, contract đề xuất, calendar/slot identity, conflict, policy, revision/publish/concurrency, permission, file scope, test, acceptance và delivery BE/FE song song.
- Bản đầu ghi D01–D08; amendment chốt quyền D02, cấu trúc/phạm vi D04 và cơ chế duyệt D09, bổ sung D10 cho mapping môn/phòng; giữ trị số 19/4/3 theo requirement; không tự xác nhận nguồn/ngày/version của policy.
- Tạo HTML và README dưới `document/wireframes/fe/timetable/075-timetable-teacher-load/`: lịch, editor, issue/load, cấu hình, list/create, publish/revise và selector state review.
- Đồng bộ `document/dev-impl-plan/{be/BE_DEV_PLAN_SUMMARY.md,fe/FE_DEV_PLAN_SUMMARY.md,summary/DEV_PLAN_SUMMARY.md,summary/MASTER_PLAN_V3-2026-09-09.md}`.
- Tạo note này và cập nhật summary Dev Note chung, BE và FE để truy cập cùng note docs-only.
- Amendment đồng bộ requirement được người dùng xác nhận trong `document/application-doc/v3/{RequirementBaseline.md,modules/02-TimetableAndTeachingLoad.md,data-model/README.md,ApplicationContext.md,change-request/CR-V3-001-academic-operations-and-targeted-communication.md}`; các API/schema đề xuất vẫn chờ duyệt. Không sửa application source, migration hoặc dependency. Không commit/push.

## Validation thực tế

| Kiểm tra | Kết quả |
|---|---|
| `git diff --check` | PASS |
| Node `new Function` với script inline HTML | PASS — parse JavaScript |
| Node + `FE/node_modules/jsdom`: DOM smoke | PASS — amendment: grid 2 buổi × 4 tiết, phân biệt buổi, picker phòng theo môn, sửa/validate/publish/revise, Admin/Giáo vụ ngang control, teacher gửi pending/chọn cả buổi, duyệt/từ chối, tạo phòng/gán môn và 8 scenario gates |
| Python pathlib kiểm tra relative Markdown links của file mới và link Plan 075 trong summaries | PASS |
| Native browser/screenshot, responsive layout, dialog focus/keyboard | NOT RUN — DOM smoke không kiểm chứng layout/native dialog |
| BE test/JaCoCo/Checkstyle/PMD/build | NOT RUN — chưa triển khai backend |
| FE lint/test/coverage/build/Storybook production | NOT RUN — chưa thay source FE |
| API/live/concurrency/MySQL migration | NOT RUN — chỉ lập kế hoạch |

Smoke dùng shim showModal/close và confirm trong jsdom; không coi đó là browser E2E. HTML mô phỏng conflict tiết cố định và lịch bận đã duyệt trong tuần xem; không xác minh conflict engine toàn học kỳ hoặc backend authorization/concurrency. Lệnh `python` ban đầu không tồn tại; đã chạy lại script soạn tài liệu thành công bằng `python3`.

## Khác biệt, blocker và bước tiếp

Đã mở rộng nội dung plan/wireframe theo chỉ dẫn bổ sung của người dùng, gồm lịch bận qua duyệt và module phòng chức năng/gán môn. Chưa có implementation nên không báo Plan 075 completed. D02 đã chốt; D04 đã chốt 2 × 4/phòng chức năng, còn giờ/ngày; D09 đã chốt cần duyệt, còn xử lý published và sửa/rút; D10 cardinality/điều kiện bắt buộc phòng còn mở. Policy metadata, mức chặn tải, revision effective dates, nguồn eligibility, tuần đổi điều kiện và bảo vệ dependency vẫn chờ quyết định tương ứng. Người dùng review plan/wireframe, xác nhận các quyết định trước khi code phần liên quan. Kiểm tra trực quan wireframe vẫn NOT RUN.
