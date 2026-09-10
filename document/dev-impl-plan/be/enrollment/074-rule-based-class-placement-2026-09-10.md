# Plan 074 BE — Xếp lớp theo quy tắc

## Trạng thái và approval gate

- Application-document version: `v3`.
- Status: `Approved; implemented slice; validation incomplete`.
- Ngày lập: `2026-09-10`.
- Đi cùng [Plan 074 FE](../../fe/enrollment/074-rule-based-class-placement-ui-2026-09-10.md) và [wireframe](../../../wireframes/fe/enrollment/074-rule-based-class-placement/README.md).
- Điều kiện mở code: người dùng duyệt plan/wireframe và Plan 074.1 được duyệt/triển khai. P1–P6 đã được user chốt ngày `2026-09-10`. Không dùng fixture wireframe làm contract.

## 1. Mục tiêu

Tạo vertical slice xếp lớp có thể xem lại: một phiên chọn năm học/khối/tập học sinh/lớp đích, khai báo rule có phiên bản, chạy mô phỏng deterministic, kiểm tra dữ liệu và sĩ số, rồi xác nhận để tạo assignment mới có audit.

Traceability: `FR-V3-PLACE-001..004`, `BR-V3-PLACE-001..002`, `NFR-V3-001..002`.

## 2. Phạm vi

### In-scope

- Aggregate `PlacementSession` và `PlacementResult`, lifecycle `DRAFT -> SIMULATED -> READY_FOR_CONFIRM -> CONFIRMED`; chỉ `CONFIRMED` được ghi enrollment/assignment.
- Aggregate/snapshot `PlacementCandidate`: mỗi candidate có `studentId`, `targetAcademicYearId`, `targetGradeId`, `sourceType`, eligibility/evidence và approval metadata; không thêm `gradeId` biến thiên theo năm học vào `Student`.
- Snapshot scope, rule version, input, cảnh báo và explanation theo từng học sinh; cùng input + rule version phải cho cùng kết quả.
- Criteria được duyệt có source snapshot, class profile và missing-data behavior; không có phân bổ ngầm theo thứ tự database.
- Preview/confirm, optimistic locking, idempotency, actor/time/correlation audit, authorization backend và transaction reject-all.
- Reuse enrollment/class/student service hiện có qua service boundary; history enrollment không bị ghi đè.
- Contract, migration/JPA, unit/contract/integration tests và fixture chung với FE sau approval.

### Out-of-scope

- Tự đặt ngưỡng sĩ số, ngưỡng điểm profile hoặc role ngoài các quyết định đã chốt.
- Transfer học sinh đã có attendance/score; policy chuyển lớp là capability riêng.
- Tối ưu timetable, teacher load, bulk import, notification, background worker và UI production.
- Thay thế endpoint v2 hoặc expose entity trực tiếp qua HTTP.

## 3. Quyết định cần chốt trước khi code

| Gate | Cần xác nhận | Đề xuất để review, chưa phải contract |
|---|---|---|
| P1 | Criteria và nguồn dữ liệu được phép | **APPROVED 2026-09-10:** catalog khởi đầu gồm `SCORE`, `GENDER`. `GENDER` lấy từ hồ sơ học sinh sau [Plan 074.1](../student/074.1-student-gender-foundation-2026-09-10.md); `SCORE` phải là numeric value đã snapshot cùng `sourceReference` trong candidate/session (không dùng `Student.averageScore` hay suy đoán nguồn điểm). Criterion khác chỉ thêm khi được approve. |
| P2 | Policy phân bổ tự động | **APPROVED 2026-09-10:** tự động hóa chỉ hỗ trợ bước đầu. Lớp `ADVANCED` lấy điểm cao nhất trong capacity (`top-N`); lớp hỗ trợ lấy điểm thấp nhất trong capacity; các lớp thường nhận phần còn lại để cân bằng học lực. Tỷ lệ giới tính mục tiêu của từng lớp bám tỷ lệ chung của khối, tính từ candidate có dữ liệu hợp lệ. Điểm bằng nhau tại ngưỡng capacity là `MANUAL_REQUIRED`, không chọn ngầm. |
| P3 | Capacity policy | **APPROVED 2026-09-10:** capacity là hard limit khi Plan 074 tự động phân bổ/xác nhận; lớp vượt giới hạn là blocking issue và không confirm. Xếp/chuyển thủ công v2 của giáo vụ giữ capacity warning non-blocking; Plan 074 không thay semantics v2 này. |
| P4 | Role/capability | **APPROVED 2026-09-10:** chỉ `ADMIN` và `ACADEMIC_OFFICE` có quyền tạo/sửa, mô phỏng, xem kết quả, xác nhận và hủy phiên. Backend kiểm tra mọi action; `TEACHER`/`STUDENT` không có access Plan 074. |
| P5 | Candidate source và target-grade eligibility | **APPROVED 2026-09-10:** `CONTINUING`, `NEW_ADMISSION`, `REPEAT`. Mọi candidate có `targetGradeId` theo năm học đích. `CONTINUING` suy từ enrollment/lớp năm trước và `nextGradeId`; `NEW_ADMISSION` có target grade do giáo vụ duyệt; `REPEAT` giữ target grade theo quyết định được duyệt. |
| P6 | Missing/conflict | **APPROVED 2026-09-10:** thiếu điểm hoặc giới tính là `MANUAL_REQUIRED`, bị loại khỏi auto-allocation và không làm chặn confirm phần kết quả tự động. Giáo vụ xếp/chuyển các em này bằng flow v2 sau đó; không gán giá trị hoặc nhóm giới tính ngầm. |

