# Developer Plan 053: Grade, Class, Subject & Class-Subject UI

## Trạng thái phê duyệt

- Application version: `v2`.
- Status: `Approved for phases 0–6; phases 4–5 implementation completed; Phase 6 blocked by backend contract`.
- Phạm vi hiện tại: FE plan hợp nhất hai phạm vi được mô tả trong backlog:
  `Grade & Class Management UI` và `Subject & Class Subject UI`.
- Phase 1–3 Storybook UI đã triển khai bằng fixture tĩnh; chưa tích hợp
  backend/database/API contract, route hoặc view production.
- User approval qua agent: phases `0–1–2–3`, then remaining phases `4–5–6` on 2026-08-27.
- Wireframe review:
  [`document/wireframes/fe/academic/053-grade-class-subject-management/index.html`](../../../../wireframes/fe/academic/053-grade-class-subject-management/index.html).

## Mục tiêu

Xây dựng module FE quản lý catalog học vụ trên authenticated v2 shell, gồm:

1. Quản lý metadata khối.
2. Quản lý lớp theo năm học và khối.
3. Hiển thị các chỉ báo sĩ số/cảnh báo mất cân bằng khi backend cung cấp dữ
   liệu tương ứng.
4. Quản lý môn học, loại `ACADEMIC`/`SKILL` và trạng thái hoạt động.
5. Cấu hình phạm vi áp dụng môn theo khối/lớp và học kỳ.
6. Gán môn vào một lớp trong một học kỳ thông qua `class_subject`.

Plan này kế thừa authenticated shell, shared API client, state components và
Academic Year/Semester context từ Plan 051/052. Các form là dialog state do
view sở hữu; không tạo route riêng chỉ để mở dialog.

## Phương án triển khai theo phase và approval gate

Không triển khai toàn bộ Plan 053 trong một prompt. Mỗi phase dưới đây dừng ở
Storybook để bạn review trực quan; chỉ tiếp tục khi có tin nhắn duyệt phase đó.

| Phase | Nội dung | API/backend | Điểm dừng chờ duyệt |
|---|---|---|---|
| `0` | Chốt plan và wireframe | Không | Bạn duyệt Plan 053 + wireframe |
| `1` | Storybook UI `Khối & lớp`: table, form, status, warning placeholder | Không gọi API; fixture tĩnh | Bạn duyệt layout/interaction nhóm khối-lớp |
| `2` | Storybook UI `Môn học`: subject table/form và applicability form | Không gọi API; fixture tĩnh | Bạn duyệt loại môn/phạm vi áp dụng |
| `3` | Storybook UI `Lớp-môn`: context năm học/lớp/học kỳ, table, status/conflict | Không gọi API; fixture tĩnh | Bạn duyệt flow gán môn |
| `4` | Tích hợp types/service/view cho khối và lớp | Contract hiện tại | Đã triển khai; validation + review nhóm khối-lớp |
| `5` | Tích hợp types/service/view cho môn, applicability và lớp-môn | Contract hiện tại; applicability create-only | Đã triển khai; validation + review nhóm môn/lớp-môn |
| `6` | Sĩ số, trung bình khối và warning thật | Chờ backend read contract/plan riêng | Đã audit và ghi blocker; chưa có FE implementation |

Mặc định sau mỗi phase agent sẽ dừng, báo file và Storybook story đã tạo,
không tự chuyển phase kế tiếp. Phase `1` là implementation đầu tiên sau khi
Plan 053 được duyệt; phase này không sửa `academicApi.ts`, không thêm route và
không gọi backend.

Checkpoint triển khai và hướng dẫn phục hồi sau crash được duy trì tại
[`HANDOFF.md`](../../../../HANDOFF.md).

## Requirement và nguồn đối chiếu

- Application version: `v2`.
- `document/application-doc/v2/ApplicationContext.md` — kiến trúc, authorization,
  lịch sử dữ liệu và nguyên tắc không suy luận role.
- `document/application-doc/v2/RequirementBaseline.md` — mã requirement của
  Academic Structure và Subject.
