ALTER TABLE student_info
    ADD COLUMN gender VARCHAR(20) NULL;

ALTER TABLE student_info
    ADD CONSTRAINT ck_student_info_gender
    CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'));