## 4. Lifecycle và invariants

```text
DRAFT --simulate--> SIMULATED --all blocking issues resolved--> READY_FOR_CONFIRM
  ^                     |                                      |
  |                     +--change scope/rule--------------------+
  +------------------------------ cancel ------------------> CANCELLED

READY_FOR_CONFIRM --confirm (version + idempotency)--> CONFIRMED
```

- `CONFIRMED` là immutable về results/rules; thao tác lặp cùng idempotency key trả kết quả cũ, không tạo assignment/audit lần hai.
- Preview luôn nêu capacity, missing data, warnings/blocking conflicts và explanation; không âm thầm bỏ học sinh.
- Automatic confirm phải từ chối nếu bất kỳ target class nào vượt capacity snapshot. Sau confirm, giáo vụ vẫn dùng flow v2 để xếp/chuyển thủ công khi nghiệp vụ cho phép; warning capacity của v2 không bị đổi thành hard-block.
- Session có thể `CONFIRMED` với kết quả `MANUAL_REQUIRED`: chỉ auto-result hợp lệ được ghi assignment; các candidate đó vẫn unassigned trong năm học đích cho tới khi giáo vụ xử lý thủ công.
- Confirm revalidate scope, authorization, candidate eligibility/target grade, current enrollment, class availability và expected version trong transaction; stale state trả `409` và không retry mù.
- Với năm học đầu tiên, toàn bộ candidate có thể là `NEW_ADMISSION`; không cần enrollment/lớp năm trước. Học sinh mới vào L8 cũng là `NEW_ADMISSION` có `targetGradeId = L8`, không bị loại vì thiếu history.
- Enrollment history giữ append-only; nếu student đã thuộc scope không an toàn, trả machine-readable issue theo policy P5/P6.

## 5. Contract đề xuất

Tên URL sau là proposal cho checkpoint; chỉ triển khai sau approval. Response phân trang dùng `ResultPaginationDTO`; error semantics theo Plan 073.

| Method | Path proposal | Mục đích |
|---|---|---|
| `POST` | `/api/v3/placement-sessions` | Tạo draft với scope và target classes |
| `GET` | `/api/v3/placement-sessions/{id}` | Đọc snapshot/lifecycle/rule và summary |
| `PUT` | `/api/v3/placement-sessions/{id}` | Sửa draft với `expectedVersion` |
| `POST` | `/api/v3/placement-sessions/{id}/simulate` | Chạy deterministic preview |
| `GET` | `/api/v3/placement-sessions/{id}/results` | List preview/result và explanation |
| `POST` | `/api/v3/placement-sessions/{id}/confirm` | Confirm atomic với idempotency key + version |
| `POST` | `/api/v3/placement-sessions/{id}/cancel` | Cancel theo capability được duyệt |

Request/response tối thiểu phải biểu diễn `scope`, target classes/capacity snapshot, ordered candidate snapshot (`targetGradeId`, `sourceType`, evidence/approval, criterion value + `sourceReference`), class profile, `expectedVersion`, issue code/severity, `explanation`, `resultVersion`, audit metadata. Không chốt enum/error code mới trước contract review.

## 6. Thiết kế backend dự kiến