- `document/application-doc/v2/modules/01-AcademicStructureModule.md` —
  `FR/BR-GRADE`, `FR/BR-CLASS`, quy tắc trạng thái và cảnh báo sĩ số.
- `document/application-doc/v2/modules/03-AttendanceAndSubjectModule.md` —
  `FR/BR-SUBJECT`, loại môn và phạm vi áp dụng.
- `document/application-doc/v2/modules/02-EnrollmentAndTeachingModule.md` —
  ranh giới enrollment và nguồn cảnh báo sĩ số.
- `document/application-doc/v2/frontend-api/00-common-contract.md` — envelope,
  HTTP status, error và typed-service boundary.
- `document/application-doc/v2/frontend-api/02-academic-structure.md` — API
  hiện tại cho Grade, Class, Subject và Class Subject.
- `document/application-doc/v2/frontend-api/07-enums-and-known-drift.md` —
  enum canonical: `SchoolClassStatus`, `SubjectType`, `SubjectStatus`,
  `ApplicationScope`, `SubjectApplicabilityStatus`, `ClassSubjectStatus`.
- `document/application-doc/v2/data-model/02-AcademicCatalog.md` — quan hệ
  `academic_year`, `grade_level`, `school_class`, `subject`, `class_subject`.
- `document/application-doc/v2/data-model/03-StudentsAndEnrollment.md` — dữ
  liệu enrollment dùng khi xác định sĩ số.
- `FE/AGENTS.override.md` và các rule FE `00-foundation`, `01-auth-routing-security`,
  `02-domain-rules`, `03-api-data-boundaries`, `04-quality-documentation`.
- Plan 052 — cách tái sử dụng Academic Year/Semester list, dialog và service
  boundary hiện có.
- Backend controllers/DTO hiện tại trong
  `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/` và
  `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/enrollment/`.

## Hiện trạng và contract constraints

- Plan 052 đã tạo `academicApi.ts`, `academic.ts`, các shared component và
  route dưới `/v2`; implementation hiện tại đang là thay đổi có sẵn của user,
  cần giữ nguyên behavior.
- API Grade hiện có `GET/POST/PUT/DELETE /api/v2/grades`.
- API Class hiện có `GET /api/v2/classes?academicYearId?`, `POST`, `PUT`,
  `POST /{classId}/close`, `DELETE`.
- API Subject hiện có `GET /api/v2/subjects?status?`, `POST`, `PUT` và
  `POST /api/v2/subjects/{subjectId}/applicabilities`.
- API Class Subject hiện có `GET /api/v2/classes/{classId}/subjects?semesterId`,
  `POST /api/v2/class-subjects` và `PUT /api/v2/class-subjects/{id}`.
- Các list response hiện tại là array, không có server pagination và không
  chứa display name của entity liên quan. FE phải resolve tên từ context đã
  tải, không gọi endpoint giả.
- Auth response/JWT chưa expose role/capability. Navigation sẽ là tĩnh; FE
  không suy luận `ADMIN`/`ACADEMIC_OFFICE` và không dùng hidden button làm
  security boundary. `403` giữ session và hiển thị access denied.

### Contract gap cần được duyệt trước khi gọi Plan 053 hoàn tất

1. `ResGradeLevelDTO` không có số lớp/số học sinh theo năm học.
2. `ResSchoolClassDTO` không có `activeStudentCount`, `gradeAverage` hoặc
   warning payload. `EnrollmentCapacityService` hiện chỉ trả
   `ResCapacityWarningDTO` trong response của thao tác enrollment, không có
   API đọc cảnh báo cho danh sách lớp.
3. Subject có endpoint tạo applicability nhưng chưa có endpoint list/update/
   deactivate applicability. FE không thể dựng màn hình reload đầy đủ danh
   sách áp dụng hoặc tự quản lý trạng thái lịch sử.

Plan chỉ cho phép hiển thị sĩ số/cảnh báo và applicability khi có dữ liệu từ
   contract thật. Không gọi `/classes/{classId}/students` cho từng dòng để giả
   lập summary, không tính lại trung bình khối ở FE và không tự tạo endpoint.

