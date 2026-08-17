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
| 1 | `student_id` | INT | - | Yes | Yes | Auto increment |
| 2 | `student_name` | VARCHAR | 20 | No | Yes | |
| 3 | `student_code` | VARCHAR | 10 | No | Yes | |

DDL:

```sql
CREATE TABLE student (
    student_id INT NOT NULL AUTO_INCREMENT,
    student_name VARCHAR(20) NOT NULL,
    student_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (student_id)
);
```

The supplied schema does not explicitly mark `student_code` as unique.

Because the UI has a generated Student Code and uses it as a visible identifier, a uniqueness constraint is reasonable but remains **TBD** until confirmed.

If confirmed:

```sql
ALTER TABLE student
ADD CONSTRAINT uk_student_code UNIQUE (student_code);
```

---

# 4. Table: `student_info`

| No. | Field | Type | Length | PK | NOT NULL | Note |
|---:|---|---|---:|---|---|---|
| 1 | `info_id` | INT | - | Yes | Yes | Auto increment |
| 2 | `student_id` | INT | - | Yes* | Yes | Student reference |
| 3 | `address` | VARCHAR | 255 | No | No | |
| 4 | `average_score` | DOUBLE | - | No | No | |
| 5 | `date_of_birth` | DATETIME | - | No | No | Display format noted as yyyy/mm/dd |

\*The supplied sheet visually marks `student_id` in the PK column. This is ambiguous because `info_id` is already marked as a primary key.

For a clean one-to-one relational design, this documentation adopts:

- `info_id` as the primary key.
- `student_id` as a NOT NULL foreign key with a UNIQUE constraint.

That preserves one `student_info` row per student without requiring a composite primary key.

Recommended DDL:

```sql
CREATE TABLE student_info (
    info_id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    address VARCHAR(255),
    average_score DOUBLE,
    date_of_birth DATETIME,
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
Student 1 -------- 0..1 StudentInfo
```

A student must have a `student` row.

The supplied form contains Student Name/Code plus Birthday/Address/Score as one logical record. In normal application usage, create both records together.

Because the DB sheet marks `student_info.student_id` NOT NULL, any `student_info` row must belong to a student.

---

# 6. Delete Strategy

Recommended:

```sql
FOREIGN KEY (student_id)
REFERENCES student(student_id)
ON DELETE CASCADE
```

This allows deleting a Student to remove its StudentInfo automatically.

Alternative:

- Service explicitly deletes `student_info` then `student` in one transaction.

Choose one strategy and use it consistently.

If `ON DELETE CASCADE` is used, DDL becomes:

```sql
CONSTRAINT fk_student_info_student
    FOREIGN KEY (student_id)
    REFERENCES student(student_id)
    ON DELETE CASCADE
```

---

# 7. Date Representation

Database sheet uses:

```text
date_of_birth: dateTime
display: yyyy/mm/dd
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

Formatting in the UI may still be `yyyy/MM/dd` or another specified display format.

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
LEFT/INNER JOIN student_info
```

depending on whether a StudentInfo record is guaranteed.

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

through FK cascade or service transaction.

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

Potential source structures:

### Separate CSV files

```text
users.csv
students.csv
student_info.csv
```

### Joined student CSV

```text
student_id,
student_code,
student_name,
date_of_birth,
address,
average_score
```

The assignment does not specify the exact CSV shape.

Therefore:

- File layout: **TBD**.
- Output path: **TBD**.
- Trigger/schedule: **TBD**.

Spring Batch should own reading and writing rather than implementing the export as an unstructured loop in a controller.

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
   student_code NOT NULL

student_info
PK info_id
FK/UNIQUE student_id -> student.student_id
   address
   average_score
   date_of_birth
```

Open schema decisions:

1. `student_code` uniqueness.
2. `DATETIME` vs `DATE` for date of birth.
3. Whether the trainer intended `student_info.student_id` as part of a composite primary key.

Resolve these before freezing the first production migration/schema script.
