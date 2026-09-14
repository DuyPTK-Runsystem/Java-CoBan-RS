# Dev Note 079 — Review và sửa Plan / Wireframe Sổ đầu bài

- Ngày: **2026-09-14**; application-document version: **v3**.
- [Plan BE](../../dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md), [Plan FE](../../dev-impl-plan/fe/lesson-log/079-lesson-log-ui-2026-09-14.md), [wireframe](../../wireframes/fe/lesson-log/079-lesson-log/README.md).
- Authorization ban đầu theo note trước: soạn plan/wireframe v3. Authorization mới trong phiên: **“tôi cho phép bạn sửa plan (v3) và wireframe”** sau review Plan 079.
- Phạm vi được phép: sửa tài liệu thiết kế và prototype. **D01–D10 đã APPROVED ở mức design/contract; implementation BE/FE approval vẫn PENDING**. Không đổi application baseline v3 hoặc đánh dấu TBD-003 đã resolved.

## 1. Thay đổi thực tế

### Plan BE/FE

- Sửa FK lớp sang `school_class(class_id)`; chuyển schema sang bảng thiết kế logic có FK/CHECK/unique rõ ràng. V23 là số dự kiến, phải kiểm tra lại trước implementation.
- Đề xuất identity ổn định class/date/session/period thay unique chỉ theo entry ID; giữ FK/snapshot nguồn lịch sử, resolve revision theo ngày kể cả ARCHIVED đã publish.
- Bổ sung scope guard publish/calendar trong BE để không làm sai tiết đã ghi; mô tả khóa chung, rollback và race với create. Chưa sửa code timetable.
- Thống nhất lifecycle; nháp không được miễn hạn; SUBMITTED không hạ về DRAFT; AMENDED readonly với giáo viên; quản lý ghi thiếu/hoàn thiện nháp quá hạn có reason và audit.
- Policy hai mode loại trừ nhau, deadline exclusive/timezone, phiên bản/ngày hiệu lực, rubric và dữ liệu cũ giữ nguyên. Bỏ khẳng định rubric là “chuẩn ngành” và quy đổi điểm không có nguồn.
- Quyền GVBM/GVCN/manager tách rõ; ADMIN/ACADEMIC_OFFICE parity lesson log vẫn là đề xuất riêng. Hoãn workflow chọn dạy thay ngoài TKB; actualTeacherId không phải input tùy ý.
- Ký tuần có điều kiện, version/tập entries/snapshot; sửa sau ký chuyển STALE và ký lại có reason. Không đồng nhất ký tuần với review từng tiết.
- Audit entry/tuần/policy append-only, FK RESTRICT, cùng transaction; retention/privacy/quyền sau thay GVCN vẫn mở trước production.
- Chốt HTTP status đề xuất, shapes/API cho hai lane và kế hoạch test history/race/deadline/weekly/recovery. Không tạo error code/payload v3 riêng.

### Prototype và hướng dẫn

- Cập nhật `index.html`: lịch cá nhân, ma trận bảy ngày × hai buổi × bốn tiết; summary tính từ fixture; quyền và deadline nhất quán; form draft/submit/amend/late-record; ký tuần và ký lại; history before/after; policy version.
- Bỏ ô chọn dạy thay và duyệt hàng loạt chưa có contract. Sửa filter sang nhãn tiếng Việt.
- 409 giữ input khi xem bản mới; thay input chỉ khi người dùng chọn dùng bản mới. 422 hết hạn giữ input và khóa mutation.
- Thêm `smoke.cjs` tái chạy kiểm tra tương tác; README nêu đúng fixture giới hạn và các flow chưa mô phỏng.
- Sửa trạng thái/nội dung hàng Plan 079 trong BE/FE/global plan summaries, Master Plan và Dev Note Summary; giữ các thay đổi có sẵn ngoài Plan 079.

## 2. File thay đổi trong task

- `document/dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md`
- `document/dev-impl-plan/fe/lesson-log/079-lesson-log-ui-2026-09-14.md`
- `document/wireframes/fe/lesson-log/079-lesson-log/{index.html,README.md,smoke.cjs}`
- `document/dev-impl-plan/{be/BE_DEV_PLAN_SUMMARY.md,fe/FE_DEV_PLAN_SUMMARY.md,summary/DEV_PLAN_SUMMARY.md,summary/MASTER_PLAN_V3-2026-09-09.md}` (chỉ cập nhật phần 079)
- `document/dev-note/summary/{079-lesson-log-plan-wireframe-2026-09-14.md,DEV_NOTE_SUMMARY.md}`

Các plan/wireframe đã là untracked và các summary đã modified khi bắt đầu. `git diff --stat` không phản ánh đầy đủ các file untracked; không coi toàn diff của summary là thay đổi task này.

## 3. Validation của lần sửa này

| Kiểm tra | Lệnh / công cụ | Kết quả và giới hạn |
|---|---|---|
| JS syntax và tương tác prototype | `node document/wireframes/fe/lesson-log/079-lesson-log/smoke.cjs` | **PASS: 12/12**; JSDOM với dialog polyfill; không thay browser/live API |
| Policy version trong form | Cùng smoke ở trên | Lần đầu FAIL vì version kỳ vọng chưa khởi tạo; đã sửa lấy version khi nạp fixture/lưu policy; lần chạy lại PASS |
| Link Markdown của Plan BE/FE, README, Dev Note | Python pathlib kiểm tra các đích local của Markdown links | **PASS** |
| Whitespace/scope | `git diff --check`, `git status --short`; kiểm tra riêng file untracked | **PASS**; file sản phẩm BE/FE không thay đổi |
| Browser thật / screenshot | Thử tạo tab Chrome tại local prototype | **NOT RUN**: Chrome báo đang tắt trước khi có tab; không có bằng chứng render/pixel/browser interactions |
| BE test/JaCoCo/Checkstyle/PMD/full build/MySQL/Flyway | Không triển khai source BE | **NOT RUN** |
| FE lint/Vitest/build/Storybook, API/live E2E | Không triển khai source FE | **NOT RUN** |

Smoke bao phủ nháp thiếu field/nộp đủ, sĩ số, chống tạo ô tự do qua UI, giữ SUBMITTED, khóa teacher quá hạn, manager reason/late-record, 409 giữ input, 422 readonly, GVCN không review từng tiết, ký lại và audit, policy giữ deadline cũ, nguồn TKB lưu trữ và fallback.

## 4. Khác biệt và việc còn mở

- Đây là bản sửa sau review, thay cho các khẳng định ban đầu “chống trùng triệt để” bằng unique entry ID hoặc “không có khác biệt”. Bản thiết kế mới bổ sung guard tích hợp TKB, late-record, ký lại tuần và audit policy/tuần; đồng thời đề xuất hoãn dạy thay riêng/in/PDF/duyệt hàng loạt.
- Người dùng đã duyệt D01–D10, bao gồm các lựa chọn về số giờ/timezone, quyền lesson log và quy tắc ký tuần, ở mức design/contract. Người dùng chưa duyệt implementation BE/FE; chưa có code hoặc migration được phép thực hiện.
- Cần kiểm chứng nguồn lịch sử enrollment/assignment và lịch sử Flyway trước code; retention/privacy trước production. Prototype không chứng minh DB unique, race transaction, audit bất biến hoặc khả năng publish giữ lịch sử.
- Browser/pixel và các gate sản phẩm còn NOT RUN. Task sửa tài liệu/prototype đã được kiểm tra bằng các gate phù hợp nêu trên; chưa tuyên bố module lesson log hoàn thành.
