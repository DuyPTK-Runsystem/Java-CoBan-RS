# Application Context

## 1. Purpose

This document is the entry point and primary context document for the training application.

The application is a small student-management system used to practice full-stack development with:

- Spring Boot for backend APIs and batch processing.
- Vue for the frontend.
- PrimeVue for UI components on the student-management screens.
- MySQL for persistent data storage.
- Storybook for isolated frontend component documentation/testing.

The required functional scope is:

1. User registration.
2. User login/logout.
3. Student list.
4. Search/filter students.
5. Sort student data.
6. Pagination.
7. Add student.
8. Update student.
9. Delete student with confirmation.
10. Export table data to CSV using a batch job.

This document describes project-wide rules only. Domain-specific rules belong in module documents.

---

## 2. Source of Truth Hierarchy

When implementation and documentation disagree, use the following priority:

1. Training assignment/screens and validation descriptions supplied by the project owner/trainer.
2. This documentation set.
3. API contract/code-level documentation.
4. Current implementation.

If the implementation differs from the assignment, update the implementation or explicitly update this documentation after confirming the intended behavior.

Any rule marked **TBD** is intentionally unresolved and must not be invented during implementation.

---

## 3. Documentation Map

- [User Module](modules/UserModule.md)
  - Login
  - Register
  - Logout
  - User validation
  - Backend/frontend design

- [Student Module](modules/StudentModule.md)
  - Student list
  - Search
  - Sorting
  - Pagination
  - Add/update/delete student
  - Backend/frontend design

- [Data Structure](DataStructure.md)
  - MySQL tables
  - Primary keys
  - Foreign keys
  - Constraints
  - JPA relationship guidance

---

## 4. High-Level Architecture

```text
Browser
  |
  v
Vue Application
  |
  | HTTP/JSON
  v
Spring Boot REST API
  |
  +--> User module
  |
  +--> Student module
  |
  +--> Spring Batch
  |
  v
MySQL
```

Frontend and backend are separate application layers.

### Frontend responsibilities

- Render screens and components.
- Handle client-side validation.
- Call REST APIs.
- Maintain UI state.
- Navigate between Login, Register, Student List, and Student Form screens.
- Display API validation/business errors.
- Use PrimeVue where required by the supplied screens.
- Provide Storybook stories for `LoginForm` and `RegisterForm`.
- Store the access token and UI-safe user summary in `sessionStorage` after login.
- Clear all authentication state from `sessionStorage` on logout or `401 Unauthorized`.
- Keep the authentication state on `403 Forbidden` and display an access-denied message.

### Backend responsibilities

- Expose REST APIs.
- Perform server-side validation.
- Apply business rules.
- Access MySQL.
- Return consistent HTTP responses/errors.
- Execute batch CSV export.

Client-side validation is a usability layer only. Every required validation rule must also be enforced on the server.

---

## 5. Technology Stack

### Backend

- Java version: project default / configured project version.
- Spring Boot.
- Spring Web.
- Spring Validation.
- Spring Data JPA.
- Spring Batch.
- MySQL driver.
- Other libraries included by the generated Spring Boot project as required.

Do not introduce additional frameworks unless there is a concrete requirement.

### Frontend

- Vue 3.
- Vite.
- PrimeVue.
- Storybook.
- Standard Vue Router should be used if routing is included in the generated Vue project.

State-management libraries are optional unless already included in the project.

### Database

- MySQL.
- Tables:
  - `user`
  - `student`
  - `student_info`

---

## 6. Functional Navigation

```text
Register
   |
   v
Login
   |
   | successful login
   v
Student List
   |
   +--> Add Student ------+
   |                      |
   +--> Edit Student -----+--> Student Form
   |                              |
   |                              +--> Save
   |                              +--> Back
   |
   +--> Delete Student
   |
   +--> Logout --> Login
```

### Login flow

```text
Login screen
  -> validate username/password
  -> POST login API
  -> success: Student List
  -> failure: display error
```

### Register flow

```text
Register screen
  -> validate fields
  -> check password confirmation
  -> POST register API
  -> save user
  -> return/navigate to Login
```

### Student management flow

```text
Student List
  -> search/sort/page
  -> Add Student -> Student Form -> create
  -> Edit -> Student Form -> update
  -> Delete -> confirmation -> delete
```

---

## 7. Required APIs

The assignment explicitly requires APIs for:

- Login
- Get List Student
- Get Student Detail
- Register Student
- Update Student
- Delete Student

The project also requires user registration from the supplied Register screen. Therefore the backend needs a user-registration endpoint even though the assignment's short API list does not name it separately.

The current API contract uses the `/api/v1` prefix and the endpoint paths documented in the User and Student modules.

---

## 8. Validation Policy

Validation must exist on both:

```text
Vue client
+
Spring Boot server
```

The backend is authoritative.

General rules supplied by the screens include:

- Username required.
- Username maximum length 20.
- Username accepts only 1-byte characters.
- Password required.
- Password minimum length 6.
- Password maximum length 15.
- Password accepts only 1-byte characters.
- Password confirmation must match password.
- Birthday/date values must be valid.
- Student code maximum length 10.
- Student name maximum length 35.
- Address maximum length 255.
- UI date display format is `dd-mm-yyy`; API date values use `yyyy-MM-dd`.

Where a screen does not explicitly specify required/optional behavior, follow the database `NOT NULL` constraints in `DataStructure.md`.

---

## 9. Frontend Component Strategy

At minimum:

```text
src/
├── components/
│   ├── LoginForm.vue
│   └── RegisterForm.vue
├── views/
│   ├── LoginView.vue
│   ├── RegisterView.vue
│   ├── StudentListView.vue
│   └── StudentFormView.vue
├── services/
│   ├── userApi.ts
│   └── studentApi.ts
└── router/
```

Recommended separation:

- `views`: page-level orchestration and routing.
- `components`: reusable UI.
- `services`: HTTP communication.
- validation logic: shared helpers/schema if duplication appears.

Do not place backend API calls directly throughout presentation markup.

---

## 10. Backend Package Strategy

Recommended logical structure:

```text
com.example.app
├── user
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
├── student
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
├── batch
└── common
```

This is a modular monolith. Do not split the assignment into microservices.

---

## 11. Batch Requirement

The assignment requires a batch process that:

```text
read information from database tables
    ->
transform/map rows
    ->
write CSV file
```

Recommended scope:

- Read from `user`, `student`, and `student_info`, either as separate exports or a clearly defined joined export.
- Use Spring Batch.
- CSV output directory/name is **TBD** unless already specified elsewhere.
- Batch trigger mechanism (manual endpoint, scheduler, command line, etc.) is **TBD**.

Do not silently choose a business-critical batch schedule without an explicit requirement.

---

## 12. UI Requirements

### Login/Register

- Implement reusable `LoginForm` and `RegisterForm` components.
- Add Storybook stories for both.
- Screens should follow the supplied training mockups sufficiently for behavior and layout.

### Student screens

The supplied specification explicitly states to use PrimeVue components.

Recommended PrimeVue components:

- DataTable
- Column
- InputText
- DatePicker/Calendar depending on installed PrimeVue version
- Button
- ConfirmDialog
- Paginator, or DataTable built-in pagination

PrimeVue DataTable is particularly appropriate for sorting, pagination, and list display.

---

## 13. Error Handling

Backend responses use the `RestResponse` envelope. Successful controller bodies are wrapped by `FormatRestResponse`, implemented as `@RestControllerAdvice` and `ResponseBodyAdvice`. Student page data is placed in `RestResponse.data`.

The current response shape is:

```json
{
  "statusCode": 200,
  "error": null,
  "message": "CALL API SUCCESS",
  "data": {}
}
```

Errors use the same envelope with the HTTP status, reason phrase in `error`, and business or validation details in `message`.

Authentication behavior:

- Anonymous requests and expired, malformed, or invalid-signature bearer tokens return `401 Unauthorized`.
- A `403 Forbidden` response represents an authenticated user without permission and must not clear frontend authentication state.

Frontend must display meaningful errors rather than relying only on console output.

---

## 14. Non-Goals

Unless separately required, this assignment does not require:

- Microservices.
- OAuth2/OIDC.
- Distributed cache.
- Message broker.
- Kubernetes.
- Complex role/permission management.

Authentication uses a stateless JWT access token. The frontend stores the token and a UI-safe user summary in `sessionStorage`; the backend does not revoke the token on logout.

---

## 15. Open Decisions

The following must be confirmed before treating them as source-of-truth rules:

- Batch CSV file path/name and whether exports are separate or joined.
- Whether a production migration/schema script is required instead of the current JPA-managed schema.

Until confirmed, code must avoid assumptions that are difficult to change.
