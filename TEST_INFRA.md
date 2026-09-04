# TEST_INFRA: End-to-End & Integration Testing Infrastructure

## 1. Architecture Overview

The testing infrastructure of **Java-CoBan** (Phân hệ Quản lý Học sinh & Học vụ V2) is structured to provide rigorous, multi-tiered quality assurance across both frontend and backend domains, with specialized focus on end-to-end integration contracts.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Vitest Test Runner                              │
│                    (v3.2.7 / jsdom environment)                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
       ┌────────────────────────────┼────────────────────────────┐
       ▼                            ▼                            ▼
┌──────────────┐             ┌──────────────┐             ┌──────────────┐
│ Unit / View  │             │ 4-Tier E2E   │             │ Backend CI   │
│ Component    │             │ Integration  │             │ JUnit 5 /    │
│ Tests        │             │ Suite        │             │ Spring Boot  │
│ (FE/src/...) │             │ (src/tests/) │             │ (BE/...)     │
└──────────────┘             └──────────────┘             └──────────────┘
```

### Core Frameworks & Tooling
- **Frontend Test Runner**: Vitest `v3.2.7` with `@vitest/coverage-v8`.
- **DOM Simulation Environment**: `jsdom` `v26.1.0`.
- **Component Mounting & Interactivity**: `@vue/test-utils` `v2.4.11`.
- **Component Stubs**: PrimeVue v4 component stubs located in `@/test/stubs` (`Button`, `InputText`, `Password`, `DatePicker`, `DataTable`, `Dialog`).
- **Backend Test Runner**: JUnit 5, Spring Boot Test, MockMvc, H2 database (MySQL compatibility mode).
- **Code Quality & Static Analysis**: Checkstyle 10+, PMD 7+, ESLint 9+ with `typescript-eslint`.

---

## 2. Directory Structure & Layout

```
FE/
├── src/
│   ├── tests/
│   │   └── e2e/                                    <-- 4-Tier E2E Integration Suite
│   │       ├── fixtures.ts                         <-- Shared deterministic fixtures & contracts
│   │       ├── tier1-feature-coverage.spec.ts      <-- Tier 1: Feature Coverage (>=5/feat)
│   │       ├── tier2-boundary-corner.spec.ts       <-- Tier 2: Boundary & Corner cases (>=5/feat)
│   │       ├── tier3-cross-feature-combinations.spec.ts <-- Tier 3: Cross-Feature workflows
│   │       └── tier4-real-world-scenarios.spec.ts  <-- Tier 4: Real-World persona scenarios
│   ├── views/                                      <-- Route views & view-level spec files
│   │   ├── AuthenticatedV2ShellView.vue
│   │   ├── AuthenticatedV2ShellView.spec.ts
│   │   ├── LoginView.vue
│   │   ├── LoginView.spec.ts
│   │   ├── StudentListView.vue
│   │   ├── StudentFormView.vue
│   │   └── StudentDetailView.vue
│   ├── components/                                 <-- Reusable UI components & specs
│   │   ├── StudentTable.vue
│   │   ├── StudentTable.spec.ts
│   │   ├── StudentForm.vue
│   │   ├── StudentForm.spec.ts
│   │   └── StudentSearchForm.vue
│   ├── services/                                   <-- Typed API clients & unit specs
│   │   ├── studentApi.ts
│   │   ├── authSession.ts
│   │   └── apiClient.ts
│   └── test/
│       └── stubs/                                  <-- PrimeVue mock stubs
└── vite.config.ts
```

---

## 3. 4 Tiers Testing Model

The E2E test suite adheres to the 4 Tiers Testing Model specified in the Project Pattern and documented in `PROJECT.md`:

```
                  ┌─────────────────────────────────────┐
                  │ Tier 4: Real-World Scenarios        │  (Persona Walkthroughs)
                  ├─────────────────────────────────────┤
                  │ Tier 3: Pairwise & Cross-Feature    │  (Chained Multi-Module Workflows)
                  ├─────────────────────────────────────┤
                  │ Tier 2: Boundary & Corner Cases     │  (Validation, 409 Conflict, Edges)
                  ├─────────────────────────────────────┤
                  │ Tier 1: Feature Coverage (>=5/feat) │  (Category-Partition Verification)
                  └─────────────────────────────────────┘
```

### Tier 1 — Feature Coverage Tests (>= 5 cases per feature)
Verifies baseline functional behavior for each feature in the Feature Inventory using Category-Partition methodology:
- **Feature 1: Login & Route Redirection to `/v2` & Shell Navigation**:
  - `TC-F1-01`: Login success stores session and resolves default redirect to `/v2`.
  - `TC-F1-02`: Login with safe redirect query parameter forwards to specified sub-route.
  - `TC-F1-03`: Login blocks open-redirect attacks (`https://evil.com`, protocol-relative `//evil.com`) and falls back safely to `/v2`.
  - `TC-F1-04`: GuestOnly navigation guard redirects authenticated users to `/v2`.
  - `TC-F1-05`: Sidebar V2 navigation contract includes "Hồ sơ học sinh" (`pi pi-user`) for `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`.
  - `TC-F1-06`: Sidebar V2 navigation contract strictly hides "Hồ sơ học sinh" for `STUDENT` role.
