# Plan 081 — Seed xếp lớp và TKB khối 7

## Phạm vi

- Bảo đảm 40 học sinh `STU2600041`–`STU2600080` có enrollment `ACTIVE` vào `7A1`–`7A4` (10 học sinh/lớp), không tạo duplicate.
- Seed TKB cho bốn lớp ở HK1 `2026-09-01`–`2026-12-31` và HK2 `2027-01-01`–`2027-05-31`, chỉ thứ Hai–thứ Sáu, theo assignment ACTIVE.
- Có audit marker `SEED_PLAN_081_G7`; bỏ qua học kỳ đã có dữ liệu để không ghi đè.

## Thay đổi

- `DemoIdentitySeeder`: bảo đảm enrollment canonical của 40 học sinh khối 7.
- `DemoTimetableSeeder`: fixture TKB HK1/HK2, kiểm tra ngày học kỳ, assignment và conflict lớp/GV.
- `DemoDataSeeder`: wire timetable seeder sau assignment seeder.

## Validation

- `compileJava`: PASS.
- `test --tests com.JavaTraining.BaiTap_RS.bootstrap.DemoDataSeederIntegrationTest`: PASS; JaCoCo chạy PASS.
- `git diff --check`: PASS.
- Full backend test, Checkstyle/PMD và build đầy đủ: chưa chạy trong lượt này.
