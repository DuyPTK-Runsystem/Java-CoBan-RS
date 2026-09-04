# WORKFLOW.md

## 1. Mục đích
Quy định quy trình bắt buộc cho AI Agent khi tạo, sửa, refactor hoặc bổ sung mã nguồn.
Tài liệu:
- Rule: `.codex/AGENTS.md`
- Docs: `document/application-doc/`
- Developer Plan: `document/dev-impl-plan/`
- References: `.codex/workflows/references/`
## 2. Nguyên tắc bắt buộc
1. Đọc rule, docs và code liên quan trước khi lập kế hoạch hoặc sửa code.
2. Không suy đoán requirement nếu thông tin đã tồn tại.
3. Không code khi Developer Plan chưa được người dùng phê duyệt bằng tin nhắn qua agent.
4. Chỉ thay đổi trong scope đã được duyệt.
5. Không tự ý đổi kiến trúc, dependency, build, CI/CD hoặc refactor ngoài scope.
6. Không báo thành công khi validation bắt buộc vẫn thất bại.
## 3. Bước 1 — Hiểu yêu cầu
Trước khi hành động:
- đọc rule áp dụng trong `.codex/AGENTS.md` và `AGENTS.override.md` nếu có;
- đọc docs liên quan trong `document/application-doc/`; chọn rõ `v1/` hoặc `v2/` trước khi đọc sâu;
- đọc implementation hiện tại và Developer Plan liên quan;
- xác định requirement, kiến trúc, flow, module, dependency và rủi ro.
Không chỉnh sửa mã nguồn ở bước này.
## 4. Bước 2 — Developer Plan
Nếu chưa có plan được phê duyệt:
1. Phân tích requirement và implementation hiện tại.
2. Tạo plan theo `.codex/workflows/references/DEVELOPER_PLAN.md`.
3. Trình bày và chờ người dùng phê duyệt rõ ràng bằng tin nhắn qua agent.
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
Dùng `.codex/workflows/references/REPORT_TEMPLATES.md` khi cần.
Chỉ code sau khi Developer Plan đã được người dùng phê duyệt bằng tin nhắn qua agent.
## 6. Bước 4 — Thực hiện code
- Tuân thủ rule, Developer Plan và convention hiện có.
- Chỉ sửa file/khu vực thuộc scope.
- Ưu tiên tái sử dụng code, hạn chế duplication.
- Giữ code dễ đọc, bảo trì và test.
- Xử lý error, validation, security và performance phù hợp.
- Không thêm feature ngoài requirement.
Nếu plan sai hoặc thiếu: dừng phần bị ảnh hưởng, báo nguyên nhân/tác động,
đề xuất plan mới và chờ người dùng phê duyệt bằng tin nhắn qua agent trước khi tiếp tục.
## 7. Bước 5 — Validation
Gọi skill `.agents/skills/backend-validation/SKILL.md` để thực hiện validation backend.
Skill này là nguồn duy nhất định nghĩa thứ tự lệnh, trạng thái kết quả và giới hạn debug.
Workflow chỉ tiếp nhận `Validation Result`, không lặp lại các lệnh hoặc quy tắc của skill.

Sau khi có `Validation Result`, tạo hoặc cập nhật Dev Note bằng
`.agents/skills/dev-note/SKILL.md`, ghi đúng kết quả thực tế.

## 8. Bước 6 — Báo cáo kết quả
Báo cáo cuối phải phản ánh đúng thực tế:
- requirement đã thực hiện;
- file thay đổi và mục đích;
- `Validation Result` từ `backend-validation`;
- Dev Note đã tạo hoặc cập nhật;
- sai lệch so với Developer Plan nếu có;
- lỗi, giới hạn hoặc bước tiếp theo nếu còn.
Dùng `.codex/workflows/references/REPORT_TEMPLATES.md`.

## 9. Thứ tự ưu tiên
1. Yêu cầu trực tiếp của người dùng trong task hiện tại.
2. Rule bắt buộc của project.
3. Developer Plan đã được phê duyệt.
4. Tài liệu requirement/kỹ thuật.
5. Workflow này.
6. Convention suy ra từ code hiện tại.
Không dùng mức thấp hơn để vi phạm mức cao hơn.

## 10. Completion checklist
- [ ] Đã đọc rule, docs và code liên quan.
- [ ] Developer Plan hợp lệ và đã được người dùng phê duyệt bằng tin nhắn qua agent.
- [ ] Thay đổi đúng scope.
- [ ] `backend-validation` đã trả về `Validation Result`.
- [ ] Dev Note đã ghi lại validation thực tế.
- [ ] Lỗi còn lại được công khai rõ ràng.
- [ ] Báo cáo cuối khớp với validation thực tế.
