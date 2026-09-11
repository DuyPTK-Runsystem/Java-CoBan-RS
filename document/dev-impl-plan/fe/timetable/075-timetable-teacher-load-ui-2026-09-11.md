# Plan 075 FE — Thời khóa biểu và định mức tiết dạy

## 1. Trạng thái và nguồn

- Version **v3**, ngày **2026-09-11**.
- **DRAFT — chờ duyệt; chưa triển khai FE/BE**.
- Contract, quyết định D01–D10, test nghiệp vụ: [Plan BE](../../be/timetable/075-timetable-teacher-load-2026-09-11.md).
- Review: [README wireframe](../../../wireframes/fe/timetable/075-timetable-teacher-load/README.md), [mở HTML](../../../wireframes/fe/timetable/075-timetable-teacher-load/index.html).
- Requirement: FR-V3-TT-001..004, BR-V3-TT-001..002. HTML dùng dữ liệu minh họa, không chứng minh thuật toán hoặc quyền production.

## 2. Hiện trạng và phạm vi

Reuse `FE/src/services/{apiClient,academicApi,assignmentApi,teacherApi}.ts`, auth session, shell `FE/src/views/shell/AuthenticatedV2ShellView.vue` và `FE/src/router/index.ts`. Chưa có production timetable module trong phạm vi đã rà. Không reuse `V3Page` để thay `ResultPaginationDTO`.

Tạo workspace lịch tuần, list revision, editor tiết học, conflict panel, load panel, calendar 2 buổi × 4 tiết, module phòng chức năng, gán phòng trong môn học, đăng ký lịch bận giáo viên, policy và xác nhận miễn giảm, kiểm tra/công bố/revision. Không tự xếp lịch, không bắt buộc kéo thả, không thêm export/import hay notification. Student giữ nguyên navigation hiện hữu.

## 3. Routes và quyền — amendment D02/D04 ngày 2026-09-11

Quyền TEACHER đăng ký lịch bận của mình và ADMIN = ACADEMIC_OFFICE trong toàn Plan 075 đã được người dùng chốt. Tất cả màn cấu hình, phòng chức năng, gán môn học, miễn giảm và công bố áp dụng ngang quyền; không còn giới hạn Giáo vụ chỉ xem mức giảm. Endpoint/route chi tiết vẫn là thiết kế chờ approval triển khai.

| Route | Vai trò | Mục đích |
|---|---|---|
| /v2/timetables | ADMIN, ACADEMIC_OFFICE | Danh sách lịch theo học kỳ, tạo lịch |
| /v2/timetables/:timetableId | ADMIN, ACADEMIC_OFFICE | Xem và biên tập revision |
| /v2/my-timetable | TEACHER | Lịch công bố của mình và tab lịch bận; scope do BE ép |
| /v2/timetables/settings | ADMIN, ACADEMIC_OFFICE | Calendar/policy/xác nhận giảm tiết |
| /v2/functional-rooms | ADMIN, ACADEMIC_OFFICE | Module quản lý phòng chức năng |
| Màn quản lý môn học hiện hữu | ADMIN, ACADEMIC_OFFICE | Thêm gán phòng chức năng; không tạo danh mục môn trùng |
| /v2/my-timetable/unavailability | TEACHER | Đăng ký/sửa/rút lịch bận của mình |
| /v2/timetables/unavailability | ADMIN, ACADEMIC_OFFICE | Xem/quản lý lịch bận giáo viên; duyệt/từ chối đăng ký; xử lý trùng published theo đề xuất D09 |

Thêm `Thời khóa biểu` cho vai trò được duyệt; không dựa vào JWT/UI để quyết định quyền cuối cùng. Route tĩnh settings/unavailability khai báo rõ, không để rơi vào param ID. Backend capabilities điều khiển action hợp lệ kết hợp lifecycle.

## 4. Wireframe và luồng

### W01 — Danh sách và tạo lịch

Chọn năm học → học kỳ → danh sách lịch có phiên bản, khoảng áp dụng, trạng thái. Pagination thật ở backend, page zero-based. Nút `Tạo thời khóa biểu` mở dialog học kỳ, khoảng áp dụng và policy. Nếu chưa có calendar, dẫn vào cấu hình trước khi lưu; dùng cấu trúc đã chốt 2 buổi/ngày, 4 tiết/buổi; giờ chuông/ngày học vẫn cần cấu hình được duyệt. Empty có action tạo, loading không hiển thị fake data, lỗi có thử tải lại.

