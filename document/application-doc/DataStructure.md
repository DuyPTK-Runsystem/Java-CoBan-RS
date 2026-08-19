# Data Structure

## 1. Database

Database engine:

- MySQL

The supplied assignment defines three tables:

```text
user
student
student_info
```

Logical relationship:

```text
user
  (independent authentication data)

student
   |
   | 1 : 1
   v
student_info
```

`student_info.student_id` references `student.student_id`.

---

# 2. Table: `user`

| No. | Field | Type | Length | PK | NOT NULL | Note |
|---:|---|---|---:|---|---|---|
| 1 | `user_id` | BIGINT | - | Yes | Yes | Auto increment; Java `Long` |
| 2 | `user_name` | VARCHAR | 20 | No | Yes | Login/registration username |
| 3 | `password` | VARCHAR | 255 | No | Yes | Password hash storage |

Recommended DDL matching the supplied sheet:

```sql
CREATE TABLE `user` (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id)
);
```

## Password storage decision

The supplied training sheet originally used `password VARCHAR(15)`, but the application stores password hashes. The project decision is:

```sql
password VARCHAR(255) NOT NULL
```

Raw password input is still validated by the User module rules: required, minimum length 6, maximum length 15, and ASCII/single-byte characters.

---

# 3. Table: `student`

| No. | Field | Type | Length | PK | NOT NULL | Note |
|---:|---|---|---:|---|---|---|
| 1 | `student_id` | BIGINT | - | Yes | Yes | Auto increment; Java `Long` |
| 2 | `student_name` | VARCHAR | 35 | No | Yes | |
| 3 | `student_code` | VARCHAR | 10 | No | Yes | |

DDL:

```sql
CREATE TABLE student (
    student_id BIGINT NOT NULL AUTO_INCREMENT,
    student_name VARCHAR(35) NOT NULL,
    student_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (student_id)
);
```

The current application treats `student_code` as globally unique because the UI uses it as a visible generated identifier.

The application model enforces this constraint:

```sql
ALTER TABLE student
ADD CONSTRAINT uk_student_student_code UNIQUE (student_code);
```

---

# 4. Table: `student_info`

| No. | Field | Type | Length | PK | NOT NULL | Note |
|---:|---|---|---:|---|---|---|
| 1 | `info_id` | BIGINT | - | Yes | Yes | Auto increment; Java `Long` |
| 2 | `student_id` | BIGINT | - | Yes* | Yes | Student reference |
| 3 | `address` | VARCHAR | 255 | No | No | |
| 4 | `average_score` | DOUBLE | - | No | No | |
| 5 | `date_of_birth` | DATE | - | No | No | API uses `yyyy-MM-dd`; UI displays `dd-mm-yyy` |

\*The supplied sheet visually marks `student_id` in the PK column. This is ambiguous because `info_id` is already marked as a primary key.

For a clean one-to-one relational design, this documentation adopts:

- `info_id` as the primary key.
- `student_id` as a NOT NULL foreign key with a UNIQUE constraint.

That preserves one `student_info` row per student without requiring a composite primary key.

Recommended DDL:

```sql
CREATE TABLE student_info (
    info_id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    address VARCHAR(255),
    average_score DOUBLE,
    date_of_birth DATE,
    PRIMARY KEY (info_id),
    CONSTRAINT uk_student_info_student UNIQUE (student_id),
    CONSTRAINT fk_student_info_student
        FOREIGN KEY (student_id)
        REFERENCES student(student_id)
);
```

If the trainer explicitly intended a composite primary key `(info_id, student_id)`, update this document before coding.

---

# 5. Cardinality

Intended application relationship:

```text
Student 1 -------- 1 StudentInfo
```

A StudentInfo must belong to exactly one Student through its NOT NULL, UNIQUE foreign
key. The application aggregate requires exactly one StudentInfo for each Student via
the bidirectional JPA association with `optional = false`, `cascade = ALL` and
`orphanRemoval = true`.

The supplied form contains Student Name/Code plus Birthday/Address/Score as one
logical record, so application services create both rows together. `date_of_birth`,
`address` and `average_score` remain nullable; mandatory StudentInfo does not make
its descriptive fields mandatory.

The child-to-parent foreign key cannot by itself enforce that every `student` row has
a child row; that invariant is maintained by the application aggregate lifecycle.

---

# 6. Delete Strategy