## Phạm vi in-scope

### 1. Typed academic catalog boundary

Mở rộng `academicApi.ts` và types hiện có, dùng `apiClient`; không gọi raw
`fetch` từ view/component.

Các type wire cần bám đúng backend:

```ts
type SchoolClassStatus = 'PLANNED' | 'ACTIVE' | 'CLOSED'
type SubjectType = 'ACADEMIC' | 'SKILL'
type SubjectStatus = 'ACTIVE' | 'INACTIVE'
type ApplicationScope = 'GRADE' | 'CLASS'
type SubjectApplicabilityStatus = 'ACTIVE' | 'INACTIVE'
type ClassSubjectStatus = 'ACTIVE' | 'INACTIVE' | 'COMPLETED'
```

Shape tối thiểu:

```ts
interface GradeLevel {
  id: number
  code: string
  name: string
  gradeLevel: 6 | 7 | 8 | 9
  displayOrder: number
  nextGradeId: number | null
  active: boolean
  description: string | null
}

interface SchoolClass {
  id: number
  academicYearId: number
  gradeLevelId: number
  classCode: string
  className: string | null
  capacity: number | null
  status: SchoolClassStatus
}

interface Subject {
  id: number
  code: string
  name: string
  subjectType: SubjectType
  applicationScope: ApplicationScope
  status: SubjectStatus
}

interface SubjectApplicability {
  id: number
  subjectId: number
  semesterId: number
  scopeType: ApplicationScope
  gradeLevelId: number | null
  classId: number | null
  status: SubjectApplicabilityStatus
}

interface ClassSubject {
  id: number
  classId: number
  subjectId: number
  semesterId: number
  status: ClassSubjectStatus
}
```

Service methods dự kiến:

| Method | Endpoint | Mục đích FE |
|---|---|---|
| `GET` | `/api/v2/grades` | Load danh sách khối |
| `POST` | `/api/v2/grades` | Tạo khối |
| `PUT` | `/api/v2/grades/{id}` | Sửa/kích hoạt/ngừng sử dụng khối bằng `active` |
| `DELETE` | `/api/v2/grades/{id}` | Xóa khối chưa được tham chiếu |
| `GET` | `/api/v2/classes?academicYearId={id}` | Load lớp theo năm học |
| `POST` | `/api/v2/classes` | Tạo lớp |
| `PUT` | `/api/v2/classes/{id}` | Sửa metadata/trạng thái lớp |
| `POST` | `/api/v2/classes/{id}/close` | Đóng lớp có confirmation |
| `DELETE` | `/api/v2/classes/{id}` | Xóa lớp chưa phát sinh dữ liệu |
| `GET` | `/api/v2/subjects?status={status}` | Load/filter môn |
| `POST` | `/api/v2/subjects` | Tạo môn |
| `PUT` | `/api/v2/subjects/{id}` | Sửa môn/trạng thái |
| `POST` | `/api/v2/subjects/{id}/applicabilities` | Tạo cấu hình áp dụng |
| `GET` | `/api/v2/classes/{classId}/subjects?semesterId={id}` | Load lớp-môn |
| `POST` | `/api/v2/class-subjects` | Gán môn cho lớp trong học kỳ |
| `PUT` | `/api/v2/class-subjects/{id}` | Đổi trạng thái lớp-môn |

Không thêm service method cho endpoint applicability list/count/warning khi
backend chưa có contract.

### 2. Route và screen flow

Thêm navigation tĩnh vào v2 shell:

```text
Khối, lớp & môn học -> /v2/academic-catalog/grades
```

Route dự kiến:

```text
/v2/academic-catalog/grades
/v2/academic-catalog/classes
/v2/academic-catalog/subjects
/v2/academic-catalog/class-subjects
```

Các route là các màn list/context; form tạo/sửa và confirmation vẫn là dialog.
Không thêm role metadata cho tới khi auth contract expose capability thật.

Flow chính:

```text
Khối & lớp
  ├─ Khối: list → create/edit → active toggle/delete confirmation
  └─ Lớp: chọn năm học → list/filter → create/edit → close/delete confirmation

Môn học
  ├─ Môn: list/filter → create/edit → active/inactive
  ├─ Applicability: chọn môn → chọn học kỳ + khối/lớp → create
  └─ Lớp-môn: chọn năm học → lớp → học kỳ → list → add subject/status
```

### 3. Màn hình Khối

- Hiển thị bảng khối theo `displayOrder` từ API.
- Cột: tên, mã, cấp 6–9, khối tiếp theo, active, mô tả, thao tác.
- Dialog tạo/sửa gồm `code`, `name`, `gradeLevel`, `nextGradeId`, `active`,
  `description`; `displayOrder` được giữ trong model nhưng không hiển thị trên
  form UI.
- `nextGradeId` chỉ cho chọn khối khác chính nó; FE kiểm tra sớm chu trình
  đơn giản nhưng backend vẫn là nguồn validation cuối.
- `active=false` hiển thị rõ là ngừng dùng cho dữ liệu mới, không xóa lịch sử.
- Xóa dùng confirmation; `409`/ràng buộc tham chiếu hiển thị dưới dạng lỗi
  nghiệp vụ, không tự fallback sang hard delete khác.
- Cột thống kê số lớp/số học sinh chỉ hiển thị khi backend trả field/endpoint
  được phê duyệt; trước đó dùng trạng thái `Chưa có dữ liệu thống kê`, không
  tự suy luận từ tên lớp.

### 4. Màn hình Lớp và cảnh báo sĩ số

- Bắt buộc chọn `academicYearId` trước khi gọi list theo năm học.
- Hiển thị context năm học, bộ lọc cục bộ theo khối và status trên full list
  hiện tại.
- Cột: mã lớp, tên, khối, sĩ số, trạng thái, ghi chú, thao tác.
- Với contract hiện tại, `sĩ số active` và `cảnh báo` ở trạng thái unavailable;
  không gọi N+1 endpoint để giả lập.
- Khi có warning payload thật, dùng warning non-blocking theo `BR-CLASS-005`:
  hiển thị số học sinh hiện tại, trung bình khối và lý do lệch quá 20%; warning
  không khóa edit/close.
- Dialog tạo lớp gồm `academicYearId`, `gradeLevelId`, `classCode`,
  `className`, `capacity`, `status`.
- Dialog sửa lớp không cho đổi `academicYearId`; nếu lớp đã có enrollment,
  backend quyết định việc đổi khối và FE hiển thị `409` rõ ràng.
- `CLOSED` là read-only; close/delete đều có confirmation và xử lý `409`.

### 5. Màn hình Môn học và applicability

- List môn với filter cục bộ theo `subjectType`, `applicationScope` và status;
  request `status` chỉ dùng khi cần tải subset từ endpoint hiện có.
- Cột: mã, tên, loại (`CHÍNH KHÓA`/`KỸ NĂNG`), phạm vi (`Theo khối`/`Theo lớp`),
  trạng thái (`Đang giảng dạy`/`Tạm ngưng giảng dạy`), thao tác; wire value
  vẫn giữ `ACADEMIC`/`SKILL`, `ACTIVE`/`INACTIVE`.
- Dialog tạo/sửa gồm `code`, `name`, `subjectType`, `applicationScope`, `status`.
- Không hiển thị `NORMAL`; dùng đúng wire value `ACADEMIC`.
- Subject `SKILL` được gắn nhãn rõ là môn kỹ năng/optional theo requirement;
  FE không tự tính hoặc trình bày điểm trung bình.
- Dialog “Cấu hình áp dụng” gồm `semesterId`, `scopeType`, và một trong:
  `gradeLevelId` hoặc `classId` tùy `scopeType`/`applicationScope`.
- Nếu backend chỉ trả response của lệnh create, UI hiển thị kết quả vừa tạo và
  cảnh báo “chưa có API tải lại danh sách applicability”; không giả lập bảng
  lịch sử đầy đủ.
