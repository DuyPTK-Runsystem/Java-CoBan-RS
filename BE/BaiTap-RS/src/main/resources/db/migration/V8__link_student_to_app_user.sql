ALTER TABLE student ADD COLUMN user_id BIGINT NULL;

ALTER TABLE student ADD CONSTRAINT uk_student_user UNIQUE (user_id);

ALTER TABLE student ADD CONSTRAINT fk_student_user FOREIGN KEY (user_id)
    REFERENCES app_user (user_id);
