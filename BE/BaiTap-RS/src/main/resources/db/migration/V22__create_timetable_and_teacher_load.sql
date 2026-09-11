CREATE TABLE functional_room (
    room_id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id),
    CONSTRAINT uk_functional_room_code UNIQUE (code)
);

CREATE TABLE subject_functional_room (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    functional_room_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_subject_functional_room UNIQUE (subject_id, functional_room_id),
    CONSTRAINT fk_sfr_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id),
    CONSTRAINT fk_sfr_room FOREIGN KEY (functional_room_id) REFERENCES functional_room (room_id)
);

CREATE TABLE timetable_calendar (
    calendar_id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (calendar_id),
    CONSTRAINT uk_timetable_calendar_semester UNIQUE (semester_id),
    CONSTRAINT fk_ttc_semester FOREIGN KEY (semester_id) REFERENCES semester (semester_id)
);

CREATE TABLE timetable_period (
    period_id BIGINT NOT NULL AUTO_INCREMENT,
    calendar_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    session VARCHAR(20) NOT NULL,
    period_index INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    PRIMARY KEY (period_id),
    CONSTRAINT uk_timetable_period UNIQUE (calendar_id, day_of_week, session, period_index),
    CONSTRAINT fk_ttp_calendar FOREIGN KEY (calendar_id) REFERENCES timetable_calendar (calendar_id)
);

CREATE TABLE timetable_closed_date (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calendar_id BIGINT NOT NULL,
    closed_date DATE NOT NULL,
    reason VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_timetable_closed_date UNIQUE (calendar_id, closed_date),
    CONSTRAINT fk_ttcd_calendar FOREIGN KEY (calendar_id) REFERENCES timetable_calendar (calendar_id)
);

CREATE TABLE teacher_unavailability (
    unavailability_id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    day_of_week INT NULL,
    specific_date DATE NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    session VARCHAR(20) NOT NULL,
    period_indexes VARCHAR(50) NOT NULL,
    note VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_reason VARCHAR(255) NULL,
    decided_by BIGINT NULL,
    decided_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (unavailability_id),
    CONSTRAINT fk_tu_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (teacher_id),
    CONSTRAINT fk_tu_semester FOREIGN KEY (semester_id) REFERENCES semester (semester_id),
    CONSTRAINT fk_tu_decided_by FOREIGN KEY (decided_by) REFERENCES app_user (user_id)
);

CREATE INDEX idx_tu_teacher_status ON teacher_unavailability (teacher_id, status);

CREATE TABLE teacher_load_policy (
    policy_id BIGINT NOT NULL AUTO_INCREMENT,
    version VARCHAR(50) NOT NULL,
    source VARCHAR(255) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    base_periods INT NOT NULL DEFAULT 19,
    homeroom_reduction INT NOT NULL DEFAULT 4,
    nursing_reduction INT NOT NULL DEFAULT 3,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version_lock BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_id),
    CONSTRAINT uk_tlp_version UNIQUE (version)
);

CREATE TABLE teacher_load_eligibility (
    eligibility_id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    evidence_reference VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (eligibility_id),
    CONSTRAINT fk_tle_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (teacher_id)
);

CREATE INDEX idx_tle_teacher_status ON teacher_load_eligibility (teacher_id, status);

CREATE TABLE timetable_head (
    timetable_id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    current_revision_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (timetable_id),
    CONSTRAINT uk_tth_semester UNIQUE (semester_id),
    CONSTRAINT fk_tth_semester FOREIGN KEY (semester_id) REFERENCES semester (semester_id)
);

CREATE TABLE timetable_revision (
    revision_id BIGINT NOT NULL AUTO_INCREMENT,
    timetable_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    revision_number INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    policy_id BIGINT NULL,
    validation_fingerprint VARCHAR(255) NULL,
    blocking_count INT NOT NULL DEFAULT 0,
    warning_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (revision_id),
    CONSTRAINT uk_ttr_revision UNIQUE (timetable_id, revision_number),
    CONSTRAINT fk_ttr_head FOREIGN KEY (timetable_id) REFERENCES timetable_head (timetable_id),
    CONSTRAINT fk_ttr_semester FOREIGN KEY (semester_id) REFERENCES semester (semester_id),
    CONSTRAINT fk_ttr_policy FOREIGN KEY (policy_id) REFERENCES teacher_load_policy (policy_id)
);

ALTER TABLE timetable_head
    ADD CONSTRAINT fk_tth_cur_rev FOREIGN KEY (current_revision_id) REFERENCES timetable_revision (revision_id);

CREATE TABLE timetable_entry (
    entry_id BIGINT NOT NULL AUTO_INCREMENT,
    revision_id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    functional_room_id BIGINT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (entry_id),
    CONSTRAINT fk_tte_revision FOREIGN KEY (revision_id) REFERENCES timetable_revision (revision_id),
    CONSTRAINT fk_tte_assignment FOREIGN KEY (assignment_id) REFERENCES subject_teaching_assignment (assignment_id),
    CONSTRAINT fk_tte_period FOREIGN KEY (period_id) REFERENCES timetable_period (period_id),
    CONSTRAINT fk_tte_room FOREIGN KEY (functional_room_id) REFERENCES functional_room (room_id)
);

CREATE INDEX idx_tte_rev_period ON timetable_entry (revision_id, period_id);

CREATE TABLE timetable_publish_intent (
    id BIGINT NOT NULL AUTO_INCREMENT,
    revision_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    actor_id BIGINT NULL,
    payload_hash VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_ttpi_key UNIQUE (idempotency_key),
    CONSTRAINT fk_ttpi_revision FOREIGN KEY (revision_id) REFERENCES timetable_revision (revision_id)
);

CREATE TABLE timetable_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    timetable_id BIGINT NOT NULL,
    revision_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id BIGINT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