- Không cho cấu hình môn `INACTIVE` vào lớp; lỗi `409` từ backend hiển thị tại
  form/list context.

### 6. Màn hình Class Subject

- Chọn năm học, lớp và học kỳ; học kỳ chỉ lấy trong năm học đã chọn.
- Gọi `GET /classes/{classId}/subjects?semesterId={semesterId}` sau khi đủ
  context; không gọi với query thiếu `semesterId`.
- Hiển thị tên/mã môn bằng cách join `subjectId` với danh sách Subject đã load;
  nếu không resolve được thì hiển thị ID kỹ thuật và trạng thái dữ liệu thiếu.
- Nút “Thêm môn cho lớp” mở dialog chọn môn `ACTIVE` và tạo record `ACTIVE`;
  không cho chọn môn chưa áp dụng để tránh request chắc chắn `409`.
- Action status theo record: `ACTIVE`, `INACTIVE`, `COMPLETED`; không có delete
  vì backend không expose delete và lịch sử lớp-môn cần được giữ.
- UI hiển thị diễn giải trạng thái lớp-môn trực tiếp bên trong tag bằng tiếng
  Việt, không lặp lại diễn giải ở bên cạnh; dialog dùng nhãn phạm vi `Theo khối`
  hoặc `Theo lớp` cho môn được chọn.
- Nếu môn chưa được cấu hình applicability cho khối/lớp + học kỳ, hiển thị
  conflict message và liên kết người dùng về flow “Cấu hình áp dụng”.
- `semester CLOSED` và `class CLOSED` hiển thị read-only; backend vẫn là nguồn
  quyết định cuối.

### 7. State, validation và error behavior

Tất cả list/dialog phải có loading, empty, success, validation, `401`, `403`,
`404`, `409` và lỗi server theo shared components Plan 051/052.

Validation sớm ở FE:

- required, max length và enum cho toàn bộ form;
- `displayOrder`/`capacity`/ID dương khi có giá trị;
- `gradeLevel` trong 6–9;
- class code duy nhất trong năm học là backend rule;
- `class_subject` duy nhất theo class + subject + semester là backend rule;
- applicability phải có đúng field theo `GRADE` hoặc `CLASS`;
- không làm giả việc kiểm tra subject applicability nếu list endpoint chưa có.

### 8. Component, test và Storybook theo từng phase

Component presentation dự kiến:

```text
GradeTable.vue / GradeDialog.vue
SchoolClassTable.vue / SchoolClassDialog.vue
CapacityWarningBanner.vue
SubjectTable.vue / SubjectDialog.vue
SubjectApplicabilityDialog.vue
ClassSubjectTable.vue / ClassSubjectDialog.vue
```

Views sở hữu route context, selected entity, dialog mode, loading/error state
và service calls. Components chỉ nhận props typed và emit interaction.

Storybook là checkpoint chính của Plan 053, dùng state deterministic và không
gọi backend:

- Phase 1: list khối/lớp, create/edit dialog, `PLANNED`/`ACTIVE`/`CLOSED`,
  loading/empty/forbidden, warning sĩ số dạng fixture và trạng thái “chưa có
  contract thống kê”.
- Phase 2: subject list/form, `ACADEMIC`/`SKILL`, `GRADE`/`CLASS`,
  `ACTIVE`/`INACTIVE`, applicability create form và conflict state.
- Phase 3: selector context, class-subject list, `ACTIVE`/`INACTIVE`/
  `COMPLETED`, closed read-only và lỗi chưa cấu hình applicability.

Mỗi phase phải có story đủ để review trước khi nối service/view. Không dùng
Storybook để giả lập backend authorization hoặc biến fixture thành API contract.

## Out-of-scope

- Backend controller/service/DTO, database, migration và seed data.
- Tự tạo API thống kê sĩ số, API list applicability hoặc API CRUD ngoài contract.
- Tự tính sĩ số trung bình khối, cảnh báo vượt 20% hoặc official academic result
  trong FE.
