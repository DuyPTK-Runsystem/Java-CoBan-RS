# Developer Plan: Shared Dev Trace Logging Skill

## Mục tiêu

Đưa quy ước trace log do developer/agent viết vào repository để mọi agent làm việc với project dùng chung qua `.agents/skills`.

## Requirement và phạm vi

- Mỗi developer trace log bắt đầu bằng `>>>`, nêu module rõ ràng và kết thúc bằng `[threadName] [HttpRequestId]`.
- Tạo `.agents/skills/dev-trace-logging/SKILL.md` cho Java/Spring; sử dụng SLF4J parameterized logging và MDC key `requestId`.
- Khi ứng dụng chưa có cơ chế request ID, skill hướng dẫn `OncePerRequestFilter` tạo UUID và cleanup MDC trong `finally`.
- Không sửa Java source để chèn log/filter; không tạo remote repository, CI hoặc dependency mới.

## Phương án và file ảnh hưởng

`.agents/skills/` là vị trí skill version-controlled sẵn có của project. Tạo skill tại đây để chia sẻ quy ước; giữ skill cá nhân ở `~/.codex/skills` nguyên trạng.

- Tạo: `.agents/skills/dev-trace-logging/SKILL.md`.
- Cập nhật: Developer Plan và Dev Note summaries.
- Tạo/cập nhật: `document/dev-note/be/workflow-skill/019-dev-trace-logging-skill-2026-08-19.md`.

## Validation và rủi ro

- Chạy `python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/dev-trace-logging`.
- Backend build/test/PMD/Checkstyle: `NOT RUN`, vì không đổi source backend.
- Không log secret hoặc dữ liệu nhạy cảm không cần thiết; cleanup MDC trong `finally` tránh rò rỉ context giữa request.

## Approval status

- Trạng thái: Approved.
- Approved by user via agent on 2026-08-19.