### W02 — Lịch tuần

Header: `Thời khóa biểu`, học kỳ, `Bản nháp`/`Đã kiểm tra`/`Đã công bố`; chọn tuần và chế độ `Theo lớp`, `Theo giáo viên`, `Theo phòng chức năng`, kèm filter buổi và tiết 1–4. Grid nhóm **Sáng 1–4, Chiều 1–4** (8 ô theo tiết cho mỗi ngày), ngày/giờ thực từ calendar. Card hiển thị môn, lớp/giáo viên theo ngữ cảnh, phòng; có dấu và chữ khi trùng, không chỉ màu.

Click ô trống mở `Thêm tiết học`; click card mở `Sửa tiết học`. Form gồm phân công hợp lệ (môn, GV, lớp), thứ, buổi, tiết 1–4, khoảng áp dụng và phòng chức năng theo mapping môn. Môn không dùng phòng chức năng ẩn picker/lưu null; môn được gán phòng chỉ chọn trong danh sách hợp lệ. Quy tắc bắt buộc mọi tiết hay theo tiết thực hành chờ D10. Backend resolve identity. Lưu → tải lại detail/week/review. Xóa tiết cần confirm, chỉ xóa draft. Giữ input khi lỗi, khóa double-submit. Không auto-save kéo thả chưa review.

### W03 — Kiểm tra lịch

Panel `Cần xử lý` cạnh grid: tổng lỗi toàn lịch; mỗi dòng mô tả `Cô Lan có hai lớp vào Thứ hai, buổi sáng, tiết 1`, kèm lớp/buổi/phòng chức năng cụ thể và `Đến tiết học`. Action chuyển filter/tuần đến entry đó, giữ các input chưa lưu bằng confirm bỏ thay đổi. Filter đang xem không được làm tổng lỗi thành 0. `Lưu nháp` cho phép conflict nghiệp vụ; `Kiểm tra lịch` chạy backend. `Công bố` bị khóa khi chưa kiểm tra/còn lỗi/thiếu policy/quyền, lý do hiện ngay bên cạnh.

Hiển thị thêm issue `Trùng lịch bận của giáo viên` khi đăng ký đã có hiệu lực theo D09; có ngày/buổi/tiết, không lộ lý do riêng tư trên lịch chung.

### W04 — Định mức tiết dạy

Tab có giáo viên, số tiết đã xếp/tuần, chuẩn, mức giảm, định mức sau giảm, chênh lệch, trạng thái. Không tự tính từ các card đang tải. Chi tiết hiển thị nguồn policy và khoảng hiệu lực; chỉ role có quyền thấy lý do nhạy cảm. Chưa có nguồn dùng copy `Chưa có chính sách đủ thông tin để đối chiếu`; không biến missing thành 0. 19/15/16/12 chỉ là fixture từ tham số đã có trong requirement.

### W05 — Công bố và revision

`Công bố` mở dialog: học kỳ, phiên bản, ngày áp dụng, policy, tổng lỗi/cảnh báo toàn scope; có ghi rõ lịch trước được thay thế từ ngày nào. Confirm dùng expectedVersion, expectedHeadVersion và idempotency key của một intent. Sau thành công tải lại, khóa sửa và hiện `Tạo bản điều chỉnh`. Không đổi badge trước khi BE thành công. Revision mới vẫn là draft, lịch cũ còn hiệu lực tới mốc thay thế được duyệt D05.

### W06 — Cấu hình

Dành cho ADMIN và ACADEMIC_OFFICE ngang quyền. Calendar có 2 buổi/ngày, mỗi buổi 4 tiết, giờ chuông/ngày nghỉ; không còn switch áp dụng phòng thường. Policy có nguồn, ngày hiệu lực, phiên bản và tham số 19/4/3; activation khóa khi metadata thiếu. Xác nhận miễn giảm và thông tin căn cứ có cùng quyền cho hai role. Policy active read-only, sửa bằng version mới.

### W07 — Quản lý phòng chức năng và gán môn

