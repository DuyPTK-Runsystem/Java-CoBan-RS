CREATE TABLE semester (
    semester_id BIGINT NOT NULL AUTO_INCREMENT,
    academic_year_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    automatic_lock_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL,
    locked_at TIMESTAMP NULL,
    locked_by BIGINT NULL,
    lock_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (semester_id),
    CONSTRAINT uk_semester_year_code UNIQUE (academic_year_id, code),
    CONSTRAINT ck_semester_dates CHECK (end_date > start_date),
    CONSTRAINT ck_semester_status CHECK (status IN ('DRAFT', 'ACTIVE', 'LOCKED', 'CLOSED')),
    CONSTRAINT fk_semester_year FOREIGN KEY (academic_year_id)
        REFERENCES academic_year (academic_year_id),
    CONSTRAINT fk_semester_locked_by FOREIGN KEY (locked_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_semester_year_status ON semester (academic_year_id, status);

CREATE TABLE teacher (
    teacher_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    teacher_code VARCHAR(50) NOT NULL,
    teacher_name VARCHAR(150) NOT NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(20) NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(150) NULL,
    department VARCHAR(100) NULL,
    join_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (teacher_id),
    CONSTRAINT uk_teacher_user UNIQUE (user_id),
    CONSTRAINT uk_teacher_code UNIQUE (teacher_code),
    CONSTRAINT ck_teacher_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'INACTIVE')),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_teacher_status ON teacher (status);

CREATE TABLE subject (
    subject_id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    subject_type VARCHAR(20) NOT NULL,
    application_scope VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (subject_id),
    CONSTRAINT uk_subject_code UNIQUE (code),
    CONSTRAINT ck_subject_type CHECK (subject_type IN ('ACADEMIC', 'SKILL')),
    CONSTRAINT ck_subject_scope CHECK (application_scope IN ('GRADE', 'CLASS')),
    CONSTRAINT ck_subject_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE subject_applicability (
    subject_applicability_id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    grade_level_id BIGINT NULL,
    class_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (subject_applicability_id),
    CONSTRAINT uk_subject_applicability_grade UNIQUE (
        subject_id, semester_id, scope_type, grade_level_id
    ),
    CONSTRAINT uk_subject_applicability_class UNIQUE (
        subject_id, semester_id, scope_type, class_id
    ),
    CONSTRAINT ck_subject_applicability_scope CHECK (
        (scope_type = 'GRADE' AND grade_level_id IS NOT NULL AND class_id IS NULL)
        OR (scope_type = 'CLASS' AND grade_level_id IS NULL AND class_id IS NOT NULL)
    ),
    CONSTRAINT ck_subject_applicability_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_subject_applicability_subject FOREIGN KEY (subject_id)
        REFERENCES subject (subject_id),
    CONSTRAINT fk_subject_applicability_semester FOREIGN KEY (semester_id)
        REFERENCES semester (semester_id),
    CONSTRAINT fk_subject_applicability_grade FOREIGN KEY (grade_level_id)
        REFERENCES grade_level (grade_level_id),
    CONSTRAINT fk_subject_applicability_class FOREIGN KEY (class_id)
        REFERENCES school_class (class_id)
);

CREATE INDEX idx_subject_applicability_subject_semester_status
    ON subject_applicability (subject_id, semester_id, scope_type, status);
CREATE INDEX idx_subject_applicability_grade_semester_status
    ON subject_applicability (grade_level_id, semester_id, status);
CREATE INDEX idx_subject_applicability_class_semester_status
    ON subject_applicability (class_id, semester_id, status);

CREATE TABLE class_subject (
    class_subject_id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_subject_id),
    CONSTRAINT uk_class_subject_tuple UNIQUE (class_id, subject_id, semester_id),
    CONSTRAINT ck_class_subject_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMPLETED')),
    CONSTRAINT fk_class_subject_class FOREIGN KEY (class_id)
        REFERENCES school_class (class_id),
    CONSTRAINT fk_class_subject_subject FOREIGN KEY (subject_id)
        REFERENCES subject (subject_id),
    CONSTRAINT fk_class_subject_semester FOREIGN KEY (semester_id)
        REFERENCES semester (semester_id)
);

CREATE INDEX idx_class_subject_class_semester ON class_subject (class_id, semester_id);
CREATE INDEX idx_class_subject_subject_semester ON class_subject (subject_id, semester_id);

CREATE TABLE homeroom_assignment (
    assignment_id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    status VARCHAR(20) NOT NULL,
    assigned_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assignment_id),
    CONSTRAINT ck_homeroom_assignment_status CHECK (status IN ('ACTIVE', 'ENDED')),
    CONSTRAINT ck_homeroom_assignment_dates CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT fk_homeroom_assignment_class FOREIGN KEY (class_id)
        REFERENCES school_class (class_id),
    CONSTRAINT fk_homeroom_assignment_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher (teacher_id),
    CONSTRAINT fk_homeroom_assignment_by FOREIGN KEY (assigned_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_homeroom_assignment_class_status
    ON homeroom_assignment (class_id, status);
CREATE INDEX idx_homeroom_assignment_teacher_status
    ON homeroom_assignment (teacher_id, status);

CREATE TABLE subject_teaching_assignment (
    assignment_id BIGINT NOT NULL AUTO_INCREMENT,
    class_subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    status VARCHAR(20) NOT NULL,
    assigned_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assignment_id),
    CONSTRAINT ck_subject_teaching_assignment_status CHECK (status IN ('ACTIVE', 'ENDED')),
    CONSTRAINT ck_subject_teaching_assignment_dates CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT fk_subject_teaching_assignment_class_subject FOREIGN KEY (class_subject_id)
        REFERENCES class_subject (class_subject_id),
    CONSTRAINT fk_subject_teaching_assignment_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher (teacher_id),
    CONSTRAINT fk_subject_teaching_assignment_by FOREIGN KEY (assigned_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_subject_teaching_assignment_class_subject_status
    ON subject_teaching_assignment (class_subject_id, status);
CREATE INDEX idx_subject_teaching_assignment_teacher_status
    ON subject_teaching_assignment (teacher_id, status);
