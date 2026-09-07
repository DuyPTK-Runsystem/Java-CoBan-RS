# Developer Plan: Frontend Documentation Refactor

## Mục tiêu

Chuẩn hóa bộ rule và API guide v2 của FE sau review, giữ lại các quy tắc legacy
UI đang áp dụng và giảm duplication trong tài liệu.

## Requirement liên quan

- Application Documentation v2 là nguồn nghiệp vụ cho FE mới.
- FE hiện vẫn có Student profile UI/API v1 cần được bảo toàn.
- API enum và response shape phải bám implemented backend contract.
- Tài liệu phải có routing rõ ràng để agent đọc đúng phạm vi.

## Phạm vi

### In-scope

- Tạo rule riêng cho legacy Student UI/CRUD, gồm flow, list/form và Storybook.
- Thêm route từ `FE/AGENTS.override.md` tới rule legacy.
- Sửa wording account provisioning để phân biệt FE v2 scope với endpoint v3.
- Bỏ đánh số section toàn cục trong API guide v2.
- Để `07-enums-and-known-drift.md` làm registry canonical và thay các block enum
  lặp trong tài liệu domain bằng cross-reference.
- Cập nhật FE Plan/Dev Note summaries và tạo Dev Note cho thay đổi này.

### Out-of-scope

- Không sửa backend hoặc FE source code.
- Không thay đổi API contract, requirement, CR hoặc data model.
- Không chạy hoặc thay đổi FE runtime/build configuration.

## Phương án triển khai

1. Tạo `FE/agent-rules/05-legacy-student-ui.md` cho các quy tắc UI v1 bị mất
   khi tách rule.
2. Sửa routing và terminology trong rule hiện tại.
3. Chuẩn hóa heading của toàn bộ `document/application-doc/v2/frontend-api/`.
4. Giữ enum canonical tại file 07; domain docs chỉ mô tả hành vi đặc thù và link
   tới registry.
5. Chạy kiểm tra diff và link nội bộ.

## Validation dự kiến

- `git diff --check`.
- Kiểm tra tất cả link Markdown nội bộ trong các file bị ảnh hưởng.
- Kiểm tra không còn heading API domain dùng numbering toàn cục.

## Rủi ro và giảm thiểu

- Có thể bỏ sót rule legacy khi di chuyển: đối chiếu trực tiếp với
  `FE/AGENTS.override.md` trước refactor.
- Có thể tạo link sai sau khi thêm file: chạy link-target check.
- Tài liệu untracked phải được giữ cùng bộ khi commit để routing không trỏ tới
  file thiếu.

## Approval status

- Được người dùng phê duyệt qua tin nhắn ngày 2026-08-27 cho các đề xuất 1–4
  trong review tài liệu.
