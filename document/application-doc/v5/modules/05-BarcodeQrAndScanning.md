# Module 05 — Barcode, QR and Camera

## Barcode

Book-copy barcode:

```text
LIB-{9 digit zero-padded copy id}
```

Ví dụ:

```text
LIB-000000123
```

Symbology: Code128.

Barcode identify `BookCopy`, không identify `Book`.

## Library card

Card number baseline:

```text
LC-{year}-{6 digit sequence}
```

Payload:

```text
v1|{cardNo}|{patronId}|{expEpochDay}|{HMAC}
```

QR ECC level tối thiểu M nếu implementation dùng ZXing.

## Security

- HMAC secret ở environment/secret store.
- Không commit secret.
- So sánh signature constant-time.
- Verify signature không thay DB state lookup.
- Card REVOKED/EXPIRED fail dù payload cryptographically valid.

## Camera

Decode tại browser. Vì không có thiết bị thực tế trong demo, core FE dùng upload ảnh QR/barcode làm input thay thế; ảnh được đọc và decode thành chuỗi ngay ở FE.

Preferred implementation có thể dùng native `BarcodeDetector` khi available và ZXing fallback nếu dependency được plan duyệt.

Bắt buộc:

- manual input fallback;
- upload-image demo cho QR user/thẻ và barcode sách;
- stop media tracks khi unmount/leave page;
- handle permission denied;
- duplicate scan guard;
- HTTPS hoặc localhost requirement được ghi trong runbook/demo.

Backend circulation nhận chuỗi đã decode, không nhận camera frame hoặc file ảnh trong hot path.

## Circulation UX

Màn hình `/library/circulation` ưu tiên keyboard/scanner flow:

1. patron scan/lookup;
2. copy scans;
3. summary;
4. Enter/explicit confirm;
5. visible success/error per copy.

Không gửi camera frame lên backend cho barcode decode.

## Printable output

SHOULD:

- QR/card PDF;
- sheet PDF nhiều Code128 labels.

Nếu thêm PDF library mới, dependency phải được Developer Plan review thay vì mặc định copy OpenPDF từ source training.
