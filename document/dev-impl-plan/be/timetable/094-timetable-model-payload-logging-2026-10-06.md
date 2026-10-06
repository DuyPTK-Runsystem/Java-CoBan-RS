# Developer Plan 094 — Timetable model payload logging

Ngày: 2026-10-06. Approved: người dùng phê duyệt phương án qua root agent trước triển khai.

## Mục tiêu và requirement
DEBUG đầy đủ system prompt, snapshot/user request/retry feedback và schema thực sự gửi tại proposal/save boundary bằng literal `[MODEL REQ]`; log raw generation text/tool calls trước validation/conversion bằng literal `[MODEL RESPONSE}`.

## Scope và implementation
Sửa `SpringAiTimetableModelGateway` và thêm helper trace nhỏ trong package ai. Giữ prompt/API/schema/provider options/timeout/retry nguyên trạng. Hai virtual-thread boundaries `TimetableAgentProviderCall` và `TimetableAgentModelActionService` chỉ truyền MDC requestId tối thiểu, cleanup sau call. Prefix `>>>TimetableAgent`, suffix thread/requestId, fallback N/A; không log HTTP headers/credentials/options chứa key. Không bật DEBUG mặc định.

## Validation và risks
DEV không chạy test; QA độc lập source review và backend-validation. Không live provider/DB. DEBUG full payload có dữ liệu lịch và kích thước lớn, chỉ dùng development khi bật logger chủ động. Không thêm test chỉ mirror một dòng log; QA kiểm tra log trước parse, malformed/multiple generation và schema actual.

## Output
Mỗi model call có request đầy đủ và mọi response generation nhận được; timeout không có model response không được bịa response. Dev Note 094 và summary ghi QA pending.
