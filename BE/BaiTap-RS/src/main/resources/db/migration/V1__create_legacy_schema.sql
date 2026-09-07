CREATE TABLE `user` (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_user_name UNIQUE (user_name)
);

CREATE TABLE student (
    student_id BIGINT NOT NULL AUTO_INCREMENT,
    student_name VARCHAR(35) NOT NULL,
    student_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (student_id),
    CONSTRAINT uk_student_student_code UNIQUE (student_code)
);

CREATE TABLE student_info (
    info_id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    address VARCHAR(255) NULL,
    average_score DOUBLE NULL,
    date_of_birth DATE NULL,
    PRIMARY KEY (info_id),
    CONSTRAINT uk_student_info_student UNIQUE (student_id),
    CONSTRAINT fk_student_info_student FOREIGN KEY (student_id) REFERENCES student (student_id)
);
