# Wireframe V4-001 — Agent xếp thời khoá biểu

- [Mở HTML](index.html).
- [Developer Plan](../../../../dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md), [CR v4](../../../../application-doc/v4/change-request/CR-V4-001-spring-ai-timetable-agent.md).
- Static review artifact, dữ liệu minh hoạ. Nút disabled có chủ đích; không gọi model/API hoặc lưu TKB.
- Status DRAFT: chờ review UI và implementation approval. Visual browser review NOT RUN.
- [Amendment harness/tools/cache](../../../../application-doc/v4/agent-contract/harness-tools-and-prompt-cache.md): read tools là đề xuất backend, không đổi wireframe hiện tại. Cache miss hoặc review quá 300s không tự hủy phương án; validity do snapshot/proposal/approval policy riêng quyết định.

## Luồng review

1. Từ workspace draft chọn “Gợi ý thời khoá biểu”; chọn lớp/khoảng ngày, số tiết mỗi phân công, tiết giữ nguyên và ưu tiên.
2. Application kiểm tra input rồi gửi snapshot; hiển thị đang tạo gợi ý, không đổi lịch.
3. Preview theo tuần với 2 buổi × 4 tiết, diff và warnings. Dữ liệu mẫu chỉ điền một phần grid để review layout, không đại diện lịch đủ số tiết.
4. “Duyệt phương án” gắn đúng proposal; sau đó “Lưu bản nháp” yêu cầu application thực thi.
5. Server receipt thành công: mở lại draft và reload. Publish vẫn ở workflow hiện có.

## State checklist cho Storybook sau approval

| State | Thông tin và hành vi |
|---|---|
| idle / generating | Input hoặc tiến trình; chưa được duyệt/lưu |
| needs-input | Nêu phân công/slot/policy/demand thiếu, giữ input |
| invalid-schema / provider-timeout | Thông báo gợi ý chưa dùng được, không sửa TKB |
| conflicts / no-solution-found | Issue cụ thể; chặn duyệt/lưu; không gọi là vô nghiệm |
| ready / warnings | Grid + diff + warnings; duyệt khả dụng theo backend |
| approved / executing | Input đổi hủy duyệt; disable thao tác lặp khi đang lưu |
| stale / expired | Giữ input, yêu cầu tạo/kiểm tra phương án mới và duyệt lại |
| denied | Không đủ quyền theo backend, không suy luận từ role do model gửi |
| saved | Receipt server + target revision/version, tải lại lịch |
| committed-response-lost | Kiểm tra trạng thái hành động theo receipt/key, không tự gửi save mới |

Tiêu chí review: grid/diff dễ đọc, responsive, focus/keyboard, cảnh báo rõ, phân biệt duyệt với lưu, không hiển thị chi tiết tool/schema cho người dùng. HTML chưa chứng minh các tiêu chí runtime này.
