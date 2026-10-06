# Developer Plan 092 — Timetable agent snapshot debug

Ngày: 2026-10-06. Phạm vi đã được người dùng trực tiếp cho phép: “gọi subculi thêm log đi”, trong ngữ cảnh điều tra hai lần test tại `temp-docs-debug`. Đây là thay đổi logging giới hạn; không cần xác định phiên bản business docs.

## Mục tiêu và requirement

Ghi lại đầy đủ JSON snapshot thực sự đưa vào model để đối chiếu metadata/context và lỗi validation. Trace tuân thủ skill `dev-trace-logging`: prefix `>>>TimetableAgent`, parameterized SLF4J, suffix `[threadName] [HttpRequestId]`, dùng MDC `requestId` và fallback `N/A`.

## Phạm vi và flow

`TimetableAgentProposalGenerator.attempt` tạo prompt từ snapshot rồi gọi provider. Thêm một log DEBUG có guard ngay trước provider call, gồm snapshotId, độ dài và `snapshot.snapshotJson()`. Giữ nguyên retry, timeout, API/schema/database và cấu hình logging. Không ghi raw model output, user request, token/header; không đụng `temp-docs-debug`.

File sửa: `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/timetableagent/service/TimetableAgentProposalGenerator.java`; thêm import MDC và trace trong `attempt`. Giữ nguyên Lombok constructor/logger.

## Validation và rủi ro

DEV chỉ bàn giao diff; QA độc lập kiểm tra source, compile/test, Checkstyle, PMD/build theo backend-validation và ghi Dev Note thực tế. Không gọi live provider/DB. Không thêm unit test mô phỏng một dòng logging.

JSON có thể lớn và chứa metadata lịch học; chỉ ghi khi DEBUG cho class được bật chủ động. Mỗi provider attempt ghi một lần để gắn với đúng snapshot đầu vào; không bật DEBUG mặc định. Output kỳ vọng là một trace snapshot đầy đủ kèm request correlation ngay trước model call.
