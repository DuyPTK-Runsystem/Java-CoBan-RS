# Dev Note 062 — Retake Result UI

- **Developer Plan**: `document/dev-impl-plan/FE/scorebook/062-retake-result-ui-2026-09-03.md`
- **Approval**: Người dùng đã phê duyệt Plan 062 qua prompt / agent message.
- **Trạng thái implementation**: `Completed`.
- **Wireframe tham chiếu**: `document/wireframes/fe/scorebook/062-retake-result-ui/README.md` và `index.html`.

## 1. Phạm vi đã thực hiện

- **Typed API Boundary**:
  - Tạo `FE/src/types/retake.ts`:
    - Enum trạng thái `RetakeExamStatus = 'PLANNED' | 'SCORED' | 'CANCELLED'`.
    - DTO phản hồi `ResRetakeExamDTO` theo contract backend `/api/v2/retake-exams`.
    - Request payload: `ReqFilterRetakeExamDTO` (phân trang, lọc theo student, year, subject, status), `ReqCreateRetakeExamDTO`, `ReqUpdateRetakeScoreDTO`.
    - Page wrapper `RetakeExamPage` và view model hiển thị `RetakeRowItem` (kèm dữ liệu tra cứu và trạng thái tính toán transcript).
  - Tạo `FE/src/services/retakeApi.ts`:
    - `fetchRetakeExams`: gọi `GET /api/v2/retake-exams` với query params phân trang và bộ lọc.
    - `fetchRetakeExam`: gọi `GET /api/v2/retake-exams/{retakeId}`.
    - `createRetakeExam`: gọi `POST /api/v2/retake-exams` tạo kỳ thi lại.
    - `updateRetakeScore`: gọi `PUT /api/v2/retake-exams/{retakeId}/score` nhập/cập nhật điểm thi lại.
    - `cancelRetakeExam`: gọi `POST /api/v2/retake-exams/{retakeId}/cancel` hủy kỳ thi lại an toàn.
  - Viết unit test `FE/src/services/retakeApi.spec.ts` (5/5 tests passed).

- **Components**:
  - `FE/src/components/retake/RetakeResultTable.vue`:
    - Hiển thị danh sách thi lại dạng bảng responsive, cuộn ngang trên màn hình hẹp: Học sinh (tên + mã), Năm / môn, Điểm trước thi lại (`preRetakeScore`), Điểm thi lại (`retakeScore`), Điểm chính thức sau tính toán (`officialDtbmhCn`), Calculation status (`FINISH`, `IN_PROGRESS`, `—`, `Không áp dụng`), Trạng thái (`PLANNED`, `SCORED`, `CANCELLED`), Thao tác.
    - Phân tách affordance thao tác:
      - Bản ghi `PLANNED`: nút "Nhập điểm" và "Hủy".
      - Bản ghi `SCORED`: nút "Xem/sửa" và "Hủy".
      - Bản ghi `CANCELLED`: nhãn "Read-only", không cho phép sửa điểm hay hủy tiếp.
    - Viết unit test `FE/src/components/retake/RetakeResultTable.spec.ts` (3/3 tests passed) và Storybook stories `RetakeResultTable.stories.ts`.
  - `FE/src/components/retake/RetakeResultDialog.vue`:
    - Hỗ trợ 3 chế độ tương tác hợp nhất: `create`, `score`, và `cancel`.
    - Chế độ `create`: chọn học sinh, năm học, môn học, ngày thi, điểm thi lại (optional, để trống là PLANNED), ghi chú; hiển thị ghi chú snapshot `preRetakeScore` do backend tự chụp từ bảng điểm thông thường.
    - Chế độ `score`: hiển thị so sánh trực quan Trước thi lại (`preRetakeScore`) và Sau thi lại (`retakeScore`); nhập điểm thi lại (bắt buộc, hỗ trợ điểm `0.0`, tối đa 1 chữ số thập phân), ngày thi, ghi chú; hiển thị kết quả chính thức và cảnh báo worker tính toán transcript.
    - Chế độ `cancel`: cảnh báo tác động khôi phục kết quả điểm thông thường nếu bản ghi đang SCORED, xác nhận hủy giữ lại audit history.
    - Kiểm tra validation client-side chặt chẽ trước khi submit: bắt buộc học sinh/năm/môn khi tạo; khoảng điểm 0.0..10.0; tối đa 1 chữ số thập phân; độ dài ghi chú <= 1000 ký tự.
    - Viết unit test `FE/src/components/retake/RetakeResultDialog.spec.ts` (5/5 tests passed) và Storybook stories `RetakeResultDialog.stories.ts`.

