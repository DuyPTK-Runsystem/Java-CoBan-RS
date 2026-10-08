# Error Contract v5

## Principle

Library dùng global error format hiện hữu của Java-CoBan-RS.

Machine `code` ổn định; FE map/translate bằng code, không parse `message`.

HTTP status phải phản ánh category, nhưng business code mới là stable client contract.

## Baseline codes

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `REFERENCE_ONLY` | copy không cho mượn |
| 400 | `CARD_PAYLOAD_MALFORMED` | QR malformed |
| 400 | `CARD_SIGNATURE_INVALID` | QR tampered/invalid |
| 400 | `CARD_EXPIRED` | card expired |
| 400 | `CARD_REVOKED` | card revoked |
| 400 | `INVALID_FINE_AMOUNT` | invalid monetary input |
| 403 | `PATRON_BORROWING_SUSPENDED` | Library borrowing suspended |
| 403 | `LIBRARY_RESOURCE_FORBIDDEN` | lacks role/ownership |
| 404 | `BOOK_NOT_FOUND` | no book |
| 404 | `COPY_NOT_FOUND` | no copy by id/barcode |
| 404 | `PATRON_NOT_FOUND` | no patron |
| 404 | `ACTIVE_LOAN_NOT_FOUND` | return/renew cannot resolve active loan |
| 409 | `COPY_ALREADY_ON_LOAN` | concurrent/active loan conflict |
| 409 | `MAX_ACTIVE_LOANS` | max five |
| 409 | `RENEW_LIMIT_REACHED` | renew count max |
| 409 | `COPY_RESERVED` | reservation prevents renew |
| 409 | `CARD_ALREADY_ACTIVE` | patron already has active card |
| 409 | `PATRON_ALREADY_EXISTS` | app_user already activated |
| 409 | `RESERVATION_ALREADY_EXISTS` | duplicate active reservation if policy forbids |
| 422 | `COVER_NOT_RECOGNIZED` | AI metadata confidence insufficient |
| 422 | `AI_OUTPUT_INVALID` | structured output rejected |
| 429 | `AI_RATE_LIMITED` | app/provider rate limit |
| 503 | `AI_UNAVAILABLE` | AI feature unavailable and no transparent result |

## Concurrency mapping

Database unique/lock failure for active loan must be translated to `409 COPY_ALREADY_ON_LOAN`.

Do not expose:

- SQLState;
- constraint name as client contract;
- stack trace;
- vendor error body.

## AI fallback

Nếu feature có deterministic fallback (ví dụ reminder template), request không nhất thiết trả `AI_UNAVAILABLE`; service có thể hoàn thành bằng fallback và record telemetry.

Nếu user explicitly requests AI-only action và AI unavailable, return stable 503/appropriate code.
