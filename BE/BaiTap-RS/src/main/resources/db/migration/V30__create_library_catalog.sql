CREATE TABLE book (
    book_id BIGINT NOT NULL AUTO_INCREMENT,
    isbn VARCHAR(20) NULL,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(200) NOT NULL,
    publisher VARCHAR(200) NULL,
    published_year INT NULL,
    category VARCHAR(100) NULL,
    list_price DECIMAL(12,2) NULL,
    cover_url VARCHAR(2048) NULL,
    archived_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (book_id),
    UNIQUE KEY uk_book_isbn (isbn),
    KEY idx_book_active_title (archived_at, title),
    KEY idx_book_category_year (category, published_year)
);

CREATE TABLE book_copy (
    book_copy_id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    barcode VARCHAR(64) NOT NULL,
    shelf_location VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    reference_only BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (book_copy_id),
    UNIQUE KEY uk_book_copy_barcode (barcode),
    KEY idx_book_copy_book_status (book_id, status, reference_only),
    CONSTRAINT fk_book_copy_book FOREIGN KEY (book_id) REFERENCES book (book_id)
);

CREATE TABLE book_copy_batch_request (
    batch_request_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    payload_fingerprint CHAR(64) NOT NULL,
    response_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (batch_request_id),
    UNIQUE KEY uk_book_copy_batch_actor_book_key (actor_user_id, book_id, idempotency_key),
    CONSTRAINT fk_book_copy_batch_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_book_copy_batch_book FOREIGN KEY (book_id) REFERENCES book (book_id)
);