The current application uses the JPA aggregate lifecycle:

```text
Student.studentInfo: cascade = ALL, orphanRemoval = true
StudentService.deleteStudent: delete Student in a transaction
```

Deleting Student therefore deletes its associated StudentInfo through JPA cascade.
The database foreign key does not currently rely on `ON DELETE CASCADE`.

---

# 7. Date Representation

Database sheet uses:

```text
date_of_birth: dateTime
API: yyyy-MM-dd
UI: dd-mm-yyy
```

Recommended Java mapping:

```java
LocalDate
```

rather than `LocalDateTime`, because birthday is a calendar date without a meaningful time-of-day.

Recommended MySQL type for a clean implementation:

```sql
DATE
```

instead of `DATETIME`.

However, if strict compatibility with the supplied database sheet is required, use `DATETIME` and normalize the time component.

This is another explicit specification-vs-domain-model decision.

Recommended application choice:

```text
MySQL DATE
<-> Java LocalDate
<-> JSON yyyy-MM-dd
```

The current UI display format is `dd-mm-yyy`.

---

# 8. Average Score

Supplied type:

```text
DOUBLE
```

Java mapping:

```java
Double
```

or primitive `double` only if the database field becomes NOT NULL.

No allowed score range is supplied.

Do not enforce `0 <= score <= 10` unless explicitly confirmed.

---

# 9. JPA Entity Guidance

## User

```text
User
- id
- userName
- password
```

## Student

```text
Student
- id
- studentName
- studentCode
- studentInfo
```

## StudentInfo

```text
StudentInfo
- id
- student
- address
- averageScore
- dateOfBirth
```

Recommended association:

```java
Student  @OneToOne  StudentInfo
```

The owning side should correspond to the table containing the FK (`student_info`).

Avoid exposing JPA entities directly from REST controllers. Use DTOs to prevent:

- persistence coupling,
- accidental password exposure,
- recursion in entity relationships,
- unstable API serialization.

---

# 10. Data Access Use Cases

## Login

Reads:

```text
user
```

Lookup by username, then verify password according to selected credential-storage design.

## User registration

Writes:

```text
user
```

## Student list/search

Reads joined data:

```text
student
INNER JOIN student_info
```

StudentInfo is mandatory in the application aggregate; its three descriptive columns
can still be null.

## Add student

Writes transactionally:

```text
student
+
student_info
```

## Update student

Updates transactionally:

```text
student
+
student_info
```

## Delete student

Deletes logically:

```text
student
+
student_info
```

through JPA cascade in the service transaction.

---

# 11. Index Guidance

Primary-key indexes are created automatically.

Recommended additional indexes based on search behavior:

```sql
CREATE INDEX idx_student_code
    ON student(student_code);

CREATE INDEX idx_student_name
    ON student(student_name);

CREATE INDEX idx_student_info_dob
    ON student_info(date_of_birth);
```

If `student_code` becomes UNIQUE, a separate non-unique index on the same column is unnecessary.

These indexes are implementation optimizations, not mandatory business rules.

---

# 12. Batch Data Source

The batch requirement states that information should be read from tables and written to CSV.

The export source is the following inner join:

```sql
select
    s.student_id,
    s.student_name,
    s.student_code,
    si.address,
    si.average_score,
    si.date_of_birth
from student s
join student_info si on s.student_id = si.student_id
```

The CSV column order is:

```text
student_id,student_name,student_code,address,average_score,date_of_birth
```

The export is triggered by an authenticated API call and returned as CSV `byte[]`.
The application does not write or retain a CSV file on its filesystem. Spring Batch owns
the table reading and CSV mapping/writing, rather than an unstructured controller loop.
An object that cannot be processed is skipped while remaining objects continue; the CSV
contains successful rows only and the skipped object is logged.

---

# 13. Final Authoritative Schema Summary

Current documented model:

```text
user
PK user_id
   user_name NOT NULL
   password NOT NULL

student
PK student_id
   student_name NOT NULL
   student_code NOT NULL UNIQUE

student_info
PK info_id
FK/UNIQUE student_id -> student.student_id
   address
   average_score
   date_of_birth
```

Open schema decisions:

1. `DATETIME` vs `DATE` for date of birth.
2. Whether the trainer intended `student_info.student_id` as part of a composite primary key.

Resolve these before freezing the first production migration/schema script.
