# Developer Plan 087 — Scorebook context giới hạn theo phân công giáo viên

## Trạng thái

- Application-document version: `v3` (scorebook kế thừa contract v2).
- Status: `IMPLEMENTED SLICE — VALIDATION INCOMPLETE` (approved by user `2026-09-29`).
- Phạm vi: backend lookup, frontend scorebook context, kiểm thử và tài liệu contract liên quan.

## 1. Mục tiêu

Khi giáo viên mở workspace sổ điểm, các lựa chọn lớp và lớp-môn chỉ bao gồm phạm vi
giáo viên được phân công giảng dạy và đang có hiệu lực. Admin và Phòng giáo vụ giữ
nguyên danh sách hiện tại. Backend tiếp tục là nguồn kiểm tra quyền cuối cùng.

## 2. Nguồn và yêu cầu

- `document/application-doc/v3/ApplicationContext.md`: v3 kế thừa identity, phân công,
  điểm và bảng điểm từ v2; FE không suy diễn quyền và không thay backend authorization.
- `document/application-doc/v2/modules/04-AssessmentAndScoringModule.md`:
  `FR-SCORE-001` — GVBM mở sổ điểm của lớp-môn-học kỳ được phân công.
- `document/application-doc/v3/RequirementBaseline.md`: `NFR-V3-001` — backend là
  nguồn kiểm tra cuối cùng cho quyền và tính toàn vẹn.
- Code hiện tại: `ScorebookWorkspaceView` nạp danh mục lớp/môn; `ScorebookGuard` kiểm
  tra quyền scorebook bằng phân công GVBM hiệu lực; API xem môn của lớp có thể cho
  GVCN xem cả lớp nên không đủ để quyết định danh sách môn của GVBM.

## 3. Phạm vi

### In-scope

- Cấp lookup lớp-môn dành cho giáo viên hiện tại, dựa trên danh tính phiên đăng nhập;
  client không truyền teacher ID làm căn cứ quyền.
- Chỉ trả phân công GVBM khớp năm học/học kỳ được chọn và còn hiệu lực theo cùng quy
  tắc ngày hiệu lực mà backend dùng để cho phép truy cập sổ điểm.
- Dùng kết quả đó để giới hạn các lựa chọn năm học/học kỳ/lớp/môn trong workspace TEACHER;
  reset lựa chọn phụ thuộc khi context thay đổi hoặc không còn hợp lệ.
- Giữ nguyên trải nghiệm danh mục của ADMIN và ACADEMIC_OFFICE.
- Cập nhật API guide/DTO/service/view/component theo contract được triển khai; bổ sung
  kiểm thử backend authorization/query và frontend service/workspace.
- Giữ các guard trên API sổ điểm và nhập điểm làm kiểm tra quyền bắt buộc.

### Out-of-scope

- Thay đổi phân công, lifecycle sổ điểm, nhập/công bố điểm, schema/migration hoặc dữ liệu.
- Quyền GVCN xem/chỉnh sửa sổ điểm; phân công chủ nhiệm không thay cho phân công GVBM.
- Tái thiết kế role discovery/navigation hoặc thay đổi các lookup dùng ngoài workspace.
- Browser/live hoặc xác nhận dữ liệu production nếu không có môi trường fixture phù hợp.

## 4. Phương án và ranh giới

Backend cung cấp lựa chọn đã giới hạn theo giáo viên xác thực, kỳ học và hiệu lực phân
công. FE gọi qua typed service; không lọc cục bộ trên danh sách lớp/môn phổ thông để
tạo cảm giác phân quyền. Các vai trò office tiếp tục dùng lookup hiện có. API chi tiết
được ghi trong contract docs cùng implementation, không thay đổi schema.

## 5. Khu vực dự kiến

- Backend assignment controller/service/repository/access service và test tương ứng.
- `FE/src/services/academicApi.ts` hoặc service assignment có sẵn, types assignment,
  `ScorebookWorkspaceView.vue`, `ScorebookContextPanel.vue` và specs.
- `document/application-doc/v2/FrontendApiGuide.md` cùng contract/API note liên quan;
  Dev Note sau triển khai.

## 6. Kiểm thử và validation

- Backend: giáo viên chỉ truy vấn được phân công của chính mình; kết quả lọc theo năm
  học/học kỳ/ngày hiệu lực; giáo viên không có phân công nhận danh sách rỗng; office
  lookup hiện tại không đổi; truy cập scorebook ngoài phân công vẫn bị từ chối.
- Frontend: teacher gọi lookup đã giới hạn, các dropdown chỉ chứa giá trị trả về, đổi
  kỳ/lớp reset context cũ, danh sách rỗng hiển thị đúng; office path giữ nguyên.
- Chạy gates FE/BE theo workflow và skill validation tương ứng; mọi gate không chạy ghi
  `NOT RUN`/`BLOCKED`.
- DEV và TEST là hai agent khác nhau. DEV không chạy hoặc tự xác nhận test; TEST độc lập
  chạy kiểm thử/validation sau khi DEV báo bàn giao.

## 7. Rủi ro và giới hạn

- Phải dùng đúng cùng quy tắc hiệu lực ngày với `ScorebookGuard`; khác biệt giữa lọc dropdown
  và guard sẽ gây lựa chọn không mở được hoặc quyền truy cập sai.
- Không có teacher ID đáng tin cậy ở FE; vì vậy backend phải suy ra giáo viên từ principal.
- Không thay đổi behavior danh mục cho office và không cấp quyền qua phân công GVCN.

## 8. Bổ sung được duyệt — giáo viên tạo sổ điểm

- Approved by user: `2026-09-29` (teacher creation request; explicit approval received).
- Giáo viên được tạo sổ điểm cho `classSubjectId` thuộc phân công GVBM của chính
  tài khoản đang đăng nhập và còn hiệu lực tại thời điểm tạo.
- Backend kiểm tra quyền tạo theo cùng guard/phạm vi phân công đang dùng để truy cập
  sổ điểm; client không được tự quyết định quyền. Giáo viên ngoài phân công nhận
  `403`. Admin và ACADEMIC_OFFICE giữ nguyên quyền tạo hiện tại.
- FE cho phép thao tác tạo ở vai trò TEACHER khi có lớp-môn hợp lệ trong context;
  cập nhật trạng thái rỗng để không hướng giáo viên đến giáo vụ khi họ có quyền tạo.
- Không đổi schema/migration, không cấp quyền dựa trên phân công GVCN, và không mở
  rộng quyền lifecycle khác (mở/công bố/chỉnh sửa) ngoài các guard đang áp dụng.
- Kiểm thử: teacher được phân công tạo thành công; teacher không được phân công bị
  từ chối; Admin/ACADEMIC_OFFICE không hồi quy; FE hiển thị/gọi luồng tạo đúng role.
- DEV và TEST tiếp tục là hai agent độc lập; TEST không sửa code/test artifacts.
