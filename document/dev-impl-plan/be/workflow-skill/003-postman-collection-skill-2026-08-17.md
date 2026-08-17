# Developer Plan: Postman Collection Skill

## 1. Mục tiêu

- Tạo skill project mới để hướng dẫn agent tạo hoặc cập nhật Postman collection khi người dùng yêu cầu trực tiếp.
- Skill giúp agent dựng collection dựa trên Dev Note, context hiện có hoặc thay đổi trong codebase.

## 2. Requirement liên quan

- Yêu cầu trực tiếp:
  - Dùng `skill-creator`.
  - Tạo skill yêu cầu agent create/update Postman collection.
  - Skill chỉ dùng khi nhận request từ user.
  - Agent tạo collection mới hoặc update collection có sẵn dựa trên Dev Note, context information hoặc changed codebase.
  - Không cần make report.
- Quy tắc project:
  - Thay đổi skill/workflow cần Developer Plan và approval trước khi sửa.

## 3. Phạm vi

### In-scope

- Tạo skill mới `.agents/skills/postman-collection/`.
- Viết `SKILL.md` ngắn gọn, project-specific, gồm:
  - Trigger rõ ràng: chỉ dùng khi user yêu cầu Postman collection.
  - Cách tìm Postman collection hiện có.
  - Cách suy luận endpoint/request/response từ controller, DTO, `application.properties`, Dev Note và Developer Plan.
  - Cách tạo hoặc cập nhật file JSON Postman Collection v2.1.
  - Quy tắc không tự ý gọi API thật, không đưa secret từ `.env` vào collection.
  - Không yêu cầu report riêng sau khi update collection.
- Tạo `agents/openai.yaml` bằng script của `skill-creator`.
- Validate skill bằng `quick_validate.py`.

### Out-of-scope

- Không tạo Postman collection ngay trong task này, trừ khi user yêu cầu riêng.
- Không thay đổi backend source code.
- Không thay đổi API behavior.
- Không tự động bắt buộc Postman collection sau mọi backend task.

## 4. Kiến trúc hiện tại

- Skill project nằm trong `.agents/skills/`.
- Project đã có Dev Note trong `document/dev-note/`.
- Backend API có thể được suy luận từ Spring controllers và DTOs trong `BE/BaiTap-RS/src/main/java`.
- Chưa có skill Postman collection.

## 5. Phương án triển khai

- Dùng `skill-creator/scripts/init_skill.py` để khởi tạo `postman-collection`.
- Không thêm references/scripts nếu chưa cần; hướng dẫn chính nằm trong `SKILL.md`.
- Nội dung skill sẽ ưu tiên:
  - Preserve existing collection structure and IDs where possible.
  - Add/update requests by endpoint identity: method + path.
  - Use `{{baseUrl}}` variable instead of hard-coded host.
  - Use `{{accessToken}}` for bearer auth examples.
  - Generate sample JSON bodies from request DTO fields.
  - Keep generated examples safe and non-secret.
- Nếu có collection hiện có:
  - Read and update it.
  - Preserve folders, variables, scripts, descriptions unless directly affected.
- Nếu chưa có collection:
  - Create a v2.1 collection under a sensible project docs path, proposed default:

```text
document/postman/Java-CoBan.postman_collection.json
```

## 6. Phạm vi file dự kiến

### Tạo mới

- `.agents/skills/postman-collection/SKILL.md`
- `.agents/skills/postman-collection/agents/openai.yaml`

### Chỉnh sửa

- Không chỉnh workflow bắt buộc hiện có.
- Có thể cập nhật Dev Note cho task này nếu cần theo `dev-note`.

## 7. API / Database / Integration

- Không ảnh hưởng API runtime.
- Không ảnh hưởng database.
- Skill output tương lai là Postman collection JSON.

## 8. Test và validation

- Validate skill:

```text
python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/postman-collection
```

- Kiểm tra file sinh ra:

```text
find .agents/skills/postman-collection -maxdepth 3 -type f
```

## 9. Rủi ro

- Skill trigger quá rộng làm agent tự update Postman khi không được yêu cầu.
  - Giảm thiểu: description nêu rõ chỉ dùng khi user yêu cầu trực tiếp.
- Collection JSON bị phá format/metadata.
  - Giảm thiểu: preserve existing fields và chỉ update endpoint liên quan.
- Secret bị lộ vào collection.
  - Giảm thiểu: dùng variables, không copy `.env` secret values.

## 10. Output dự kiến

- Có skill `postman-collection` hợp lệ.
- Skill chỉ dùng khi user yêu cầu tạo/cập nhật Postman collection.
- Skill hướng dẫn agent dùng Dev Note/context/code changes để tạo/update collection.

## 11. Approval status

- Trạng thái: Approved.
- Approved by user via agent on 2026-08-17.