- **Feature 2: Multi-dimensional Search & Student List V2**:
  - `TC-F2-01`: Server-side pagination query contract produces correct parameters (`page`, `size`, `sortField`, `sortDirection`).
  - `TC-F2-02`: Exact student code search filters data matching `CR-STUDENT-001` format (`STU` + 7 digits).
  - `TC-F2-03`: Filter by student name performs case-insensitive substring match.
  - `TC-F2-04`: Server-side sort by allow-list fields orders records consistently.
  - `TC-F2-05`: Drill-down navigation contract resolves to `/v2/students/:studentId`.
- **Feature 3: Student Creation & V3 Account Provisioning**:
  - `TC-F3-01`: Create student V3 payload auto-generates compliant username and default password.
  - `TC-F3-02`: Create student V3 with explicitly specified username and password.
  - `TC-F3-03`: Security guarantee: Response payload never leaks password or passwordHash.
  - `TC-F3-04`: Student V1 creation backwards compatibility succeeds without account provisioning (`userId = null`).
  - `TC-F3-05`: `TEACHER` role is blocked from provisioning V3 student accounts (403 Forbidden).
- **Feature 4: 4-Tab Student Detail Workspace**:
  - `TC-F4-01`: Tab 1 (Profile & User Account) binds demographic data and excludes deprecated `averageScore`.
  - `TC-F4-02`: Tab 2 (Enrollment & Transfer History) displays current class and transfer history (`GET /api/v2/students/{id}/enrollments`).
  - `TC-F4-03`: Tab 3 (Attendance History) binds summary counters and session records (`GET /api/v2/attendance/students/{id}/history`).
  - `TC-F4-04`: Tab 4 (Transcripts) binds semester scores and calculation status.
  - `TC-F4-05`: Tab 4 Recalculate button invokes calculation task for authorized roles (`POST /api/v2/students/{code}/transcripts/recalculate`).
- **Feature 5: Safe Lifecycle & Inactivation Policies**:
  - `TC-F5-01`: Delete unlinked orphan student record succeeds with 204 No Content contract.
  - `TC-F5-02`: Hard delete is blocked when student has active enrollments or scorebook data (foreign key protection).
  - `TC-F5-03`: Guided status transition warning prompts user to change status instead of deletion.
  - `TC-F5-04`: Transitioning student status to `INACTIVE` retires student from unassigned queue.
  - `TC-F5-05`: Transitioning student status to `GRADUATED` preserves academic transcript archive.

### Tier 2 — Boundary Value Analysis & Corner Cases (>= 5 cases per feature)
Applies Boundary Value Analysis, error guessing, and extreme edge conditions:
- **Boundary 1: Authentication & Navigation Boundaries**:
  - `TC-B1-01`: Invalid credentials (401 Unauthorized) resets session and retains current view.
  - `TC-B1-02`: Mid-session token expiration triggers session eviction and redirect to login.
  - `TC-B1-03`: Unauthenticated direct access to `/v2` is intercepted by router guard.
  - `TC-B1-04`: `STUDENT` role attempting direct URL access to `/v2/students` is blocked.
  - `TC-B1-05`: Malicious redirect URIs with protocol-relative, `javascript:`, or control chars are sanitized.
- **Boundary 2: Search Input Boundaries & Pagination Edge Cases**:
  - `TC-B2-01`: Zero search results returns empty content and `totalElements = 0` without error.
  - `TC-B2-02`: Out-of-bounds pagination (`page > totalPages`) returns safe empty page.
  - `TC-B2-03`: Negative or zero pagination parameters are sanitized to default valid bounds.
  - `TC-B2-04`: Unicode Vietnamese characters and maximum length (35 chars) in search string.
  - `TC-B2-05`: Invalid birth date format submission is rejected by validation.
- **Boundary 3: Student V3 Creation & Conflict Boundaries**:
  - `TC-B3-01`: Duplicate `studentCode` triggers 409 Conflict without partial record creation.
  - `TC-B3-02`: Duplicate `username` triggers 409 Conflict and complete transaction rollback.
  - `TC-B3-03`: Long Vietnamese names trigger initial-based fallback username <= 20 characters.
  - `TC-B3-04`: Password length boundary validation rejects < 6 or > 15 characters.
  - `TC-B3-05`: Strict `studentCode` regex validation `STU[0-9]{7}` rejects malformed codes.
- **Boundary 4: Workspace Tab Boundaries & Edge Data States**:
  - `TC-B4-01`: Tab 2 handles newly admitted student with zero enrollment history gracefully.
  - `TC-B4-02`: Tab 3 prevents division by zero when `totalSessions` is 0 (0% display, no `NaN`).
  - `TC-B4-03`: Tab 4 renders placeholder dash `—` for students with no recorded scores.
  - `TC-B4-04`: `TEACHER` role accessing Tab 4 for unassigned class receives 403 Forbidden.
  - `TC-B4-05`: Concurrent recalculation requests are throttled when already `CALCULATING`.
