# Wireframe 061 — Transcript Viewer UI

## Mục đích

Wireframe tĩnh, deterministic cho Plan 061. Không gọi backend, không chứa production component và không quyết định thay cho contract đang bị gate ở Plan 046.

Mở `index.html` trực tiếp trong browser. Dùng dropdown **Demo state** để duyệt các state:

- `Finish — term`: transcript học kỳ đã tính xong;
- `In progress — annual`: đang tính, không coi điểm cũ là official latest;
- `Empty`: chưa có môn/kết quả trong context hợp lệ;
- `Not found — 404`: resource không tồn tại;
- `Unauthorized — 401`: session hết hạn;
- `Forbidden — 403`: backend từ chối scope;
- `Conflict — 409`: chỉ là error state contract, không có mutation trong viewer;
- `Loading`: skeleton tĩnh.

## Scope được thể hiện

- Student self-service `/me` là flow mặc định.
- Term/annual tabs.
- Subject rows, assessment/term/annual result fields.
- `REGULAR`/`RETAKE`, điểm trước/sau retake.
- `IN_PROGRESS`/`FINISH`, source/calculated version và last calculated time.
- Read-only styling, empty/error/permission states và responsive table.

## Contract blockers được giữ nguyên

Plan 046 trong Developer Plan vẫn ghi `Proposed`, còn Dev Note/summary ghi đã hoàn thành. Wireframe không giả định inconsistency này đã được giải quyết. Staff viewer và student picker không được minh họa như capability mặc định vì role discovery/assignment scope cần contract được approve.

Các label/field trong preview là dữ liệu demo có chú thích; implementation phải dùng DTO/enum thực tế sau khi gate được chốt. Đặc biệt không dùng `NORMAL` thay cho wire `ACADEMIC`, không đổi `null` thành `0`, không tự tính official average và không thêm `MIXED`.

## Review checklist

- [ ] Approved Plan 061 và wireframe bằng agent message.
- [ ] Chốt trạng thái/response DTO/error envelope của Plan 046.
- [ ] Chốt student self-only hay thêm staff scope.
- [ ] Xác nhận role/capability contract trước khi làm navigation/visibility.
- [ ] Xác nhận có/không pagination cho transcript subjects.
- [ ] Xác nhận empty domain state có phải `200` rỗng hay `404`.