Module `Phòng chức năng`: danh sách tìm kiếm/phân trang, thêm/sửa mã/tên/trạng thái; xóa chỉ phòng chưa có tham chiếu, ngừng dùng báo các tiết bị ảnh hưởng. Màn `Môn học` hiện hữu thêm vùng `Phòng chức năng`: chọn phòng đang hoạt động (đề xuất nhiều lựa chọn, chờ D10), lưu mapping có version. Không gán phòng học thường. Lịch chọn phòng theo môn; không dùng fixture làm lựa chọn production. Sau đổi mapping/phòng, reload và validate lại draft; bảo vệ lịch đã công bố.

### W08 — Đăng ký lịch bận

Teacher mở `Lịch của tôi` → `Lịch bận` → `Đăng ký lịch bận`: chọn học kỳ, một ngày hoặc lặp theo tuần (đề xuất), khoảng hiệu lực, thứ khi lặp, buổi Sáng/Chiều, một hay nhiều tiết trong 1–4, ghi chú tùy chọn. Chọn cả buổi tương ứng đủ bốn tiết. Danh sách có ngày/khoảng áp dụng, buổi, tiết, trạng thái, sửa/rút đăng ký. Không có chọn giáo viên khác ở flow teacher.

ADMIN và ACADEMIC_OFFICE có cùng màn danh sách để lọc giáo viên và xử lý tác động lên TKB. Đăng ký trùng lịch đã công bố hiển thị các tiết bị ảnh hưởng; không tự dời/hủy tiết. **Đã chốt cần duyệt trước.** Sau gửi hiện `Chờ duyệt`; chỉ `Đã duyệt` mới chặn tiết trùng. ADMIN/ACADEMIC_OFFICE có `Duyệt`/`Từ chối`, lý do từ chối và xác nhận; teacher không có action duyệt. Đề xuất chỉ sửa bản chờ duyệt, rút bản cũ để gửi lại nếu đã được duyệt; D09 còn chờ chốt thao tác sau duyệt và lịch published. Nếu approve trả conflict, giữ đăng ký chờ duyệt, chỉ ra tiết đã công bố cần điều chỉnh rồi duyệt lại. Wireframe mô phỏng cả gửi và quyết định duyệt, không lưu dữ liệu thật.

## 5. State và recovery

| State | UI |
|---|---|
| loading / empty | Skeleton hoặc hướng dẫn tạo lịch; không dùng fixture production |
| DRAFT có conflict | Cho lưu; badge chưa công bố, khóa publish |
| VALIDATED | Cho publish nếu capability và dependency còn hợp lệ |
| PUBLISHED / ARCHIVED | Read-only; chỉ bản phù hợp có action revise |
| 400 / 422 | Giữ form, hiển thị lỗi an toàn; không tự map field không có trong response |
| 401 | `apiClient` xử lý session/login |
| 403 | Giữ session, không đủ quyền, không lộ dữ liệu đã cache khác scope |
| 404 | Không tìm thấy lịch, quay lại danh sách |
| 409 | Giữ input để đối chiếu, khóa submit cũ, `Tải lại dữ liệu`; không retry mutation mù |
| network / 5xx | Giữ input; retry read; publish timeout thì đọc trạng thái trước |

Thay filter/tuần dùng request sequence hoặc abort để response cũ không ghi đè mới. Khi có dirty form, đổi lịch/route phải xác nhận bỏ thay đổi. Mobile dùng list theo ngày và panel phía dưới; keyboard có nút thêm/sửa thay kéo thả; dialog focus/escape đúng, lỗi aria-live, màu kèm nhãn.

## 6. Contract và file scope

BE plan mục 6 là nguồn đề xuất duy nhất cho endpoint. Checkpoint chốt JSON fixtures cho detail, entries, review, policies, calendar, phòng chức năng, mapping môn, lịch bận và permissions; chưa được coi là approved chỉ vì HTML có nút.

Tạo mới:

