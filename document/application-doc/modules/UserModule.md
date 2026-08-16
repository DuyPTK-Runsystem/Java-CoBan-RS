# User Module

## 1. Scope

The User module owns:

- User registration.
- User login.
- User logout.
- Current logged-in username display.
- User credential validation.

Primary table:

- `user`

Screens/components:

- Login screen.
- Register screen.
- `LoginForm`.
- `RegisterForm`.

---

## 2. Domain Model

```text
User
├── userId
├── userName
└── password
```

Database mapping:

| Property | Table | Column | Constraint |
|---|---|---|---|
| userId | user | user_id | PK, NOT NULL, auto increment |
| userName | user | user_name | VARCHAR(20), NOT NULL |
| password | user | password | VARCHAR(15), NOT NULL in supplied schema |

### Security note

The supplied database design gives `password VARCHAR(15)`, while secure password hashing normally requires a much larger column and the stored value is a hash rather than the raw password.

This creates a specification conflict.

For strict replication of the training sheet, keep the supplied schema. For a security-correct implementation, change the password column length and store only a hash. This decision must be confirmed before implementation and then reflected in `DataStructure.md`.

Never silently truncate a password hash into the supplied 15-character column.

---

# 3. Login

## 3.1 UI fields

| Item | Control | Bound field |
|---|---|---|
| User Name | input | `user.user_name` |
| Password | password input | `user.password` |
| Login | button | action |
| Register | button/link | navigation |

Initial state:

- User Name enabled.
- Password enabled.
- Login enabled.
- Register enabled.

---

## 3.2 Validation

### User Name

- Required.
- Maximum length: 20.
- Only 1-byte characters.

### Password

- Required.
- Minimum length: 6.
- Maximum length: 15.
- Only 1-byte characters.

Validation must run on both frontend and backend.

"1-byte character" is interpreted as ASCII-compatible single-byte input for this assignment. If the trainer expects another encoding definition, replace this interpretation explicitly.

---

## 3.3 Login behavior

```text
User enters credentials
  ->
client validation
  ->
login API
  ->
server validation
  ->
find matching user
  ->
success / failure
```

Failure:

- Display a message when username/password is invalid or does not match an existing user.

Success:

- Establish the application's authenticated state.
- Navigate to Student List.
- Student List displays the logged-in username.

Do not return the stored password to the frontend.

---

# 4. Register User

## 4.1 UI fields

| Item | Control | Bound field |
|---|---|---|
| User Name | input | `user.user_name` |
| Password | password input | `user.password` |
| Confirm Password | password input | frontend/request only |
| Back | button | navigation |
| Register | button | action |

Initial state:

- Three inputs enabled.
- Back enabled.
- Register enabled.

---

## 4.2 Validation

### User Name

- Required.
- Maximum length: 20.
- Only 1-byte characters.
- Must not already exist.

### Password

- Required.
- Minimum length: 6.
- Maximum length: 15.
- Only 1-byte characters.

### Confirm Password

- Required.
- Minimum length: 6.
- Maximum length: 15.
- Only 1-byte characters.
- Must equal Password.

---

## 4.3 Register behavior

```text
Register clicked
  ->
client validation
  ->
POST registration request
  ->
server validation
  ->
check duplicate username
  ->
persist user
  ->
navigate to Login
```

If username exists:

- Do not create a new user.
- Return/display an appropriate message.

If password and confirmation differ:

- Do not call persistence logic.

`confirmPassword` is not a database field.

---

# 5. Logout

From Student List or Student Form:

```text
Logout
  ->
clear authenticated state
  ->
navigate to Login
```

The exact server-side logout behavior depends on the selected authentication mechanism.

---

# 6. Backend Design

Recommended classes:

```text
user/
├── controller/
│   └── UserController
├── service/
│   └── UserService
├── repository/
│   └── UserRepository
├── entity/
│   └── User
└── dto/
    ├── LoginRequest
    ├── LoginResponse
    └── RegisterUserRequest
```

## Controller responsibilities

- Parse HTTP request.
- Trigger bean validation.
- Delegate to service.
- Map service result to HTTP response.

Do not implement database queries or credential business rules directly in the controller.

## Service responsibilities

- Check username duplication.
- Authenticate credentials.
- Create users.
- Apply user-domain business rules.
- Coordinate password storage strategy.

## Repository responsibilities

Typical operations:

```java
Optional<User> findByUserName(String userName);
boolean existsByUserName(String userName);
```

---

# 7. Recommended REST Contract

Exact paths can be renamed consistently.

## Register user

```http
POST /api/users/register
```

Request:

```json
{
  "userName": "NguyenVanA",
  "password": "secret1",
  "confirmPassword": "secret1"
}
```

## Login

```http
POST /api/auth/login
```

Request:

```json
{
  "userName": "NguyenVanA",
  "password": "secret1"
}
```

Response must not expose the persisted password.

## Logout

```http
POST /api/auth/logout
```

Whether this endpoint is required depends on the selected session/token design.

---

# 8. Frontend Design

Recommended structure:

```text
src/
├── components/
│   ├── LoginForm.vue
│   └── RegisterForm.vue
├── views/
│   ├── LoginView.vue
│   └── RegisterView.vue
└── services/
    └── userApi.ts
```

## LoginForm

Responsibilities:

- Render username/password.
- Client validation.
- Emit/execute login submit.
- Provide Register navigation.

It should not contain Student-module logic.

## RegisterForm

Responsibilities:

- Render registration fields.
- Validate password confirmation.
- Submit registration.
- Provide Back navigation.

---

# 9. Storybook

The assignment explicitly requires Storybook for:

- `LoginForm`
- `RegisterForm`

Minimum useful stories:

```text
LoginForm
├── Default
├── Filled
└── ValidationError

RegisterForm
├── Default
├── Filled
├── PasswordMismatch
└── ValidationError
```

Stories should mock callbacks/API boundaries rather than requiring a live Spring Boot server.

---

# 10. Backend Validation Guidance

Request DTOs should carry validation rather than binding HTTP input directly to JPA entities.

Conceptually:

```text
LoginRequest
- userName: required, max 20, one-byte
- password: required, min 6, max 15, one-byte
```

and:

```text
RegisterUserRequest
- userName
- password
- confirmPassword
```

Cross-field password matching belongs in service validation or a dedicated custom constraint.

---

# 11. User Module Invariants

These rules are authoritative unless the assignment is updated:

1. Username cannot be blank.
2. Username cannot exceed 20 characters.
3. Password cannot be blank.
4. Password length is 6..15 according to the supplied screen.
5. Register confirmation must match the password.
6. Duplicate usernames are rejected.
7. Successful login navigates to Student List.
8. Register screen Back navigates to Login.
9. Logout navigates to Login.
10. Password must never be returned in normal API responses.
