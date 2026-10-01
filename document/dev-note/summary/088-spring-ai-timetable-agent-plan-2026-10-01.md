# Dev Note 088 — CR v4 và kế hoạch agent thời khoá biểu

- Ngày: 2026-10-01; docs-only.
- [Plan 088](../../dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md): **DRAFT, implementation approval PENDING**.
- Authorization: người dùng yêu cầu lập kế hoạch Spring AI agent xếp TKB, xác nhận nền v3 và CR cải tiến v4; provider/model giữ cấu hình linh hoạt.

## Thực tế đã làm

Soạn [CR-V4-001](../../application-doc/v4/change-request/CR-V4-001-spring-ai-timetable-agent.md), plan chung BE/FE, prompt, proposal/tool argument schema và HTML wireframe static. Không thay source application, dependency, migration hoặc cấu hình triển khai.

| Khu vực file | Mục đích |
|---|---|
| `document/application-doc/v4/README.md`, `change-request/CR-V4-001-spring-ai-timetable-agent.md` | Phân loại cải tiến trên nền v3, requirements và decisions draft |
| `document/application-doc/v4/agent-contract/` | Prompt versioned, JSON Schema và ví dụ đúng domain IDs |
| `document/dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md` | Kiến trúc, API/storage/file scope/test/risks/approval gates |
| `document/wireframes/fe/timetable/V4-001-spring-ai-agent/` | Preview flow và trạng thái UI static để review |
| Plan/Dev Note summaries | Liên kết plan/note cross-area |

## Quyết định và evidence

- Đọc checkout hiện tại: Boot 4.0.7/Java 21; `ReqEntryItemDTO` dùng assignment/period/room/date range; `validateRevision()` có ghi dữ liệu; `ClassSubject` không có số tiết yêu cầu.
- Dùng pure candidate validation trước lưu; application giữ snapshot/approval/executor. Native tool call tham chiếu immutable proposal sau duyệt; server map DTO, scope-check IDs và revalidate atomically.
- Dòng Spring AI 2.0.x tương thích Boot 4.x theo tài liệu chính thức, không coi là runtime dependency proof. Provider/model chưa chốt.
- CR/plan/API/schema/design đều draft; user xác nhận version/phân loại chưa phê duyệt implementation.

## Validation thực tế

| Gate | Kết quả |
|---|---|
| Node inline: UTF-8, relative Markdown links, fenced blocks | PASS — 9 file mới, 32 relative links, không replacement character; fences cân bằng |
| Node inline: JSON Schema draft-07 compile và ví dụ bằng AJV từ `FE/node_modules` | PASS — 2 schema, 2 positive examples, 8 negative cases (unknown field, invalid status/ID/date, missing field, extra SQL parameter, invalid version/type) |
| Node inline: HTML DOM structure bằng jsdom từ `FE/node_modules` | PASS — tiếng Việt, 8 hàng slot, 5 ngày minh hoạ, label targets, nút disabled, không script; static smoke không phải browser proof |
| `git diff --check` và Node kiểm tra whitespace file mới | PASS — không trailing whitespace; git check cho tracked summaries |
| BE test/JaCoCo/Checkstyle/PMD/build | NOT RUN — không thay production code |
| FE lint/test/coverage/build/Storybook | NOT RUN — không thay production FE |
| Native browser/layout/accessibility review | NOT RUN — wireframe static, chưa review trực quan |
| Provider, API runtime, MySQL migration/concurrency, lưu/reload thật | NOT RUN — chưa implementation |

## Deviation, blocker và tiếp theo

### Amendment ngôn ngữ system prompt — 2026-10-01

- Người dùng yêu cầu system prompt tiếng Anh, kết quả tiếng Việt. Đã chuyển hai system prompts proposal/action và tool description sang tiếng Anh, yêu cầu explanation/message/summary tiếng Việt; identifiers/schema/enums không dịch.
- Prompt version draft tăng timetable-agent-1 → timetable-agent-2. Đồng bộ Plan/CR/harness guide; JSON Schema, ví dụ JSON và application source không đổi.
- Validation amendment ngôn ngữ: Node inline kiểm hai system prompts tiếng Anh có yêu cầu Vietnamese, promptVersion, schema/ví dụ, UTF-8/fences/whitespace/local links PASS; `git diff --check` PASS. Provider/runtime NOT RUN.

### Amendment tài liệu sau trao đổi — 2026-10-01

- Authorization mới: người dùng yêu cầu “cập nhật tài liệu đi”, phạm vi docs-only; implementation approval vẫn PENDING.
- Thêm [harness/tools/snapshot/cache](../../application-doc/v4/agent-contract/harness-tools-and-prompt-cache.md), đồng bộ CR, Plan 088, prompt guide, v4 README và wireframe README. Không sửa application, dependency, migration hoặc hai JSON Schema hiện có.
- Làm rõ hybrid và harness; đưa read/validate tools thành D08 đề xuất; D09 snapshot persistence/reconstruction và D10 metrics-first prompt cache giữ draft/TBD. Không nâng câu hỏi trước đó thành approval các lựa chọn này.
- Nguồn TTL: tài liệu Claude chính thức xác nhận 5 phút mặc định và lựa chọn 1 giờ; 300s không áp dụng chung khi provider/model chưa chốt. Không gọi keep-warm timer; snapshot/approval validity độc lập cache.
- Các PASS trong bảng phía trên là validation lần soạn ban đầu. Amendment: Node inline kiểm 7 Markdown files/41 relative links, UTF-8 replacement characters, whitespace, balanced fences và duplicate headings PASS; AJV compile 2 schema/2 ví dụ hợp lệ và reject extra tool argument PASS; `git diff --check` PASS. Browser/provider/cache benchmark/runtime vẫn NOT RUN.

Không triển khai feature. Bổ sung wireframe/contract để kế hoạch có thể review. Nhu cầu số tiết chưa có nguồn trong contract đã đọc nên đề xuất nhập/xác nhận; không tự gán số tiết. Provider/model, scale/budget, retention và concurrency protocol còn checkpoint trong plan. Review CR/plan/wireframe và duyệt trước code; sau implementation ghi Dev Notes BE/FE với validation thực tế.