- `FE/src/types/timetable.ts`: requests/responses theo fixture, không lặp transport wrapper.
- `FE/src/services/timetableApi.ts`, `teacherLoadApi.ts`, cùng `.spec.ts`: endpoint, query, body, version/idempotency.
- `FE/src/types/functionalRoom.ts`, `teacherUnavailability.ts`; `FE/src/services/functionalRoomApi.ts`, `teacherUnavailabilityApi.ts`, `subjectFunctionalRoomApi.ts` cùng specs theo contract mới.
- `FE/src/views/functional-room/FunctionalRoomListView.vue`, `FE/src/components/functional-room/FunctionalRoomDialog.vue` và spec/story.
- `FE/src/views/timetable/TeacherUnavailabilityView.vue`, `FE/src/components/timetable/TeacherUnavailabilityDialog.vue` và spec/story.
- Mở rộng `FE/src/views/academic/SubjectListView.vue`, `FE/src/components/academic/SubjectDialog.vue` hoặc component con `SubjectFunctionalRoomPanel.vue`, tests/story tương ứng; giữ nguyên CRUD fields/contract môn v2, mapping gọi service v3 riêng.
- `FE/src/fixtures/timetableFixture.ts`: deterministic review states, không chứa dữ liệu thật.
- `FE/src/views/timetable/TimetableListView.vue`, `TimetableWorkspaceView.vue`, `TimetableSettingsView.vue`, `MyTimetableView.vue` và view specs.
- `FE/src/components/timetable/TimetableWeekGrid.vue`, `TimetableEntryDialog.vue`, `TimetableConflictPanel.vue`, `TeacherLoadPanel.vue`, `TimetablePublishDialog.vue`, `TimetableCalendarForm.vue`,  `TeacherLoadPolicyForm.vue`, `TeacherLoadEligibilityForm.vue` cùng spec/story phù hợp.

Sửa `FE/src/router/index.ts`, route specs và `FE/src/views/shell/AuthenticatedV2ShellView.vue` cùng tests để thêm navigation theo D02. View sở hữu context/service calls/loading/form orchestration; component props/emits typed, không gọi HTTP. Không thêm global store/calendar dependency khi grid hiện tại có thể dựng bằng Vue/PrimeVue.

## 7. Test, delivery và acceptance

- Service: page zero-based + ResultPaginationDTO, filter tuần/lớp/GV/phòng chức năng/buổi/tiết, đúng payload và version, giữ key khi retry cùng intent.
- Components: thêm/sửa/xóa draft; trùng hiển thị đủ nhãn; publish khóa khi thiếu policy hoặc tổng lỗi toàn lịch >0 dù filter hiện tại sạch; không tự tính định mức; published read-only.
- Components bổ sung: grid đủ Sáng/Chiều × 4; phòng thường không xuất hiện; room picker theo môn; mapping reload; form lịch bận 1–4 tiết, khoảng ngày, sửa/rút và self scope.
- Views: list/create → API → route thật; đổi tuần không nhận stale response; save/reload; conflict điều hướng đúng entry; 401/403/404/409/422; dirty navigation; teacher self-registration scope và settings guard; parameterized ADMIN/ACADEMIC_OFFICE ngang quyền tất cả action.
- Storybook: loading, empty, draft, overlap, policy missing, eligibility missing, load warning, ready, published, archived, stale-version, forbidden, network-error, mobile, busy-registration/pending/approved/rejected/withdrawn, approval-published-conflict, busy-conflict, functional-room-list, subject-room-mapping.
- Validation trong FE: `npm run lint`, `npm run test`, `npm run test:coverage`, `npm run build`, `npm run build-storybook`; `git diff --check` từ root.
- Browser/live: ADMIN/giáo vụ tạo lịch thật, thêm tiết trùng GV/lớp/phòng, lưu nháp rồi sửa, kiểm tra policy và publish; reload; hai tab gây 409; revise giữ lịch cũ theo ngày; teacher xem lịch và đăng ký lịch bận mình, không sửa/duyệt người khác; Giáo vụ/Admin duyệt hoặc từ chối; pending không chặn, approved chặn; ADMIN/ACADEMIC_OFFICE cùng CRUD phòng/mapping/settings; Student không có mục mới. Không lấy jsdom/HTML làm evidence live.

FE dựng fixture sau 075.0 được duyệt, song song BE 075.1–075.3; phải tích hợp API thật và production route trong cùng plan. Acceptance không đạt nếu chỉ có review component. Hiện implementation/quality/browser-live đều **NOT RUN**; wireframe review **PENDING**. Dev Note phải cập nhật riêng từng gate, không gộp thành PASS.
