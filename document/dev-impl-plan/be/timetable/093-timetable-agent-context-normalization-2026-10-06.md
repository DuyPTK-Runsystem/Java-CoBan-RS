# Developer Plan 093 — Timetable agent occupied-context normalization

Ngày: 2026-10-06. Phạm vi được người dùng ủy quyền trực tiếp: cùng `assignmentId`, `periodId`, `functionalRoomId` và các khoảng ngày giao nhau trong context phải được coi là một occupied context; giữ nguyên khoảng rời, assignment hoặc phòng khác; dùng cùng semantics cho snapshot gửi model, retained validation và teacher-load. Không sửa persisted timetable rows.

## Mục tiêu và flow hiện tại

`TimetableAgentSnapshotEntries` chỉ loại published context khi khớp đủ cả ngày bắt đầu/kết thúc với current entry. Các bản sao có khoảng ngày khác nhau vẫn cùng vào snapshot. `TimetableAgentEntryProjection` đưa current/published vào retained validation, còn `TimetableAgentSnapshotLoads` và `TimetableAgentDomainValidation` chuyển chúng thành `TimetableEntry` cho evaluator; do đó các bản sao giao nhau bị đếm/trình bày lặp.

## Phạm vi

- In scope: chuẩn hóa context dẫn xuất trên bộ khóa `(assignmentId, periodId, functionalRoomId)`, gộp các interval giao nhau thành union; giữ phần ngày không giao nhau và khoảng rời; dùng chuẩn hóa nhất quán ở snapshot/model và đường retained/load.
- Bảo toàn current entry IDs, locked-entry semantics, nội dung current ngoài proposal scope, và dữ liệu persisted. Chỉ tạo giá trị context dẫn xuất mới; không sửa entity managed.
- Out of scope: retry, timeout, feedback, database writes/migrations, thay đổi API contract.

## Phương án và file

Thêm utility thuần `TimetableAgentOccupiedContext` để chuẩn hóa interval một cách bất biến, giữ metadata đại diện của current entry; phần published bị che bởi current sẽ được cắt theo union current và phần published còn lại được chia thành các khoảng rời. Gọi chung utility khi dựng `EntryData`/snapshot và khi tạo retained entities để teacher-load và validator nhận cùng occupancy. Current entries gốc giữ nguyên để khóa ID và diff/persistence.

Dự kiến chỉnh `TimetableAgentSnapshotEntries.java`, `TimetableAgentEntryProjection.java`, cùng snapshot builder nếu cần đồng bộ JSON nguồn; thêm utility và unit tests cho giao nhau, lồng nhau, khoảng rời, key khác, cắt published qua biên current, và bất biến input/ID. Kiểm tra test command, compile, Checkstyle, PMD/build thuộc QA độc lập; DEV không chạy kiểm tra.

## Rủi ro và output

Rủi ro chính là làm mất identity của current hoặc xóa nhầm occupancy ngoài scope. Giảm thiểu bằng giữ nguyên `currentEntries` canonical và locked IDs, cắt chỉ phần context trùng đã được thay thế, đồng thời giữ nguyên các ngày còn lại. Kết quả dự kiến: interval overlap chỉ góp một lần vào context/model, retained validation và teacher-load; database rows không đổi.
