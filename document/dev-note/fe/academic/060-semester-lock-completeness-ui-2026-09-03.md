# Dev Note 060: Semester Lock & Completeness UI

Final validation correction: lint PASS; full test PASS (46 files/148 tests); coverage PASS (84.35% statements); production build PASS; Storybook build PASS; browser visual QA and live SMTP smoke test NOT RUN.

## 1. Plan và approval

- Developer Plan: `document/dev-impl-plan/FE/academic/060-semester-lock-completeness-ui-2026-09-03.md`
- Approval: User approved Plan 060 via agent message before implementation.
- Scope: FE semester completeness, email notification actions, and semester lock/reopen UI.

## 2. Đã triển khai

- Hiển thị báo cáo completeness và chi tiết dữ liệu còn thiếu.
- Hiển thị lịch sử thông báo qua email với trạng thái tiếng Việt.
- Gửi email nhắc điểm và thử gửi lại email lỗi qua endpoint backend hiện có.
- Xác nhận trước mutation, chống gửi lặp khi đang xử lý, cập nhật lại dữ liệu sau thao tác.
- Giữ luồng khóa/mở lại học kỳ và lý do mở lại.
- Không hiển thị channel, `IN_APP`, hoặc thông tin checkpoint trên UI.
- Service map các field transport-only khỏi UI model; không lưu secret/password trong FE.

## 3. Files thay đổi

- `FE/src/types/academic.ts`
- `FE/src/services/academicApi.ts`
- `FE/src/services/academicApi.spec.ts`
- `FE/src/views/SemesterListView.vue`
- `FE/src/components/SemesterStatusDialog.vue`
- `FE/src/components/SemesterNotificationPanel.vue`
- `FE/src/components/SemesterNotificationPanel.stories.ts`

## 4. Contract và giới hạn

- FE dùng các endpoint đã có: completeness report, notifications list, dispatch, retry-failed, lock, reopen.
- Backend response notification vẫn chứa field kỹ thuật; FE nhận ở boundary nhưng không đưa vào UI model.
- Contract role/capability hiện chưa expose role trong login/account/JWT. FE không suy diễn role; authorization vẫn do backend quyết định và lỗi `403` được hiển thị theo transport contract.
- Email UI chỉ xác nhận hệ thống gửi thành công; không claim người nhận đã nhận email.

## 5. Validation thực tế

- `npm.cmd run test -- --run src/services/academicApi.spec.ts src/views/SemesterListView.spec.ts`: PASS, 9 tests.
- `npm.cmd run test`: PASS, 45 files / 146 tests.
- `npm.cmd run test:coverage`: PASS, 84.41% statements.
- `npm.cmd run build`: PASS.
- `npm.cmd run build-storybook`: PASS.
- `npm.cmd run lint`: FAIL do baseline ngoài write set: `FE/src/views/ScoreChangeRequestView.vue:16,18` unused `ApiError` và `AssessmentColumn`.

## 6. Remaining / next steps

- Main agent cần xử lý hoặc ghi nhận contract role/capability trước khi claim role-aware navigation.
- Cần live backend/SMTP smoke test để xác nhận request gửi email thành công; chưa chạy trong FE validation.
- Plan 060 được ghi nhận Completed sau khi chạy lại các FE gates; browser visual QA và live SMTP smoke test chưa chạy.
