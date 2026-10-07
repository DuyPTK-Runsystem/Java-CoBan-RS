# Module 06 — Library AI

## Architecture rule

Library AI dùng Spring AI abstraction hiện hữu.

Không hard-code:

- vendor;
- model name;
- vendor SDK;
- provider-specific request body trong domain service.

Provider-specific capability chỉ được thêm sau capability check và configuration.

## AI-01 — Book metadata from image

Input: image bìa/trang bản quyền.

Output structured:

```json
{
  "title": "string",
  "author": "string",
  "publisher": "string|null",
  "publishedYear": 2024,
  "isbn": "string|null",
  "confidence": 0.95
}
```

Backend validation:

- size/MIME;
- schema;
- confidence policy;
- ISBN format nếu có.

AI chỉ prefill. Librarian xác nhận trước create/update Book.

## AI-02 — Natural-language search

Model output là filter DTO, ví dụ:

```json
{
  "keyword": "lịch sử Việt Nam",
  "category": "HISTORY",
  "publishedYearFrom": 2015,
  "availableOnly": true,
  "sort": "TITLE_ASC"
}
```

Backend validate allowlist/range rồi gọi Catalog query.

Model không sinh SQL, JPQL hoặc repository method name để execute.

## AI-03 — Summary and tags

SHOULD.

Có thể chạy batch cho book chưa enrich.

Tags lưu bằng MySQL-compatible representation:

- normalized relation; hoặc
- JSON column;

không dùng PostgreSQL `varchar[]`.

## AI-04 — Recommendation

SHOULD.

Input gửi provider phải tối thiểu hóa PII. Ưu tiên history dạng book/category/rating-like signals thay vì tên/email/student code.

Recommendation không thay thế availability/business validation.

## AI-05 — Reminder drafting

SHOULD.

Java cung cấp:

- overdue days;
- amount;
- due date;
- tone level.

Model chỉ draft prose.

Không gửi:

- full name;
- email;
- phone;
- student code;
- auth data.

Java/template layer gắn lời chào và exact numeric facts.

AI lỗi → template cứng.

## AI-06 — Policy Q&A

BONUS.

Chỉ trả lời từ approved library policy context. Không biết phải nói không biết/escalate librarian.

Không được coi model answer là authoritative mutation/business decision.

## Guardrails

- feature flag `app.ai.library.enabled` hoặc config equivalent;
- timeout;
- bounded retries;
- structured output cho machine-readable task;
- input length limits;
- logging metadata/token/latency nhưng không log sensitive prompts;
- no Internet in tests;
- 429/5xx/timeout/malformed-output fallback tests;
- library core vẫn chạy khi AI off.

## API

```text
POST /api/v2/library/ai/book-metadata
POST /api/v2/library/ai/search
GET  /api/v2/library/ai/recommendations
POST /api/v2/library/ai/policy-chat
```

Nếu catalog UI muốn natural search như một mode của `/api/v2/books`, FE có thể gọi AI translation endpoint rồi Catalog query; không duplicate catalog authorization/query rules.
