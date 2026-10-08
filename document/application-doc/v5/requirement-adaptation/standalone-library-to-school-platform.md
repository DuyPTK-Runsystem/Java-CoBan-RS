# Adaptation Map — Standalone Library → Java-CoBan-RS v5

## Quy ước

- `KEEP`: giữ business behavior.
- `ADAPT`: giữ mục tiêu nhưng đổi integration/contract.
- `REUSE`: capability đã có ở platform; Library chỉ dùng.
- `DROP`: standalone concern không được đưa vào v5.
- `TBD`: cần product/implementation decision.

## Platform assumptions

| Source standalone | v5 | Class |
|---|---|---|
| Spring Boot app riêng | cùng Spring Boot app hiện hữu | DROP/REUSE |
| PostgreSQL 16 | MySQL hiện hữu | ADAPT |
| Flyway V1–V7 mới | tiếp nối migration head của repo | ADAPT |
| `com.training.library` | `com.JavaTraining.BaiTap_RS.library.*` hoặc package vertical tương thích repo | ADAPT |
| Vue app riêng | FE hiện hữu | REUSE |
| Pinia/Axios bắt buộc | dùng FE conventions hiện hành | DROP |
| JWT + refresh-token subsystem | existing JWT/Security | REUSE |
| role `MEMBER` | `library_patron` domain state + existing STUDENT/TEACHER | ADAPT |
| role `LIBRARIAN` | thêm role LIBRARIAN | KEEP/ADAPT |
| Anthropic SDK/Claude cố định | Spring AI/provider configurable | ADAPT |
| Postgres Testcontainers | MySQL Testcontainers cho DB-specific tests | ADAPT |
| docker compose mới | deployment/runtime hiện hữu | REUSE |

## Business rules BR-01..08

| Source | v5 |
|---|---|
| BR-01 max 5 copies | `BR-V5-LIB-CIRC-001` KEEP |
| BR-02 due 14 days, reference not borrowable | `BR-V5-LIB-CIRC-002`, `BR-V5-LIB-CAT-003` KEEP |
| BR-03 renew 2x +7 days, no reservation | `BR-V5-LIB-CIRC-003` KEEP |
| BR-04 progressive fine | `BR-V5-LIB-FINE-001` KEEP |
| BR-05 debt >500k suspends account | suspend `library_patron` borrowing only; **do not lock app_user** ADAPT |
| BR-06 one active loan/copy | KEEP; implement MySQL-safe DB/transaction constraint |
| BR-07 card 12 months / lost invalid | KEEP |
| BR-08 lost = price +50k | KEEP |

## M1 — Catalog

- FR-01 → `FR-V5-LIB-CAT-001`: KEEP.
- FR-02 → `FR-V5-LIB-CAT-002`: KEEP.
- FR-03 → `FR-V5-LIB-CAT-003`: KEEP.
- FR-04 → `FR-V5-LIB-CAT-004`: KEEP.
- FR-05 → `FR-V5-LIB-CAT-005`: ADAPT; storage implementation không mặc định filesystem.

## M2 — Member/Card

- FR-10 “register/manage member” → `FR-V5-LIB-PATRON-001`: **ADAPT mạnh**. Library không tạo user/student/teacher; chỉ activate patron cho identity hiện hữu.
- FR-11 issue card → `FR-V5-LIB-CARD-001`: KEEP.
- FR-12 render card → `FR-V5-LIB-CARD-002`: KEEP/SHOULD.
- FR-13 lost/reissue → `FR-V5-LIB-CARD-003`: KEEP.
- FR-14 self QR → `FR-V5-LIB-CARD-004`: KEEP.

## M3 — Circulation

FR-20..25 được giữ về nghiệp vụ và chuyển thành `FR-V5-LIB-CIRC-001..006`.

Khác biệt:

- borrower identity resolve qua `library_patron`;
- endpoint dùng `/api/v2`;
- owner authorization dựa trên authenticated user mapping;
- concurrency strategy dùng MySQL, không PostgreSQL partial index.

## M4 — Fine/Batch

FR-30..35 được giữ về intent.

Thay đổi:

- reuse Spring Batch hiện hữu;
- notification/email reuse platform;
- update `LibraryPatronStatus`, không update global account lock;
- MySQL upsert/transaction design thay PostgreSQL `ON CONFLICT`.

## M5 — Security

| Source | v5 |
|---|---|
| FR-40 auth access/refresh token | DROP as Library requirement |
| FR-41 role authorization | REUSE + Library authorization contract |
| FR-42 owner-only data | KEEP |
| FR-43 suspend in UserDetailsService | REWRITE: borrowing suspension ở Library domain |
| FR-44 audit | REUSE existing audit + add domain events where needed |
| FR-45 login throttling | OUT OF V5; platform-security concern |

Library không thêm `/api/v2/auth/*`.

## M6 — Barcode/QR/Camera

FR-50..55 giữ gần nguyên. Camera decode ở client; manual fallback bắt buộc.

## M7 — AI

FR-60..65 giữ product intent nhưng:

- không direct Anthropic SDK;
- không hard-code Claude model;
- use Spring AI structured output;
- model không sinh SQL;
- backend query DB;
- feature flags/fallback;
- no Internet tests.

## API adaptation

Source `/api/books`, `/api/members`, ... được chuyển sang v5 API baseline:

```text
/api/v2/books
/api/v2/book-copies
/api/v2/library-patrons
/api/v2/library-cards
/api/v2/loans
/api/v2/returns
/api/v2/reservations
/api/v2/fines
/api/v2/library/batch-jobs
/api/v2/library/ai/*
```

Nếu path collision xuất hiện trước implementation thì namespace affected resource bằng `/api/v2/library/*`; không bump toàn Library lên `/v3` chỉ vì một collision.

## Evaluation/training requirements

Các nội dung source về “5 tuần”, “thang điểm 100”, “trừ điểm tự động” là training-delivery metadata, không phải product requirement của Java-CoBan-RS.

Chúng có thể được chuyển thành Developer Plan quality gates nhưng không nằm trong runtime contract v5.