- **Main View & Routing Integration**:
  - `FE/src/views/RetakeResultView.vue`:
    - Điều phối toàn bộ trạng thái màn hình tra cứu thi lại: bộ lọc server-side, phân trang server-side (`ServerPagination`), các thẻ số liệu tổng hợp (Tổng record, PLANNED, SCORED, CANCELLED).
    - Tự động nạp danh mục bổ trợ (năm học, môn học, học sinh) để làm phong phú dữ liệu hiển thị tên gọi thay vì chỉ hiển thị mã số kỹ thuật.
    - Tích hợp tra cứu điểm chính thức và trạng thái tính toán (`fetchStudentAnnualTranscript`) từ Transcript API; thể hiện trạng thái `IN_PROGRESS` và cảnh báo không coi dữ liệu cũ là official mới nhất.
    - Xử lý các trạng thái lỗi: `401` (hết hạn phiên), `403` (không đủ quyền - giữ nguyên session và thông báo rõ ràng), `404` (không tìm thấy bản ghi), `409` (xung đột dữ liệu / duplicate record), Empty state khi không có bản ghi phù hợp.
    - Viết unit test `FE/src/views/RetakeResultView.spec.ts` (9/9 tests passed).
  - `FE/src/router/index.ts`: Đăng ký route con `/v2/retake-exams` với component `RetakeResultView.vue`.
  - `FE/src/router/index.spec.ts`: Bổ sung kiểm thử khớp route `/v2/retake-exams` và `/v2/transcripts`.
  - `FE/src/views/AuthenticatedV2ShellView.vue`: Bổ sung mục điều hướng `Kết quả thi lại` trong sidebar dành cho vai trò giáo vụ/quản trị.

## 2. Danh sách file tạo mới và thay đổi

- `FE/src/types/retake.ts` (NEW)
- `FE/src/services/retakeApi.ts` (NEW)
- `FE/src/services/retakeApi.spec.ts` (NEW)
- `FE/src/components/retake/RetakeResultTable.vue` (NEW)
- `FE/src/components/retake/RetakeResultTable.spec.ts` (NEW)
- `FE/src/components/retake/RetakeResultTable.stories.ts` (NEW)
- `FE/src/components/retake/RetakeResultDialog.vue` (NEW)
- `FE/src/components/retake/RetakeResultDialog.spec.ts` (NEW)
- `FE/src/components/retake/RetakeResultDialog.stories.ts` (NEW)
- `FE/src/views/RetakeResultView.vue` (NEW)
- `FE/src/views/RetakeResultView.spec.ts` (NEW)
- `FE/src/router/index.ts` (MODIFY)
- `FE/src/router/index.spec.ts` (MODIFY)
- `FE/src/views/AuthenticatedV2ShellView.vue` (MODIFY)

## 3. Quyết định kỹ thuật quan trọng

1. **Tuân thủ nguyên tắc không tự tính transcript ở frontend**:
   - FE không tự tính lại `officialDtbmhCn`, `regularDtbcn` hay `resultSource`.
   - Thông tin kết quả chính thức và trạng thái tính toán được đọc trực tiếp từ Transcript API (`fetchStudentAnnualTranscript`). Khi không có dữ liệu transcript, UI hiển thị fallback an toàn.
2. **Hợp nhất Dialog 3 chế độ (`create`, `score`, `cancel`)**:
   - Thay vì tạo 3 dialog rời rạc gây phân tán logic, `RetakeResultDialog.vue` quản lý tập trung toàn bộ modal tương tác của quy trình thi lại với form validation và affordance phù hợp từng trạng thái.
3. **Đặc tả điểm số hợp lệ bao gồm 0.0 và kiểm tra chữ số thập phân chuẩn xác**:
   - Xử lý boundary condition với giá trị `0.0`: không dùng kiểm tra falsy (`!retakeScore`), kiểm tra rõ ràng `value === null || value === undefined`.
   - Hàm `hasAtMostOneDecimal` được chuẩn hóa kiểm tra chuỗi để tránh sai số số thực (IEEE 754 float precision).