- Enrollment CRUD, chuyển lớp, giáo viên/GVCN/GVBM, scorebook, attendance.
- Role-aware navigation/route guard dựa trên suy luận client.
- Xóa class-subject hoặc xóa applicability khi backend không expose operation.
- Server-side search/filter/pagination chưa có trong contract.
- Refactor Student legacy hoặc thay đổi behavior của Plan 052.

## Khu vực/file dự kiến thay đổi khi được duyệt

```text
FE/src/types/academic.ts                         # mở rộng types v2 catalog
FE/src/services/academicApi.ts                  # typed methods cho catalog
FE/src/services/academicApi.spec.ts              # URL/query/body/status tests
FE/src/components/GradeTable.vue
FE/src/components/GradeTable.spec.ts
FE/src/components/GradeTable.stories.ts
FE/src/components/GradeDialog.vue
FE/src/components/GradeDialog.spec.ts
FE/src/components/SchoolClassTable.vue
FE/src/components/SchoolClassTable.spec.ts
FE/src/components/SchoolClassTable.stories.ts
FE/src/components/SchoolClassDialog.vue
FE/src/components/SchoolClassDialog.spec.ts
FE/src/components/CapacityWarningBanner.vue
FE/src/components/CapacityWarningBanner.spec.ts
FE/src/components/SubjectTable.vue
FE/src/components/SubjectTable.spec.ts
FE/src/components/SubjectTable.stories.ts
FE/src/components/SubjectDialog.vue
FE/src/components/SubjectDialog.spec.ts
FE/src/components/SubjectApplicabilityDialog.vue
FE/src/components/SubjectApplicabilityDialog.spec.ts
FE/src/components/ClassSubjectTable.vue
FE/src/components/ClassSubjectTable.spec.ts
FE/src/components/ClassSubjectDialog.vue
FE/src/components/ClassSubjectDialog.spec.ts
FE/src/views/GradeListView.vue
FE/src/views/SchoolClassListView.vue
FE/src/views/SubjectListView.vue
FE/src/views/ClassSubjectListView.vue
FE/src/views/*.spec.ts                              # chỉ các view bị thêm
FE/src/router/index.ts
FE/src/views/AuthenticatedV2ShellView.vue
FE/src/styles.css                                  # chỉ class layout cần thiết
```

Danh sách trên là vùng dự kiến. Khi triển khai phải kiểm tra file hiện có và
không tạo abstraction/store mới nếu không có nhu cầu thực tế.

## API, schema và data changes

- Không thay đổi API/backend/schema trong Plan 053.
- Không ghi các endpoint thống kê/applicability list còn thiếu vào
  `FrontendApiGuide.md` như current contract.
- Nếu muốn đáp ứng đầy đủ FR-GRADE-004/FR-CLASS-008 và reload applicability,
  cần một backend plan/CR riêng hoặc amendment được người dùng duyệt trước.

## Test plan và validation dự kiến

### Unit/service tests

- Method/path/query/body cho toàn bộ Grade, Class, Subject, Applicability và
  Class Subject endpoint.
- Enum mapping đúng wire value, đặc biệt `ACADEMIC`, `GRADE`, `CLASS`.
- `academicYearId`/`semesterId` context không bị thiếu hoặc đổi nhầm khi
  chuyển selector.
- Form validation cho field required, length, positive, grade range và
  applicability scope.
- Action matrix của Grade active, Class PLANNED/ACTIVE/CLOSED, Subject
  ACTIVE/INACTIVE và Class Subject ACTIVE/INACTIVE/COMPLETED.
- Warning sĩ số là non-blocking khi payload có thật; unavailable state không
  gọi N+1 hoặc tự tính.
- `400/403/404/409` hiển thị đúng FormAlert/PageState; `401` giữ semantics
  clear session/redirect từ shared client.
- Dialog cancel giữ filter, selector context và list state.

### Mandatory quality gates sau khi có implementation

