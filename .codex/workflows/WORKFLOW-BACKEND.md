# WORKFLOW.md

## 1. Mục đích
Quy định quy trình bắt buộc cho AI Agent khi tạo, sửa, refactor hoặc bổ sung mã nguồn.
Tài liệu:
- Rule: `.codex/AGENTS.md` v
- Docs: `document/application-doc/`
- Developer Plan: `document/dev-impl-plan`
- References: `.codex/workflows/references`
## 2. Nguyên tắc bắt buộc
1. Đọc rule, docs và code liên quan trước khi lập kế hoạch hoặc sửa code.
2. Không suy đoán requirement nếu thông tin đã tồn tại.
3. Không code khi approval gate hoặc Developer Plan bắt buộc chưa được phê duyệt.
4. Chỉ thay đổi trong scope đã được duyệt.
5. Không tự ý đổi kiến trúc, dependency, build, CI/CD hoặc refactor ngoài scope.
6. Không báo thành công khi validation bắt buộc vẫn thất bại.
## 3. Bước 1 — Hiểu yêu cầu
Trước khi hành động:
- đọc rule áp dụng trong `.claude/rules/`;
- đọc docs liên quan trong `.claude/docs/`;
- đọc implementation hiện tại và Developer Plan liên quan;
- xác định requirement, kiến trúc, flow, module, dependency và rủi ro.
Không chỉnh sửa mã nguồn ở bước này.
## 4. Bước 2 — Developer Plan
Nếu chưa có plan được phê duyệt:
1. Phân tích requirement và implementation hiện tại.
2. Tạo plan theo `references/DEVELOPER_PLAN.md`.
3. Trình bày và chờ người dùng phê duyệt rõ ràng.
Nếu đã có plan:
1. Đọc toàn bộ plan.
2. Đối chiếu với requirement và code hiện tại.
3. Xác nhận plan vẫn khả thi và đúng scope.
Nếu plan không còn phù hợp, đề xuất cập nhật và chờ phê duyệt lại.
Không tự ý đổi phương án kỹ thuật hoặc scope đã duyệt.
## 5. Bước 3 — Báo cáo trước khi code
Báo cáo ngắn gọn:
- cấu trúc và flow hiện tại;
- feature/module sẽ thực hiện;
- phạm vi và file/class/method dự kiến thay đổi;
- API/database/integration bị ảnh hưởng nếu có;
- output, rủi ro và validation dự kiến.
Dùng `references/REPORT_TEMPLATES.md` khi cần.
Chỉ code sau khi đáp ứng approval gate áp dụng.
## 6. Bước 4 — Thực hiện code
- Tuân thủ rule, Developer Plan và convention hiện có.
- Chỉ sửa file/khu vực thuộc scope.
- Ưu tiên tái sử dụng code, hạn chế duplication.
- Giữ code dễ đọc, bảo trì và test.
- Xử lý error, validation, security và performance phù hợp.
- Không thêm feature ngoài requirement.
Nếu plan sai hoặc thiếu: dừng phần bị ảnh hưởng, báo nguyên nhân/tác động,
đề xuất plan mới và chờ phê duyệt trước khi tiếp tục.
## 7. Bước 5 — Validation
Trước khi hoàn tất:
- đọc và tuân thủ `config/checkstyle/checkstyle.xml`;
- chạy Checkstyle;
- chạy PMD;
- chạy build;
- chạy test liên quan.
Sửa lỗi do thay đổi hiện tại gây ra rồi chạy lại validation.
Lỗi cũ ngoài scope phải được phân biệt và báo cáo; không tự ý refactor.
Chi tiết: `references/VALIDATION_AND_DEBUG.md`.

## 8. Bước 6 — Giới hạn debug
Tối đa **10 vòng**: code → validation → phân tích → sửa → chạy lại.
Theo dõi số vòng đã dùng.
Sau 10 vòng chưa giải quyết được, dừng và báo:
- lỗi còn lại và nguyên nhân đã xác định;
- phương án đã thử;
- thay đổi đã thực hiện;
- phần chưa hoàn thành/thông tin còn thiếu;
- bước tiếp theo đề xuất.
Không tiếp tục trial-and-error ngẫu nhiên hoặc che giấu thất bại.

## 9. Bước 7 — Báo cáo kết quả
Báo cáo cuối phải phản ánh đúng thực tế:
- requirement đã thực hiện;
- file thay đổi và mục đích;
- Checkstyle / PMD / build / test;
- sai lệch so với Developer Plan nếu có;
- lỗi, giới hạn hoặc bước tiếp theo nếu còn.
Dùng `references/REPORT_TEMPLATES.md`.

## 10. Thứ tự ưu tiên
1. Yêu cầu trực tiếp của người dùng trong task hiện tại.
2. Rule bắt buộc của project.
3. Developer Plan đã được phê duyệt.
4. Tài liệu requirement/kỹ thuật.
5. Workflow này.
6. Convention suy ra từ code hiện tại.
Không dùng mức thấp hơn để vi phạm mức cao hơn.

## 11. Completion checklist
- [ ] Đã đọc rule, docs và code liên quan.
- [ ] Developer Plan hợp lệ và được phê duyệt khi bắt buộc.
- [ ] Thay đổi đúng scope.
- [ ] Checkstyle, PMD, build và test liên quan đã được chạy.
- [ ] Debug không vượt quá 10 vòng.
- [ ] Lỗi còn lại được công khai rõ ràng.
- [ ] Báo cáo cuối khớp với validation thực tế.