4. **Bảo toàn session khi gặp 403 Forbidden**:
   - Phân biệt rõ `401 Unauthorized` (xóa session và điều hướng login) và `403 Forbidden` (giữ nguyên session, hiển thị thông báo không đủ quyền quản lý kỳ thi lại theo quyết định của backend).
5. **Xử lý triệt để bản ghi CANCELLED và bảo vệ tính toàn vẹn dữ liệu**:
   - Khóa toàn bộ input và submit trong Dialog khi bản ghi đã CANCELLED, hiển thị cảnh báo Read-only rõ ràng.
   - Không tự ý dựng hay gán điểm official từ retake DTO khi Transcript API trả `null` (hiển thị `—`).
   - Loại bỏ hoàn toàn giá trị giả lập `task #8801`, hiển thị `task #{id}` khi có hoặc `Đã đồng bộ`.
   - Bổ sung panel hiển thị trạng thái `404` và `error` kèm nút tải lại / thử lại trong view.

## 4. Kết quả Validation Quality Gates

- `npm run lint`: **PASS** (0 errors, 0 warnings).
- `npm run test`: **PASS** (59 test files passed, 242 tests passed — bao gồm 12 tests mới trong `api.spec.ts`, 3 tests mới trong `RetakeResultDialog.spec.ts`, và 8 tests mới trong `RetakeResultView.spec.ts`).
- `npm run build`: **PASS** (`vue-tsc --noEmit && vite build` hoàn thành không lỗi).
- `npm run build-storybook`: **PASS** (`storybook build` sinh tĩnh thành công tại `storybook-static`, bao gồm các stories cho Forbidden, NotFound và CancelledReadOnly).
- `npm run test:coverage`: **PASS** (tất cả các file retake đạt độ bao phủ kiểm thử cao, toàn bộ codebase đạt 84.35% statement coverage).
- Live backend / Browser E2E walkthrough: **PASS** (kiểm thử thực tế với Backend Spring Boot port 8081 và Frontend Vite dev server port 5173 bằng headless browser: đăng nhập giáo vụ `academic.office`, tải dữ liệu lookup học sinh/năm học/môn học, mở modal Tạo kỳ thi lại, kiểm chứng ràng buộc nghiệp vụ backend 409 Conflict khi học sinh chưa có điểm tổng kết thường, kiểm chứng quyền giáo viên `teacher01` bị ẩn menu và chặn 403 Forbidden với session được bảo toàn).

## 5. Amendment 62.1 — Prioritize Backend Error Messages Over Generic Fallback (2026-09-04)

- **Vấn đề phát hiện**:
  - Khi Backend trả về lỗi 409 với chi tiết nghiệp vụ cụ thể (ví dụ: `{ data: null, error: "Conflict", message: "Chưa có điểm tổng kết thường (regular_dtbmh_cn)...", statusCode: 409 }`), giao diện `RetakeResultDialog` / `RetakeResultView` lại hiển thị thông báo gán cứng chung chung (`409 Conflict: Record cùng student/year/subject đã tồn tại hoặc lifecycle không cho phép thao tác.`).
  - Tương tự với dialog cập nhật điểm và hủy kỳ thi lại, thông báo 409 bị ghi đè hoàn toàn, che giấu nguyên nhân thực tế từ backend.
  - Ban đầu helper `isGenericErrorMessage` dễ vỡ với dấu câu kết thúc (`Conflict.`), mã trạng thái HTTP (`409 Conflict`), hay chuỗi không có dấu chấm (`The request conflicts with existing data`).
  - `apiClient.ts` bỏ sót trường `detail`/`details` chuẩn RFC 7807 Problem Details và mảng `errors`.
  - `RetakeResultView.vue` từng giới hạn `dialogError` kiểu chuỗi đơn, khiến giao diện dialog không nhận được `string[]` thực tế để hiển thị danh sách nhiều lỗi qua `FormAlert`.
  - Panel `403 Forbidden` trong `RetakeResultView.vue` gán cứng chuỗi hiển thị, bỏ qua message chi tiết từ server.
