# Dev Note 073 — v3 Foundation Contract

## Liên kết

- Developer Plan: `document/dev-impl-plan/be/073-v3-foundation-contract-2026-09-09.md` và
  `document/dev-impl-plan/fe/foundation/073-v3-foundation-contract-2026-09-09.md`.
- Approval: Plan 073, CR-V3-001 và baseline v3 được người dùng phê duyệt qua agent ngày
  `2026-09-10`.

## Phạm vi thực tế

- Chuyển CR/baseline/Plan 073 sang approved và thêm contract checkpoint v3.
- Thêm BE DTO boundary `V3PageResponse` cùng unit test.
- Thêm FE typed page/query/error state, URL query serializer, fixture deterministic,
  `V3ContractReviewPanel` và Storybook states.
- Bổ sung review state 401 và fallback page-size do capability truyền vào.
- Không thêm endpoint, schema/migration, route production hoặc nghiệp vụ Plans 074–079.

## Quyết định chính

- Page mới v3 dùng `items`, `page`, `pageSize`, `total`, `appliedFilters`; endpoint cụ thể
  vẫn phải do plan capability chốt.
- Theo quyết định người dùng, không thêm error code/error payload riêng cho v3; handler và
  contract v2 không đổi. Capability sau chỉ đề xuất semantics lỗi chi tiết khi API thật cần nó.
- `401` runtime vẫn do `apiClient` xóa session/gọi redirect handler; state 401 trong Storybook
  chỉ là review evidence, không thay thế redirect thật.
- `TBD-003` vẫn chặn role matrix nghiệp vụ. Wireframe/Storybook là review-only và chưa
  được coi là UI production.

## Validation Result

### Backend

- `./gradlew test --tests com.JavaTraining.BaiTap_RS.common.contract.v3.V3ContractDtoTest
  --console=plain --quiet`: PASS; `V3ContractDtoTest` có `2` test, `0` failure/error.
- `./gradlew test --console=plain --quiet`: PASS; report có `380` test, `0` failure/error;
  `jacocoTestReport` được
  tạo bởi test task.
- `./gradlew checkstyleMain` và `./gradlew checkstyleTest`: PASS task; không có warning ở
  file Plan 073. Warning còn lại là baseline cũ ở file khác.
- `./gradlew pmdMain`: FAIL baseline với `5` violation ngoài scope ở
  `SchoolClassService` và `ClassTranscriptQueryService`.
- `./gradlew build`: FAIL do `pmdMain` (5 violation) và `pmdTest` (79 violation baseline),
  không phải test failure.

### Frontend

- `npm run lint`: PASS.
- `npm run test`: PASS; `89` file, `475` test.
- `npm run test:coverage`: PASS; tổng report `85.70%` line, `69.12%` function,
  `74.79%` branch; project không đặt threshold.
- `npm run build`: PASS.
- `npm run build-storybook`: PASS; warning dependency PrimeVue và chunk-size không làm build fail.

### Không chạy

- Browser/live API walkthrough: `NOT RUN`; không có endpoint v3 capability hoặc môi trường
  resettable/fixture role cho flow thật.

## Sai lệch và bước tiếp theo

Amendment sau implementation (`2026-09-10`): người dùng quyết định toàn hệ thống dùng
`ResultPaginationDTO`. `V3PageResponse` được ghi nhận ở Dev Note này vẫn là lịch sử thực tế
của Plan 073, nhưng contract đã bị supersede và artifact/test sẽ được xóa trong Plan 077.

Theo yêu cầu người dùng, đã gỡ error code/error payload v3 đã từng được thêm trong Plan 073;
HTTP status và `RestResponse` v2 là error contract duy nhất hiện có. Plan 074–079 phải chốt
semantics lỗi cùng endpoint thật, chốt role matrix liên quan và bổ sung integration/browser
evidence. PMD baseline cần một plan riêng nếu muốn toàn bộ backend validation xanh.
