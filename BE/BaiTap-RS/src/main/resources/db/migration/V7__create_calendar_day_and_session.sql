CREATE TABLE calendar_day (
    calendar_day_id BIGINT NOT NULL AUTO_INCREMENT,
    academic_year_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    calendar_date DATE NOT NULL,
    day_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    configured_by BIGINT NOT NULL,
    configured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (calendar_day_id),
    CONSTRAINT uk_calendar_day_year_date UNIQUE (academic_year_id, calendar_date),
    CONSTRAINT ck_calendar_day_type CHECK (day_type IN ('SCHOOL_DAY', 'WEEKEND', 'HOLIDAY', 'NO_CLASS')),
    CONSTRAINT fk_calendar_day_year FOREIGN KEY (academic_year_id)
        REFERENCES academic_year (academic_year_id),
    CONSTRAINT fk_calendar_day_semester FOREIGN KEY (semester_id)
        REFERENCES semester (semester_id),
    CONSTRAINT fk_calendar_day_configured_by FOREIGN KEY (configured_by)
        REFERENCES app_user (user_id),
    CONSTRAINT fk_calendar_day_updated_by FOREIGN KEY (updated_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_calendar_day_semester_date ON calendar_day (semester_id, calendar_date);

CREATE TABLE calendar_session (
    calendar_session_id BIGINT NOT NULL AUTO_INCREMENT,
    calendar_day_id BIGINT NOT NULL,
    session_period VARCHAR(20) NOT NULL,
    session_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    configured_by BIGINT NOT NULL,
    configured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (calendar_session_id),
    CONSTRAINT uk_calendar_session_day_period UNIQUE (calendar_day_id, session_period),
    CONSTRAINT ck_calendar_session_period CHECK (session_period IN ('MORNING', 'AFTERNOON')),
    CONSTRAINT ck_calendar_session_status CHECK (session_status IN ('SCHEDULED', 'NO_CLASS')),
    CONSTRAINT fk_calendar_session_day FOREIGN KEY (calendar_day_id)
        REFERENCES calendar_day (calendar_day_id),
    CONSTRAINT fk_calendar_session_configured_by FOREIGN KEY (configured_by)
        REFERENCES app_user (user_id),
    CONSTRAINT fk_calendar_session_updated_by FOREIGN KEY (updated_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_calendar_session_status_period
    ON calendar_session (session_status, session_period);
