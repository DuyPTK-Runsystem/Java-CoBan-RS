# Developer Plan: Dev Note Skill and Workflow Enforcement

## 1. Mục tiêu

- Tạo hoặc cập nhật skill liên quan để bắt buộc agent ghi Dev Note, tương tự Developer Plan nhưng dùng sau/Trong quá trình triển khai để ghi lại thay đổi thực tế, validation, quyết định và vấn đề còn lại.
- Dev Note giúp các phiên sau đọc lại nhanh trạng thái thực thi mà không phải suy luận từ chat/log.

## 2. Requirement liên quan

- Yêu cầu trực tiếp:
  - Dùng `skill-creator`.
  - Tạo hoặc cập nhật skill liên quan để force agent write Dev Note.
  - Dev Note tương tự Developer Plan.
- Quy tắc project:
  - Mọi thay đổi workflow/skill phải có Developer Plan và được người dùng approve trước khi sửa.
  - Phản hồi bằng tiếng Việt.

## 3. Phạm vi

### In-scope

- Tạo skill mới `dev-note` trong `.agents/skills/dev-note/`.
- Skill `dev-note` quy định:
  - Khi nào phải tạo/cập nhật Dev Note.
  - Vị trí lưu Dev Note.
  - Naming convention tương tự dev plan, theo area/module.
  - Nội dung tối thiểu: mục tiêu, plan liên quan, thay đổi thực tế, decisions, validation, blocker, next steps.
- Cập nhật `start-agent-session` để agent biết Dev Note là artifact bắt buộc trong project.
- Cập nhật `before-backend-report` để trước khi báo cáo backend phải kiểm tra/cập nhật Dev Note.
- Cập nhật summary hoặc tạo folder cần thiết cho Dev Note nếu chưa có.

### Out-of-scope

- Không thay đổi source backend business logic.
- Không thay đổi rule Checkstyle/PMD/build.
- Không viết lại Developer Plan cũ ngoài việc liên kết nếu cần.
- Không chỉnh plugin/system skill ngoài repo project.

## 4. Kiến trúc hiện tại

- Skill project nằm trong `.agents/skills/`.
- Dev plan hiện nằm trong `document/dev-impl-plan/` và đã được tổ chức theo:
  - `summary/`
  - `be/<module>/`
  - `fe/`
- Chưa có Dev Note artifact/folder/skill tương ứng.

## 5. Phương án triển khai

- Tạo skill `dev-note` bằng script `skill-creator/scripts/init_skill.py` theo hướng dẫn của `skill-creator`, sau đó chỉnh `SKILL.md` cho ngắn gọn, project-specific.
- Dev Note folder đề xuất:

```text
document/dev-note/
├── summary/
│   └── DEV_NOTE_SUMMARY.md
├── be/
│   ├── BE_DEV_NOTE_SUMMARY.md
│   └── <module>/
│       └── NNN-short-topic-yyyy-mm-dd.md
└── fe/
    └── FE_DEV_NOTE_SUMMARY.md
```

- Naming chi tiết:

```text
NNN-short-topic-yyyy-mm-dd.md
```

- Mapping số thứ tự Dev Note nên dùng cùng số với Developer Plan liên quan khi có, ví dụ:
  - Plan: `001-base-boilerplate-user-auth-2026-08-17.md`
  - Note: `001-base-boilerplate-user-auth-2026-08-17.md`
- Cập nhật `start-agent-session` để yêu cầu:
  - Khi task có thay đổi file/code/config/docs, agent phải đọc/tạo/cập nhật Dev Note sau khi triển khai.
  - Dev Note không thay thế Developer Plan và không bỏ qua approval gate.
- Cập nhật `before-backend-report` để yêu cầu:
  - Trước final report backend, Dev Note phải được cập nhật với validation thực tế.

## 6. Phạm vi file dự kiến

### Tạo mới

- `.agents/skills/dev-note/SKILL.md`
- `.agents/skills/dev-note/agents/openai.yaml`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`

### Chỉnh sửa

- `.agents/skills/start-agent-session/SKILL.md`
- `.agents/skills/before-backend-report/SKILL.md`
- Có thể cập nhật `.agents/skills/*/agents/openai.yaml` tương ứng nếu metadata stale.

## 7. API / Database / Integration

- Không ảnh hưởng API.
- Không ảnh hưởng database.
- Không ảnh hưởng runtime integration.

## 8. Test và validation

- Chạy validate skill:
  - `python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/dev-note`
- Kiểm tra file/folder:
  - `find .agents/skills/dev-note document/dev-note -maxdepth ...`
- Không cần chạy backend build vì chỉ thay đổi skill/document workflow.

## 9. Rủi ro

- Skill trigger không đủ rõ khiến agent không tự dùng Dev Note.
  - Giảm thiểu: description phải nêu rõ trigger cho mọi task có code/config/docs/workflow changes.
- Dev Note trùng lặp quá nhiều với Developer Plan.
  - Giảm thiểu: Dev Note tập trung vào thực tế triển khai, validation và delta so với plan.
- Workflow quá nặng.
  - Giảm thiểu: Dev Note tối thiểu, checklist ngắn, không yêu cầu văn dài.

## 10. Output dự kiến

- Có skill `dev-note` được validate.
- Có cấu trúc `document/dev-note/`.
- Agent tương lai được hướng dẫn bắt buộc cập nhật Dev Note trước final report.

## 11. Approval status

- Trạng thái: Approved by user via agent on 2026-08-17.
