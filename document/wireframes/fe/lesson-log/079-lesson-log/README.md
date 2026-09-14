# Wireframe 079 — Sổ đầu bài

- Application-document version: **v3**; cập nhật review **2026-09-14**.
- **DESIGN CONTRACT APPROVED / IMPLEMENTATION PENDING**: D01–D10 đã được duyệt; prototype đã cập nhật theo contract, còn implementation BE/FE chưa được duyệt.
- [Mở prototype](index.html) · [Plan BE](../../../../dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md) · [Plan FE](../../../../dev-impl-plan/fe/lesson-log/079-lesson-log-ui-2026-09-14.md) · [Dev Note](../../../../dev-note/summary/079-lesson-log-plan-wireframe-2026-09-14.md).

## Mở và phạm vi minh họa

Mở `index.html` trực tiếp trong trình duyệt, không cần mạng, build hoặc backend. Prototype dùng dữ liệu trong bộ nhớ; reload/đổi kịch bản đặt lại dữ liệu. Đổi vai trò giữ dữ liệu để thử chuỗi Giáo vụ điều chỉnh → GVCN ký lại.

Fixture giới hạn một lớp 6A1, một tuần 14–20/09/2026, sáu tiết đã phân công, sĩ số mẫu 40; không phải TKB đầy đủ của trường. Ma trận có bảy ngày, tám hàng tiết (2 buổi × 4); thứ Bảy/Chủ nhật là ngày nghỉ trong fixture. Đồng hồ minh họa hiển thị ở thanh review, không dùng giờ thật máy để quyết định hết hạn.

Prototype minh họa contract D01–D10 đã duyệt: hạn 48 giờ hoặc hết tuần; timezone Asia/Ho_Chi_Minh; bốn mã A/B/C/D; quyền GVBM/GVCN/quản lý; ký lại sau điều chỉnh. Đây vẫn là prototype độc lập, chưa phải implementation policy/API.

## Kịch bản review

| Cách thử | Kết quả mong đợi |
|---|---|
| GVBM → Đang ghi sổ → mở nháp Tiết 1 ngày 15/09 | Lưu nháp thiếu title/grade/count được; nộp thiếu field bị chặn |
| GVBM → mở Tiết 2 chưa ghi → điền đủ và nộp | Chỉ tạo cho occurrence có trong TKB; mở lại có nút Lưu thay đổi, không hạ SUBMITTED về nháp |
| GVBM → Nháp / tiết thiếu đã quá hạn | Chỉ xem; nháp cũng khóa; không chọn giáo viên dạy thay |
| Cùng kịch bản → Giáo vụ/Admin → nháp hoặc tiết thiếu | Hoàn thiện/ghi bổ sung yêu cầu đủ field và lý do; kết quả AMENDED |
| GVBM → Nguồn TKB đã lưu trữ | Vẫn mở nguồn lịch sử và thấy deadline của tiết cũ |
| GVBM → Xung đột 409 → mở nháp, nhập rồi lưu | Dialog/input được giữ; Xem bản mới nhất không ghi đè; Dùng bản mới là lựa chọn riêng |
| GVBM → Hết hạn 422 lúc đang nhập → nhập rồi lưu | Giữ input, khóa thao tác ghi; có thông báo liên hệ Giáo vụ |
| GVCN → Đang ghi sổ → lọc chỉ Đã nộp | Không ký được chỉ vì đã ẩn tiết thiếu; không có quyền review/amend từng tiết |
| GVCN → Tuần đủ điều kiện ký | Nhận xét và ký toàn bộ tập tiết, lưu snapshot |
| Giáo vụ → Tuần đã ký → mở một tiết, điều chỉnh kèm lý do | Tuần chuyển Cần ký lại; xác nhận cũ còn trong lịch sử |
| Giữ kịch bản trên → đổi GVCN → Ký lại tuần | Bắt buộc lý do ký lại; lịch sử có SIGN_WEEK, INVALIDATE_WEEK và lần ký mới |
| Giáo vụ/Admin → Quy định ghi sổ → chọn Đến hết tuần | Ô số giờ bị khóa; deadline mô tả 00:00 thứ Hai tiếp theo exclusive |
| Nhập ngày hiệu lực tương lai, lý do, lưu policy | Tăng phiên bản; deadline của tiết mẫu cũ không đổi |
| Empty / 403 / loading / lỗi kết nối | Hiển thị fallback riêng, không mở form ghi |

Audit dùng snapshot text được escape, không render input thành HTML. Các nút ghi/duyệt trong prototype chỉ thay dữ liệu mẫu, không gọi API và không tạo chữ ký số.

## Giới hạn prototype và validation

- Chưa mô phỏng server transaction/DB unique/Flyway, lookup nhiều lớp, pagination audit, semester đóng, enrollment lịch sử thật, nhiều policy theo timeline hoặc mạng thật.
- Luồng create draft → submit hiện mô phỏng thành công liên tiếp; ca API create thành công/submit lỗi được yêu cầu trong test plan FE, chưa có fault riêng trong prototype.
- Guard publish/calendar và chống duplicate qua revision là thiết kế BE, không được chứng minh bằng HTML. Không coi một kịch bản “nguồn lưu trữ” là integration test TKB.
- Không có in/PDF, duyệt hàng loạt hoặc ô chọn dạy thay ngoài assignment.
- Smoke tái chạy: `node document/wireframes/fe/lesson-log/079-lesson-log/smoke.cjs` từ repo root. Script dùng JSDOM có sẵn trong FE, polyfill dialog để kiểm tra logic, không giả làm browser.
- Lần cập nhật này: **JS syntax PASS; JSDOM 12/12 PASS**. Browser thật/pixel screenshot **NOT RUN**: Chrome báo đang tắt khi tạo tab. API/live BE/FE và các quality gates sản phẩm **NOT RUN**.
