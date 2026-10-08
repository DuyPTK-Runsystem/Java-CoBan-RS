# Authorization Contract v5

## Principle

Library reuse authentication hiện hữu. Authorization quyết định ở backend.

Không tạo:

```text
/api/v2/auth/login
/api/v2/auth/refresh
/api/v2/auth/logout
```

chỉ cho Library.

## Roles

| Capability | ADMIN | LIBRARIAN | ACADEMIC_OFFICE | TEACHER | STUDENT |
|---|---:|---:|---:|---:|---:|
| Catalog read | ✓ | ✓ | ✓ | ✓ | ✓ |
| Catalog mutation | ✓ | ✓ | — | — | — |
| Patron list/lookup | ✓ | ✓ | — | — | — |
| Activate/close patron | ✓ | ✓ | — | — | — |
| Issue/revoke card | ✓ | ✓ | — | — | — |
| Borrow/return on behalf | ✓ | ✓ | — | — | — |
| Mark lost | ✓ | ✓ | — | — | — |
| Record fine payment | ✓ | ✓ | — | — | — |
| Manual batch run | ✓ | ✓ | — | — | — |
| Self patron/card/history/fines | if patron | if patron | if patron | ✓ if patron | ✓ if patron |
| Create/cancel own reservation | if patron | if patron | if patron | ✓ if patron | ✓ if patron |
| AI catalog search | ✓ | ✓ | ✓ | ✓ | ✓ |
| Own recommendations | if patron | if patron | if patron | ✓ if patron | ✓ if patron |
| Fine waive | ✓ | — by default | — | — | — |

ACADEMIC_OFFICE equality với ADMIN trong một số v3 timetable rules không được suy rộng thành Library admin permission.

## Ownership

“Self” không so sánh path `patronId` với arbitrary principal field.

Backend resolve:

```text
authentication.userId -> library_patron.userId
```

và authorize object ownership.

## Borrowing eligibility

Authorization role và business eligibility tách biệt.

Một TEACHER có quyền gọi self-service API nhưng loan creation vẫn fail nếu:

- no patron;
- patron suspended/closed;
- card invalid khi flow yêu cầu card;
- max loans;
- fine/business constraints.

Không dùng Spring Security account lock cho Library debt state.

## New LIBRARIAN role

Role được thêm vào `role` table bằng Flyway migration.

`TBD-V5-LIB-001` phải chốt ai gán role. Initial implementation không được mở endpoint tự gán role cho Librarian.

## Audit

Sensitive mutations lưu actor:

- issue/revoke card;
- borrow/return/renew;
- lost;
- fine pay/waive;
- manual batch launch;
- patron status manual change;
- catalog delete/withdraw.

Audit implementation reuse existing conventions khi đủ; thêm domain audit record chỉ nếu existing metadata không đủ investigation requirement.
