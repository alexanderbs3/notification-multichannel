CREATE TABLE notifications (
                               id BINARY(16) NOT NULL PRIMARY KEY,
                               channel ENUM('EMAIL', 'PUSH', 'SMS') NULL,
                               content VARCHAR(255) NOT NULL,
                               created_at DATETIME(6) NULL,
                               recipient VARCHAR(255) NOT NULL,
                               status ENUM('CREATED', 'PROCESSED', 'FAILED') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;