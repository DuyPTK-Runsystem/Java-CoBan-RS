CREATE TABLE teacher_load_rule (
    rule_id BIGINT NOT NULL AUTO_INCREMENT,
    policy_id BIGINT NOT NULL,
    rule_code VARCHAR(80) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    reduction_periods INT NOT NULL DEFAULT 0,
    source VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rule_id),
    CONSTRAINT uk_tlr_policy_code UNIQUE (policy_id, rule_code),
    CONSTRAINT fk_tlr_policy FOREIGN KEY (policy_id) REFERENCES teacher_load_policy (policy_id)
);

INSERT INTO teacher_load_rule
    (policy_id, rule_code, rule_name, trigger_type, reduction_periods, source)
SELECT policy_id, 'HOMEROOM', 'Giảm chủ nhiệm', 'HOMEROOM', homeroom_reduction, source
FROM teacher_load_policy;

INSERT INTO teacher_load_rule
    (policy_id, rule_code, rule_name, trigger_type, reduction_periods, source)
SELECT policy_id, 'NURSING_CHILD_UNDER_12M', 'Nuôi con nhỏ dưới 12 tháng', 'ELIGIBILITY', nursing_reduction, source
FROM teacher_load_policy;
