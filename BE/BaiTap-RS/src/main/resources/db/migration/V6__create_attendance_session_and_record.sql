CREATE TABLE attendance_session (
    session_id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    session_period VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id),
    CONSTRAINT uk_attendance_session_class_date_period UNIQUE (
        class_id, attendance_date, session_period
    ),
    CONSTRAINT ck_attendance_session_period CHECK (session_period IN ('MORNING', 'AFTERNOON')),
    CONSTRAINT fk_attendance_session_class FOREIGN KEY (class_id)
        REFERENCES school_class (class_id),
    CONSTRAINT fk_attendance_session_semester FOREIGN KEY (semester_id)
        REFERENCES semester (semester_id),
    CONSTRAINT fk_attendance_session_created_by FOREIGN KEY (created_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_attendance_session_class_semester_date
    ON attendance_session (class_id, semester_id, attendance_date);

CREATE TABLE attendance_record (
    attendance_record_id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(500) NULL,
    recorded_by BIGINT NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (attendance_record_id),
    CONSTRAINT uk_attendance_record_session_student UNIQUE (session_id, student_id),
    CONSTRAINT ck_attendance_record_status CHECK (
        status IN ('ABSENT', 'EXCUSED', 'LATE', 'EARLY_LEAVE')
    ),
    CONSTRAINT fk_attendance_record_session FOREIGN KEY (session_id)
        REFERENCES attendance_session (session_id),
    CONSTRAINT fk_attendance_record_student FOREIGN KEY (student_id)
        REFERENCES student (student_id),
    CONSTRAINT fk_attendance_record_recorded_by FOREIGN KEY (recorded_by)
        REFERENCES app_user (user_id),
    CONSTRAINT fk_attendance_record_updated_by FOREIGN KEY (updated_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_attendance_record_session_student
    ON attendance_record (session_id, student_id);
