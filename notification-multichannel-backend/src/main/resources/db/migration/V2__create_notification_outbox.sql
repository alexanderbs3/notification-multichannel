CREATE TABLE notification_outbox (
                                     id BINARY(16) NOT NULL PRIMARY KEY,
                                     created_at DATETIME(6) NOT NULL,
                                     destination VARCHAR(255) NOT NULL,
                                     payload TEXT NOT NULL,
                                     status ENUM('PENDING', 'SENT', 'FAILED') NOT NULL,
                                     processed_at DATETIME(6) NULL,
                                     error_message TEXT NULL,
                                     retry_count INT DEFAULT 0,
                                     INDEX idx_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;