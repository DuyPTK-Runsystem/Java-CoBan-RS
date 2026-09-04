CREATE TABLE role (
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (role_id),
    CONSTRAINT uk_role_code UNIQUE (code)
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (role_id)
);

INSERT INTO role (code, name, description) VALUES
    ('ADMIN', 'Administrator', 'Quản trị kỹ thuật và tài khoản'),
    ('ACADEMIC_OFFICE', 'Academic Office', 'Giáo vụ'),
    ('TEACHER', 'Teacher', 'Giáo viên'),
    ('STUDENT', 'Student', 'Học sinh');

INSERT INTO user_role (user_id, role_id, assigned_at)
SELECT app_user.user_id, role.role_id, CURRENT_TIMESTAMP
FROM app_user
JOIN role ON role.code = 'ADMIN';