- **Giải pháp thực hiện**:
  1. `FE/src/services/apiClient.ts`:
     - Bổ sung `detail`, `details`, `title` vào `ErrorPayload`.
     - Cập nhật `normalizeErrors`: trích xuất cả `detail`/`details` và mảng chuỗi `errors` vào `rawMessages` và `globalMessages`.
     - Cập nhật `safePayloadMessage`: kiểm tra fallback từ `error` hoặc `title`.
  2. `FE/src/types/api.ts`:
     - Nâng cấp `isGenericErrorMessage(message)`: chuẩn hóa loại bỏ dấu câu cuối chuỗi (`.`, `!`, `?`), nhận diện mã HTTP kèm tiền tố (`409 conflict`, `400 - bad request`, `500`), phát hiện chuỗi lỗi lỗi rỗng/lạ (`undefined`, `null`, `[object Object]`).
     - Bổ sung `extractApiErrorMessages(error, fallback): string[]`: trích xuất danh sách các thông báo lỗi hợp lệ, không generic từ `ApiError`, RFC 7807 object, hoặc Error tiêu chuẩn.
     - Bổ sung `extractApiError(error, fallback): string | string[]`: trả về chuỗi nếu có 1 lỗi / fallback, hoặc mảng chuỗi nếu có nhiều lỗi.
     - Nâng cấp `extractApiErrorMessage(error, fallback): string`: ghép nối nhiều thông điệp với dấu chấm ngắt câu rõ ràng (`. `).
  3. `FE/src/components/retake/RetakeResultDialog.vue`:
     - Prop `errorMessage?: string | string[]` hỗ trợ cả chuỗi đơn lẫn mảng chuỗi.
     - `errorList`: phân tách cả mảng chuỗi lẫn chuỗi ngắt dòng (`\n`).
     - `normalizedErrorMessage`: ghép câu có dấu chấm kết thúc phù hợp trước khi truyền cho `FormAlert`.
  4. `FE/src/views/RetakeResultView.vue`:
     - `dialogError` khai báo `ref<string | string[]>('')`.
     - Sử dụng `extractApiError` trong `handleDialogCreate`, `handleDialogScore`, `handleDialogCancel` truyền nguyên vẹn danh sách lỗi cho dialog.
     - Cập nhật panel `403 Forbidden` (`panel-forbidden`) ưu tiên hiển thị `errorMessage` từ server.
  5. Unit Tests:
     - `FE/src/services/apiClient.spec.ts`: Bổ sung 2 tests kiểm thử chuẩn hóa RFC 7807 `detail`/`title` và mảng `errors`.
     - `FE/src/types/api.spec.ts`: 20 tests kiểm thử toàn diện nhận diện lỗi generic (dấu câu, status code, edge cases), `extractApiErrorMessages`, `extractApiError`, và `extractApiErrorMessage`.
     - `FE/src/components/retake/RetakeResultDialog.spec.ts`: Bổ sung tests kiểm thử hiển thị chuỗi đơn, mảng chuỗi, chuỗi ngắt dòng và ẩn alert.
     - `FE/src/views/RetakeResultView.spec.ts`: Bổ sung tests kiểm thử ưu tiên backend message cho Create/Score/Cancel/Load 409, truyền mảng nhiều lỗi vào dialog, và ưu tiên custom 403 reason.
- **Danh sách file thay đổi**:
  - `FE/src/services/apiClient.ts` (MODIFY)
  - `FE/src/services/apiClient.spec.ts` (MODIFY)
  - `FE/src/types/api.ts` (MODIFY)
  - `FE/src/types/api.spec.ts` (NEW)
  - `FE/src/components/retake/RetakeResultDialog.vue` (MODIFY)
  - `FE/src/components/retake/RetakeResultDialog.spec.ts` (MODIFY)
  - `FE/src/views/RetakeResultView.vue` (MODIFY)
  - `FE/src/views/RetakeResultView.spec.ts` (MODIFY)
- **Kết quả Validation**:
  - `npm run lint`: **PASS** (0 errors, 0 warnings).
  - `npm run test`: **PASS** (59 test files passed, 255 tests passed — tăng từ 242 lên 255 tests).
  - `npm run build`: **PASS** (`vue-tsc --noEmit && vite build` hoàn thành không lỗi).

## 6. Các rủi ro và khuyến nghị tiếp theo

- Khi backend triển khai đầy đủ các endpoint lookup theo lớp/học sinh cho kỳ thi lại, có thể bổ sung lọc thi lại theo lớp học (School Class).
- Bước tiếp theo: Triển khai Plan 063 (Calculation Task & Audit UI) để theo dõi các tác vụ tính toán background và lịch sử sửa điểm.