- **Boundary 5: Safe Lifecycle & Foreign Key Constraint Boundaries**:
  - `TC-B5-01`: Deleting non-existent student ID returns 404 Not Found cleanly.
  - `TC-B5-02`: Foreign key violation triggers managed exception instead of unhandled SQL crash.
  - `TC-B5-03`: Re-activating `INACTIVE` student back to `ACTIVE` restores eligibility in unassigned pool.
  - `TC-B5-04`: Reject enrolling `GRADUATED` student into an active school class.
  - `TC-B5-05`: `TEACHER` role is unauthorized to perform student deletion (403 Forbidden).

### Tier 3 — Cross-Feature Combinations & Chained Workflows
Verifies end-to-end integration continuity across multi-module workflows:
- **Chain 1: Multi-Module Onboarding**: Student V3 creation -> Class Placement in 6A1 -> Homeroom Attendance recording -> Scorebook Entry -> Cross-tab verification.
- **Chain 2: Class Transfer Flow**: Student transferred 6A1 -> 6A2 retains full prior attendance & transfer audit trail in Tab 2 and Tab 3.
- **Chain 3: Attendance Exception Flow**: Excused absence recorded syncs to Tab 3 counters and Tab 4 semester transcript absence statistics.
- **Chain 4: Score Modification & Recalculation Flow**: Score change request approval triggers `OUTDATED` status, recalculation task execution, and `UP_TO_DATE` status with updated GPA.
- **Chain 5: Student Self-Service Flow**: Student logs in with provisioned credentials, redirects to `/v2`, views personal transcript, and is blocked from administrative routes.

### Tier 4 — Real-World Application Scenarios (Persona Walkthroughs)
Emulates realistic human operator journeys:
- **Persona 1: Academic Office Intake (Cô Lan)**: Intake of 6th grade admissions with automatic V3 account provisioning and class placement.
- **Persona 2: Homeroom Teacher Inspection (Thầy Hùng)**: Audits student profile, inspects unexcused absence patterns, verifies grade reports, and confirms role action boundaries.
- **Persona 3: Safe Offboarding**: Student moving abroad mid-year; system denies hard deletion to preserve academic transcripts and guides status update to `INACTIVE`.
- **Persona 4: Student Self-Service (Em Minh)**: Student checks semester report card at home, verifies personalized view, and prevents URL parameter tampering.
- **Persona 5: Administrator Comprehensive Audit**: Administrator inspects multi-tab records, initiates background GPA recalculation, audits completion state, and exports full student directory CSV.

---

## 4. Test Execution Commands

### Frontend Test Execution
```bash
# Run the entire test suite once
npm --prefix FE run test -- --run

# Run specifically the 4-Tier E2E Integration Test Suite
npm --prefix FE run test -- --run src/tests/e2e

# Run test suite with V8 code coverage report
npm --prefix FE run test:coverage

# TypeScript compile & static lint checks
npm --prefix FE run lint
npm --prefix FE run build
```

### Backend Test Execution
```bash
cd BE/BaiTap-RS

# Run all backend unit and integration tests
./gradlew test

# Run code style & static code analysis checks
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest

# Generate JaCoCo coverage report
./gradlew jacocoTestReport
```

---

## 5. Mocking, Fixtures & Deterministic State Management

All fixtures and contracts are centralized in `FE/src/tests/e2e/fixtures.ts`:
- **Deterministic Auth Sessions**:
  - `ADMIN_SESSION` (Role: `ADMIN`)
  - `ACADEMIC_OFFICE_SESSION` (Role: `ACADEMIC_OFFICE`)
  - `TEACHER_SESSION` (Role: `TEACHER`)
  - `STUDENT_SESSION` (Role: `STUDENT`)
- **State Isolation**: Every test suite executes `clearAuthSession()` in `beforeEach` and `afterEach` hooks to prevent state leakage between tests.
- **Contract Schemas**: Typed interfaces for `StudentV3CreateRequest`, `StudentV3CreateResponse`, `ResStudentEnrollmentHistoryDTO`, `StudentAttendanceHistoryResponse`, `ResStudentTermTranscriptDTO`, `ResCalculationTaskDTO`.
- **Helper Functions**: Pure business logic validators (`validateSafeRedirect`, `generateStudentUsername`) matching backend and router rules.

---

## 6. Integrity and Quality Assurance Policy

1. **Strict No-Cheat Policy**: No dummy assertions (`expect(true).toBe(true)`), facade tests, or mocked passes without real logic validation.
2. **Deterministic Independence**: Every test sets up its own isolated state and can run independently in any order.
3. **Traceability**: All test IDs (`TC-F1-*` to `TC-F5-*`, `TC-B1-*` to `TC-B5-*`, `Chain 1-5`, `Persona 1-5`) map directly to project requirements `R1` through `R5` and the Login Redirect follow-up.
