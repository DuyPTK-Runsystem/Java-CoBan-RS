# Dev Note 061 — Transcript Viewer UI

- Developer Plan: `document/dev-impl-plan/fe/scorebook/061-transcript-viewer-ui-2026-09-03.md`
- Approval: Người dùng đã phê duyệt Plan 061 qua agent message.
- Trạng thái implementation: Completed.

## Phạm vi đã thực hiện

- **Typed API Layer**:
  - Thêm types tại `FE/src/types/transcript.ts` định nghĩa đầy đủ các DTO: `ResStudentTermTranscriptDTO`, `ResStudentAnnualTranscriptDTO`, `ResTranscriptCalculationStatusDTO`, `ResTermSubjectResultDTO`, `ResAssessmentColumnDTO`, `ResAnnualSubjectResultDTO`, `ResRetakeDetailDTO`.
  - Thêm client API tại `FE/src/services/transcriptApi.ts` hỗ trợ các endpoint:
    - Học sinh tự tra cứu: `/api/v2/transcripts/students/me/semesters/{semesterId}`, `/api/v2/transcripts/students/me/academic-years/{academicYearId}`, kèm endpoints kiểm tra status `/status`.
    - Staff/Teacher tra cứu học sinh theo quyền: `/api/v2/transcripts/students/{studentId}/...`.
  - Viết unit test `FE/src/services/transcriptApi.spec.ts` (3/3 tests passed).

- **Dynamic Matrix Table Components**:
  - `FE/src/components/TranscriptTermTable.vue`:
    - Header 2 tầng: Tầng 1 gồm `STT`, `Môn học`, `KTTX`, `KTĐK`, `KTCK`, `TBMHK`, `Ghi chú`. Tầng 2 đánh số thứ tự cột con.
    - Sinh cột con tự động: tính toán dựa trên số lượng cột thực tế lớn nhất của các môn học trong học kỳ (`max(KTTT/KTTX)`, `max(KTDK/KTĐK)`, `max(KTCK)`), không cố định cứng số lượng cột.
    - So khớp linh hoạt enum `AssessmentType`: hỗ trợ cả có dấu và không dấu (`'KTĐK' | 'KTDK'` và `'KTTT' | 'KTTX'`), chuẩn hóa case/whitespace, tương thích hoàn toàn với backend `@JsonValue`.
    - Ánh xạ điểm từng môn vào đúng số thứ tự cột con (`columnNo`), giữ rỗng các ô chưa có điểm ngay cả khi có khoảng cách (gap) giữa các số thứ tự cột điểm.
    - Hỗ trợ môn kỹ năng / rèn luyện (`SKILL`): merge toàn bộ cột đánh giá hiển thị Đạt / Chưa đạt.
    - Footer Summary: bảng tổng kết điểm trung bình học kỳ (`dtbhk`) và box thông tin số buổi vắng có phép / không phép.
    - Viết unit test `FE/src/components/TranscriptTermTable.spec.ts` (6/6 tests passed) và Storybook stories.
  - `FE/src/components/TranscriptAnnualTable.vue`:
    - Hiển thị bảng điểm cả năm: `STT`, `Môn học`, `ĐTB HK1`, `ĐTB HK2`, `ĐTB Cả năm ban đầu`, `Điểm thi lại`, `Điểm chính thức`, `Ghi chú thi lại & kết quả`.
    - Phân biệt rõ điểm thi lại bằng badge `RETAKE`, gạch ngang điểm cũ và hiển thị điểm mới, ghi rõ ngày thi lại.
    - Footer Summary: ĐTB cả năm ban đầu và ĐTB cả năm chính thức.
    - Viết unit test `FE/src/components/TranscriptAnnualTable.spec.ts` (3/3 tests passed) và Storybook stories.

- **Main View & Routing**:
  - `FE/src/views/TranscriptViewerView.vue`:
    - Context selectors: Chọn năm học và học kỳ (tự động load năm học/học kỳ đang ACTIVE).
    - 2 Tabs chuyển đổi mượt mà giữa "Bảng điểm Học kỳ" và "Bảng điểm Cả năm".
    - Tích hợp gọi `fetchStudentAttendanceHistory` từ `@/services/attendanceApi` để lấy `summary.excusedAbsenceCount` và `summary.unexcusedAbsenceCount` truyền vào `TranscriptTermTable`. Có xử lý fallback an toàn nếu attendance API gặp lỗi.
    - Xử lý trạng thái tính toán `calculationStatus`:
      - Khi `IN_PROGRESS`: hiển thị banner cảnh báo đang cập nhật kèm nút kiểm tra lại trạng thái.
      - Khi `FINISH`: hiển thị nhãn chính thức cùng phiên bản tính (`v0`, `v1`, `v2`...) và thời gian tính gần nhất.
    - Xử lý đầy đủ các lỗi: `401` (hết phiên), `403` (từ chối truy cập), `404` (không tìm thấy dữ liệu), Empty state.
    - Viết unit test `FE/src/views/TranscriptViewerView.spec.ts` (8/8 tests passed).
  - Đăng ký route `/v2/transcripts` trong `FE/src/router/index.ts`.
  - Thêm mục điều hướng `Bảng điểm` trong thanh sidebar của `FE/src/views/AuthenticatedV2ShellView.vue`.

