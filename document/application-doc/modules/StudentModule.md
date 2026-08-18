# Student Module

## 1. Scope

The Student module owns:

- Student list.
- Search.
- Sorting.
- Pagination.
- Student creation.
- Student update.
- Student deletion.
- Student code generation.

Primary tables:

- `student`
- `student_info`

Screens:

- Student List.
- Student Register/Update form.

The student screens should use PrimeVue components.

---

# 2. Student Aggregate

Application-level representation:

```text
Student
├── studentId
├── studentCode
├── studentName
└── info
    ├── infoId
    ├── dateOfBirth
    ├── address
    └── averageScore
```

Persistence is split between `student` and `student_info`.

The UI/API should normally treat this as one student record rather than forcing the frontend to manage two independent database tables.

---

# 3. Student List Screen

## 3.1 Header

Displays:

- Logo.
- `Welcome, {userName}`.
- Logout link.

---

## 3.2 Search controls

Search fields:

| Field | Max length/source |
|---|---|
| Student Code | 10, `student.student_code` |
| Student Name | 20, `student.student_name` |
| Birthday | `student_info.date_of_birth` |

Action:

- Search button.

Birthday validation:

- Maximum display/input length according to supplied screen.
- Must be a valid date.
- UI display format is `dd-mm-yyy`; API date values use `yyyy-MM-dd`.

Recommended behavior:

- Empty fields mean "no filter for this field".
- Multiple populated criteria should use AND semantics.

The AND behavior is a recommended default and should be changed if the trainer specifies OR semantics.

---

## 3.3 Table

Columns:

1. No
2. Code
3. Name
4. Birthday
5. Address
6. Score
7. Edit
8. Delete

Data mapping:

| UI column | Source |
|---|---|
| Code | `student.student_code` |
| Name | `student.student_name` |
| Birthday | `student_info.date_of_birth` |
| Address | `student_info.address` |
| Score | `student_info.average_score` |

`No` is a row ordinal/display value, not a database primary key.

Initial list:

- Display student information.
- Page size: 10 records.

---

## 3.4 Sorting

The supplied screen requires ascending/descending sorting when the relevant table headers are clicked.

Sortable business columns:

- Code.
- Name.
- Birthday.
- Address.
- Score.

Recommended implementation:

```text
PrimeVue DataTable sort event
    ->
query parameters
    ->
Spring API
    ->
database ORDER BY
```

Sorting should be performed server-side together with pagination.

Only an allow-list of supported sort fields may be accepted by the backend.

---

## 3.5 Pagination

Required controls:

- first
- previous
- page number
- next
- last

Click behavior:

- First → first page.
- Prev → previous page.
- Page number → selected page.
- Next → next page.
- Last → last page.

Recommended implementation is PrimeVue DataTable lazy/server-side pagination with page size 10.

Backend page numbering can be zero-based while the UI displays one-based page numbers.

---

# 4. Student Form

One form is used for both:

- Add Student.
- Update Student.

Fields:

| Item | Type | Source |
|---|---|---|
| Student Id | label/read-only | `student.student_id` |
| Student Code | input | `student.student_code` |
| Generate Code | button | action |
| Student Name | input | `student.student_name` |
| Birthday | date input | `student_info.date_of_birth` |
| Address | input | `student_info.address` |
| Average Score | input | `student_info.average_score` |
| Back | button | navigation |
| Save | button | action |

---

# 5. Add Mode

Entered from `Add Student` on Student List.

Initial behavior specified by the screen:

- Logged-in username is not shown on this mode according to the supplied note.
- Student Id is not displayed.
- Student Code is disabled/read-only.
- Generate Code is enabled.
- Student Name is editable.
- Birthday is editable.
- Address is editable.
- Average Score is editable.
- Back is enabled.
- Save is enabled.

## Generate Code

When clicked:

```text
"STU" + random number
```

The generated code is then used when registering the student.

The current backend format is `STU` plus 7 random digits, for a total of 10 characters.

Recommended backend rule:

- The final code must be unique; `student_code` has a database uniqueness constraint in the current application model.
- Do not rely exclusively on frontend random generation.

The backend validates generated and client-provided codes against the uniqueness rule.

## Save in Add mode

```text
validate input
  ->
create Student + StudentInfo
  ->
single transactional backend operation
  ->
return to Student List or report success
```

The assignment states that generated student information is registered to the database.

---

# 6. Edit Mode

Entered from an Edit link on Student List.

Specified behavior:

- Show current logged-in username.
- Student Id is displayed and disabled.
- Student Code is displayed and disabled.
- Generate Code is disabled.
- Student Name contains current value.
- Birthday contains current value.
- Address contains current value.
- Average Score contains current value.
- Back enabled.
- Save enabled.

Save behavior:

```text
validate
  ->
update persisted student data
  ->
update database
```

`studentId` identifies the record being updated.

Client-provided `studentId` must not be allowed to change an unrelated student accidentally.

---

# 7. Delete Student

From Student List:

```text
Delete clicked
  ->
show confirmation popup
  ->
cancel: do nothing
  ->
confirm: call delete API
  ->
refresh current list
```