- `placement` module: controller mỏng, service orchestration, rule validator/engine, result mapper, repositories, DTOs và contract tests.
- Data model: `PlacementSession` sở hữu scope/rule snapshot/status/version; `PlacementCandidate` sở hữu target-grade/source/eligibility snapshot; `PlacementResult` sở hữu student/target/explanation/issues/result version (`AUTO_ASSIGNED` hoặc `MANUAL_REQUIRED`). FK/index/unique cụ thể quyết định sau schema review.
- Engine là pure deterministic function: nhận snapshot đã validate, không query ngầm hay authorize. Nó phân lớp nâng cao/hỗ trợ theo score + hard capacity, cân bằng học lực giữa lớp thường và tối thiểu hóa lệch tỷ lệ giới tính so với tỷ lệ khối; output gồm allocation, explanation hoặc `MANUAL_REQUIRED`.
- Confirm delegate sang enrollment write boundary trong một transaction; không gọi controller v2 từ controller mới.
- Audit lưu actor, time, correlation, rule/scope snapshot, before/after assignment summary; không lưu dữ liệu nhạy cảm vượt retention/privacy decision.

## 7. Test và validation dự kiến

### Unit/contract

- Cùng snapshot + policy version cho kết quả/explanation như nhau.
- Lớp nâng cao chọn điểm cao nhất và lớp hỗ trợ chọn điểm thấp nhất đến capacity; điểm bằng nhau tại ngưỡng capacity thành `MANUAL_REQUIRED`, không có tie-breaker ngầm.
- Lớp thường cân bằng học lực trên phần candidate còn lại; target giới tính của mỗi lớp được phân bổ xác định từ tỷ lệ khối của candidate đủ dữ liệu.
- Reject criterion lặp/sai source/direction/weight, target class ngoài scope, capacity policy chưa hợp lệ và scope rỗng.
- Automatic preview có capacity `<=` limit cho mọi lớp mới có thể `READY_FOR_CONFIRM`; vượt limit là blocking issue. Regression xác nhận mutation manual/bulk/transfer v2 vẫn trả warning capacity theo contract v2 thay vì bị Plan 074 chặn.
- Candidate thiếu score/gender nhận `MANUAL_REQUIRED`, không có assignment tự động và không chặn confirm auto-result; giáo vụ vẫn có thể xếp/chuyển thủ công v2 sau đó.
- `CONTINUING` chỉ hợp lệ khi grade lớp năm trước dẫn tới `targetGradeId` qua `nextGradeId`; `NEW_ADMISSION` L8 và candidate của năm học đầu tiên hợp lệ khi có target-grade approval dù không có enrollment history; `REPEAT` chỉ hợp lệ theo evidence/approval.
- Lifecycle transition trái phép, edit sau confirmed, stale expected version, confirm lặp idempotency key và cancel đúng state.

### Integration/security

- `401`, `403`, `404`, `409`, `422`; `ADMIN`/`ACADEMIC_OFFICE` được kiểm tra cho mọi action, còn `TEACHER`/`STUDENT` bị `403`.
- Confirm atomic: lỗi ở một assignment/audit rollback toàn bộ; history cũ bất biến; reload trả snapshot/result/audit nhất quán.
- Migration, FK/index/unique và audit persistence theo schema final.

### Quality gates

- BE test, JaCoCo, Checkstyle, PMD và build theo workflow thực tế.
- FE fixture/component/Storybook chạy song song sau contract; browser/live là `NOT RUN` cho đến khi thực chạy trên môi trường resettable.

## 8. Phạm vi file dự kiến sau approval

| Khu vực | Thay đổi dự kiến |
|---|---|
| `BE/.../placement/**` | module controller/service/domain/DTO/repository/test mới |
| `BE/.../enrollment/**` | service boundary cần thiết để confirm atomic, không đổi history semantics |
| `BE/.../db/migration/` | migration PlacementSession/Result, audit/index/FK sau schema approval |
| `document/application-doc/v3/**` | contract/data decision amendment sau P1–P6 |
| `document/dev-note/be/enrollment/` | Dev Note thực tế sau implementation |

## 9. Acceptance criteria

- Người có capability được duyệt tạo/sửa draft, mô phỏng và xem result theo scope.
- Preview cho biết lý do phân bổ, capacity và dữ liệu thiếu/conflict; blocking issue chặn confirm.
- Confirm chỉ ghi assignment mới một lần, không phá lịch sử enrollment và có audit/version/idempotency.
- Kết quả deterministic và có thể đọc lại cùng rule/scope snapshot.
- Không có endpoint/DTO/role/policy bị suy diễn từ wireframe; mọi validation chưa chạy ghi `NOT RUN`/`BLOCKED`.