## Files tạo mới và thay đổi

- `FE/src/types/transcript.ts` (NEW - cập nhật hỗ trợ 'KTTT' | 'KTTX' | 'KTDK' | 'KTĐK' | 'KTCK')
- `FE/src/services/transcriptApi.ts` (NEW)
- `FE/src/services/transcriptApi.spec.ts` (NEW)
- `FE/src/components/TranscriptTermTable.vue` (NEW)
- `FE/src/components/TranscriptTermTable.spec.ts` (NEW)
- `FE/src/components/TranscriptTermTable.stories.ts` (NEW)
- `FE/src/components/TranscriptAnnualTable.vue` (NEW)
- `FE/src/components/TranscriptAnnualTable.spec.ts` (NEW)
- `FE/src/components/TranscriptAnnualTable.stories.ts` (NEW)
- `FE/src/views/TranscriptViewerView.vue` (NEW)
- `FE/src/views/TranscriptViewerView.spec.ts` (NEW)
- `FE/src/router/index.ts` (MODIFY)
- `FE/src/views/AuthenticatedV2ShellView.vue` (MODIFY)
- `document/wireframes/fe/transcript/061-transcript-viewer-ui/index.html` (MODIFY)
- `document/wireframes/fe/transcript/061-transcript-viewer-ui/README.md` (MODIFY)
- `document/dev-impl-plan/fe/scorebook/061-transcript-viewer-ui-2026-09-03.md` (MODIFY)

## Sửa lỗi Dropdown Năm học (Student Access Fix)

- **Vấn đề**: Học sinh đăng nhập thấy dropdown Năm học bị `"empty"` và không chọn được gì do PrimeVue Select dùng `option-label="name"`, trong khi model `AcademicYear` chỉ có trường `code` (ví dụ `"2026-2027"`).
- **Khắc phục**:
  - Đã đổi `option-label="code"` trong `FE/src/views/TranscriptViewerView.vue`.
  - Cập nhật `selectStub` trong `FE/src/views/TranscriptViewerView.spec.ts` để hiển thị `option.code ?? option.name`.

## Hoàn thiện Plan 061 (Enrichment & Validation)

1. **Khớp enum AssessmentType và ánh xạ cột con**:
   - Mở rộng `AssessmentType` trong `FE/src/types/transcript.ts` bao gồm `'KTTT' | 'KTTX' | 'KTDK' | 'KTĐK' | 'KTCK'`.
   - Cập nhật hàm matching trong `TranscriptTermTable.vue` so khớp linh hoạt cả `'KTĐK' | 'KTDK'` và `'KTTT' | 'KTTX'`.
   - Ánh xạ chính xác theo `columnNo` thay vì chỉ số mảng (`colIndex - 1`), đảm bảo dữ liệu có khoảng trống cột (gap) không bị dồn lệch ô sai vị trí.
2. **Tích hợp chuyên cần học kỳ và xử lý phiên bản 0**:
   - `TranscriptViewerView.vue` gọi song song `fetchStudentAttendanceHistory` cùng `fetchMyTermTranscript`.
   - Truyền `excusedAbsences` và `unexcusedAbsences` vào `TranscriptTermTable.vue`.
   - Fallback graceful về `null` nếu API chuyên cần thất bại.
   - Sửa điều kiện hiển thị `currentVersion` hỗ trợ `v0` thay vì bị ẩn khi giá trị là 0.
3. **Cập nhật Unit Tests**:
   - `TranscriptTermTable.spec.ts`: 6/6 passed.
   - `TranscriptViewerView.spec.ts`: 8/8 passed.
   - Toàn bộ FE test suite: 54/54 test files passed, 188/188 tests passed.

## Kết quả Validation Quality Gates

- `npm run lint`: **PASS** (0 errors, 0 warnings).
- `npm run test`: **PASS** (54 test files passed, 188 tests passed).
- `npm run build`: **PASS** (`vue-tsc --noEmit && vite build`, dist build thành công không lỗi type/bundling).
- `npm run build-storybook`: **PASS** (`storybook build` thành công, output: `storybook-static`).