PrimeVue `ConfirmDialog` is recommended.

Deleting a student must handle its `student_info` row consistently in the same logical operation.

The exact FK cascade strategy is documented in `DataStructure.md`.

---

# 8. Back

Student Form `Back`:

```text
Student Form
  ->
Student List
```

Back does not save unsaved changes.

Optional unsaved-change confirmation is not required by the supplied assignment.

---

# 9. Logout

From student screens:

```text
Logout
  ->
clear authenticated state
  ->
Login screen
```

---

# 10. Backend Design

Recommended classes:

```text
student/
├── controller/
│   └── StudentController
├── service/
│   └── StudentService
├── repository/
│   ├── StudentRepository
│   └── StudentInfoRepository
├── entity/
│   ├── Student
│   └── StudentInfo
└── dto/
    ├── StudentRequest
    ├── StudentResponse
    └── StudentSearchRequest
```

---

## 10.1 Controller

Responsibilities:

- REST request/response.
- Validation boundary.
- Parse pagination/sort/search parameters.
- Delegate to service.

---

## 10.2 Service

Responsibilities:

- Create/update/delete student.
- Generate/validate student code if generation is implemented server-side.
- Coordinate `student` and `student_info`.
- Apply transaction boundaries.

Create/update/delete across both tables should be transactional.

---

## 10.3 Repository

Responsibilities:

- CRUD persistence.
- Search/filter.
- Pageable queries.
- Sorting.

Spring Data `Pageable` is recommended.

For dynamic optional filters, valid approaches include:

- `JpaSpecificationExecutor`.
- Explicit JPQL query with optional conditions.
- QueryDSL only if already included/approved.

Do not add unnecessary query libraries for this small assignment.

---

# 11. Recommended REST Contract

## Get students

```http
GET /api/students
```

Example:

```text
/api/students?page=0&size=10&studentCode=STU001&studentName=Nguyen&birthday=2012-04-22&sort=studentName,asc
```

Response:

```json
{
  "content": [
    {
      "studentId": 1,
      "studentCode": "STU001",
      "studentName": "Nguyen Van B",
      "dateOfBirth": "1989-10-11",
      "address": "192 Truong Son-Q1",
      "averageScore": 5.6
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

The exact envelope may use Spring's `Page` serialization or a custom page DTO. Prefer a custom stable DTO if API stability matters.

---

## Create student

```http
POST /api/students
```

Example request:

```json
{
  "studentCode": "STU1234",
  "studentName": "Nguyen Van A",
  "dateOfBirth": "2012-04-22",
  "address": "Ho Chi Minh City",
  "averageScore": 8.5
}
```

---

## Update student

```http
PUT /api/students/{studentId}
```

Student Id and Student Code should not be editable from the specified Edit screen.

---

## Delete student

```http
DELETE /api/students/{studentId}
```

---

## Generate code

Two valid designs exist:

1. Generate client-side, then backend validates uniqueness.
2. Generate server-side, e.g. `POST /api/students/code`.

Server-side generation is safer if uniqueness is a requirement.

The assignment specifies the format but not an API for generation, so this endpoint is optional.

---

# 12. Frontend Design

Recommended structure:

```text
src/
├── views/
│   ├── StudentListView.vue
│   └── StudentFormView.vue
├── components/
│   ├── StudentSearchForm.vue
│   ├── StudentTable.vue
│   └── StudentForm.vue
└── services/
    └── studentApi.ts
```

For this assignment, components may be merged if splitting them adds no value. The important boundary is keeping HTTP access outside deeply nested presentation markup.

---

# 13. PrimeVue Design

Recommended mapping:

```text
Student List
├── InputText: student code
├── InputText: student name
├── DatePicker/Calendar: birthday
├── Button: Search
├── Button: Add Student
├── DataTable
│   ├── sortable columns
│   └── paginator
└── ConfirmDialog: Delete
```

Student Form:

```text
InputText
DatePicker/Calendar
InputNumber (optional for score)
Button
```

The exact component name for dates depends on installed PrimeVue version.

---

# 14. Validation

At minimum, align with database/screen limits:

- Student Code max 10.
- Student Name max 20.
- Address max 255.
- Birthday must be valid.
- Required fields follow database `NOT NULL`.
- Average Score is numeric.

The valid numeric range for `average_score` is not specified. Do not invent a 0–10 rule unless confirmed, even though that would be a common school-score range.

---

# 15. Student Module Invariants

1. Student List displays at most 10 records per requested page by default.
2. Search can use student code, student name, and birthday.
3. Supported table columns can sort ascending/descending.
4. Add Student opens the Student Form in add mode.
5. Edit opens the Student Form in edit mode.
6. Delete requires confirmation.
7. Add mode does not display Student Id.
8. Student Code is not manually editable in add mode; use Generate Code.
9. Generated code begins with `STU`.
10. Edit mode does not allow Student Id or Student Code to be changed.
11. Save in edit mode updates the existing database record.
12. Back returns to Student List.
13. Create/update/delete involving both tables must be transactional.