```bash
cd FE
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

Documentation-only planning/wireframe artifact không cần chạy FE build/test;
chỉ chạy các gate trên sau khi source FE được duyệt và triển khai.

## Acceptance criteria cho Plan 053

### Approval gate theo phase

1. Phase 0 được duyệt khi user xác nhận plan, wireframe, phạm vi contract gap
   và thứ tự phase.
2. Phase 1 chỉ được coi là đạt khi Storybook thể hiện đúng nhóm `Khối & lớp`
   và user duyệt interaction/layout; chưa yêu cầu API.
3. Phase 2 chỉ bắt đầu sau approval Phase 1 và dừng ở Storybook subject.
4. Phase 3 chỉ bắt đầu sau approval Phase 2 và dừng ở Storybook class-subject.
5. Phase 4/5 chỉ bắt đầu sau approval tương ứng; mỗi phase phải chạy quality
   gates trước khi báo hoàn tất phase.
6. Phase 6 bị block cho tới khi backend cung cấp contract thống kê/cảnh báo.

### Có thể đạt bằng contract hiện tại

7. Người dùng authenticated mở được navigation và các route catalog dưới v2
   shell; route không tự gắn role suy đoán.
8. Xem/tạo/sửa/xóa khối đúng API; active/inactive được phân biệt rõ.
9. Chọn năm học, xem/tạo/sửa/đóng/xóa lớp đúng API và action matrix.
10. Xem/tạo/sửa môn học với đúng `ACADEMIC`/`SKILL`, `GRADE`/`CLASS` và status.
11. Tạo applicability theo học kỳ + khối/lớp bằng đúng endpoint hiện tại và
   không giả lập list applicability khi contract chưa có.
12. Xem/gán/đổi trạng thái class-subject theo class + semester context; không
   tạo record khi thiếu context.
13. `CLOSED`/`INACTIVE`/`COMPLETED` được hiển thị như historical/read-only khi
   business rule yêu cầu; lỗi `409` không bị nuốt.
14. Không có raw `fetch` trong presentation component; các state loading,
   empty, forbidden, validation và conflict có UI rõ ràng.

### Phụ thuộc contract backend, chưa được đánh dấu đạt bằng FE-only

15. Bảng khối hiển thị số lớp và số học sinh theo năm học.
16. Bảng lớp hiển thị sĩ số active, trung bình khối và cảnh báo mất cân bằng.
17. Màn hình subject có thể reload đầy đủ danh sách applicability và trạng
   thái đã cấu hình.

Ba tiêu chí trên chỉ được chuyển sang `PASS` sau khi backend contract/plan bổ
    sung được phê duyệt và triển khai.

## Rủi ro, assumption và cách giảm thiểu

- **Thiếu count/warning trong list response:** giữ placeholder unavailable và
  ghi rõ contract gap; không làm N+1 request hay suy luận từ class name.
- **Applicability create-only:** chỉ hỗ trợ create + hiển thị kết quả lệnh;
  không trình bày danh sách đầy đủ như thể đã persisted/readable.
- **Subject/Class Subject ID-only response:** view phải tải context trước và
  có fallback hiển thị ID nếu dữ liệu join thiếu; không gọi user/subject lookup
  giả.
- **Status mutation trực tiếp trong DTO:** form chỉ expose status ở nơi phù
  hợp contract; close lớp dùng endpoint riêng, không thay thế bằng PUT tùy ý.
- **Role chưa có trong auth:** navigation tĩnh; authorization do backend quyết
  định và `403` được hiển thị.
- **Thay đổi sẵn của Plan 052 trong worktree:** trước khi code phải kiểm tra
  diff và chỉ bổ sung catalog behavior, không reset/chỉnh sửa ngầm phần không
  liên quan.

## Approval record

- Phase `0–1–2–3` được user duyệt ngày 2026-08-27 để triển khai UI Storybook.
- Phase `4–5–6` được user duyệt ngày 2026-08-27 để tiếp tục tích hợp.
- Phase `4–5` đã hoàn tất với contract hiện tại và qua các quality gates.
- Phase `6` đã được audit nhưng chưa thể triển khai vì backend chưa có read
  contract cho sĩ số active, trung bình khối và warning payload.
- Checkpoint phục hồi sau crash được duy trì tại [`HANDOFF.md`](../../../../HANDOFF.md).
