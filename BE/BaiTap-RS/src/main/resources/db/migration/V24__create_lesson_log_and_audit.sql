CREATE TABLE lesson_log_policy (
    policy_id BIGINT NOT NULL AUTO_INCREMENT, policy_version INT NOT NULL,
    effective_from DATE NOT NULL, timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    deadline_mode VARCHAR(20) NOT NULL, edit_window_hours INT NULL,
    require_homeroom_review BOOLEAN NOT NULL DEFAULT TRUE, rubric_json JSON NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_id), UNIQUE (policy_version), UNIQUE (effective_from),
    CONSTRAINT fk_llp_actor FOREIGN KEY (created_by) REFERENCES app_user(user_id),
    CONSTRAINT ck_llp_mode CHECK ((deadline_mode='FIXED_HOURS' AND edit_window_hours BETWEEN 1 AND 168) OR (deadline_mode='END_OF_WEEK' AND edit_window_hours IS NULL))
);
CREATE TABLE lesson_log_entry (
    entry_id BIGINT NOT NULL AUTO_INCREMENT, timetable_entry_id BIGINT NOT NULL,
    timetable_revision_id BIGINT NOT NULL, assignment_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL, class_id BIGINT NOT NULL, subject_id BIGINT NOT NULL,
    assigned_teacher_id BIGINT NOT NULL, policy_id BIGINT NOT NULL, lesson_date DATE NOT NULL,
    session VARCHAR(20) NOT NULL, period_index INT NOT NULL, source_snapshot_json JSON NOT NULL,
    roster_count_snapshot INT NULL, lesson_ends_at TIMESTAMP NOT NULL, edit_window_expires_at TIMESTAMP NOT NULL,
    title VARCHAR(255) NULL, content TEXT NULL, completion_status VARCHAR(30) NULL,
    grade VARCHAR(1) NULL, present_count INT NULL, absent_count INT NULL,
    comments VARCHAR(4000) NULL, absent_student_notes VARCHAR(500) NULL, homework VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', submitted_at TIMESTAMP NULL, submitted_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL, reviewed_by BIGINT NULL, review_comment VARCHAR(4000) NULL,
    version BIGINT NOT NULL DEFAULT 0, created_by BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(entry_id),
    UNIQUE(class_id, lesson_date, session, period_index),
    FOREIGN KEY(timetable_entry_id) REFERENCES timetable_entry(entry_id), FOREIGN KEY(timetable_revision_id) REFERENCES timetable_revision(revision_id),
    FOREIGN KEY(assignment_id) REFERENCES subject_teaching_assignment(assignment_id), FOREIGN KEY(semester_id) REFERENCES semester(semester_id),
    FOREIGN KEY(class_id) REFERENCES school_class(class_id), FOREIGN KEY(subject_id) REFERENCES subject(subject_id), FOREIGN KEY(assigned_teacher_id) REFERENCES teacher(teacher_id),
    FOREIGN KEY(policy_id) REFERENCES lesson_log_policy(policy_id), FOREIGN KEY(created_by) REFERENCES app_user(user_id), FOREIGN KEY(submitted_by) REFERENCES app_user(user_id), FOREIGN KEY(reviewed_by) REFERENCES app_user(user_id),
    CHECK(session IN ('MORNING','AFTERNOON')), CHECK(period_index BETWEEN 1 AND 4), CHECK(present_count IS NULL OR present_count >= 0), CHECK(absent_count IS NULL OR absent_count >= 0),
    CHECK(grade IS NULL OR grade IN ('A','B','C','D')), CHECK(completion_status IS NULL OR completion_status IN ('ON_SCHEDULE','BEHIND_SCHEDULE','AHEAD_OF_SCHEDULE'))
);
CREATE INDEX idx_lle_teacher_date ON lesson_log_entry(assigned_teacher_id, lesson_date);
CREATE INDEX idx_lle_class_date ON lesson_log_entry(class_id, lesson_date);
CREATE TABLE lesson_log_weekly_review (
    review_id BIGINT NOT NULL AUTO_INCREMENT, class_id BIGINT NOT NULL, semester_id BIGINT NOT NULL,
    homeroom_assignment_id BIGINT NULL, week_start DATE NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'UNSIGNED',
    weekly_comment VARCHAR(4000) NULL, weekly_grade VARCHAR(1) NULL, signed_snapshot_json JSON NULL,
    signed_at TIMESTAMP NULL, signed_by BIGINT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY(review_id), UNIQUE(class_id, semester_id, week_start), FOREIGN KEY(class_id) REFERENCES school_class(class_id),
    FOREIGN KEY(semester_id) REFERENCES semester(semester_id), FOREIGN KEY(homeroom_assignment_id) REFERENCES homeroom_assignment(assignment_id), FOREIGN KEY(signed_by) REFERENCES app_user(user_id)
);
CREATE TABLE lesson_log_revision (
    revision_id BIGINT NOT NULL AUTO_INCREMENT, entry_id BIGINT NULL, weekly_review_id BIGINT NULL, policy_id BIGINT NULL,
    action VARCHAR(30) NOT NULL, actor_id BIGINT NOT NULL, reason VARCHAR(500) NULL, before_state_json JSON NULL, after_state_json JSON NOT NULL,
    correlation_id VARCHAR(100) NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(revision_id),
    FOREIGN KEY(entry_id) REFERENCES lesson_log_entry(entry_id) ON DELETE RESTRICT, FOREIGN KEY(weekly_review_id) REFERENCES lesson_log_weekly_review(review_id) ON DELETE RESTRICT,
    FOREIGN KEY(policy_id) REFERENCES lesson_log_policy(policy_id) ON DELETE RESTRICT, FOREIGN KEY(actor_id) REFERENCES app_user(user_id),
    CHECK ((entry_id IS NOT NULL) + (weekly_review_id IS NOT NULL) + (policy_id IS NOT NULL) = 1)
);
CREATE INDEX idx_llr_entry_created ON lesson_log_revision(entry_id, created_at);

INSERT INTO lesson_log_policy
    (policy_version, effective_from, timezone, deadline_mode, edit_window_hours,
     require_homeroom_review, rubric_json, version, created_by)
SELECT 1, CURRENT_DATE, 'Asia/Ho_Chi_Minh', 'FIXED_HOURS', 48, TRUE,
       '{"A":"Tốt","B":"Khá","C":"Trung bình","D":"Yếu"}', 0, MIN(user_id)
FROM app_user
WHERE NOT EXISTS (SELECT 1 FROM lesson_log_policy);
