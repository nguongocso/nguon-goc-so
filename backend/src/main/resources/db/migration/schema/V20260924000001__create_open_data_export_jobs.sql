-- Migration tạo bảng quản lý tác vụ xuất dữ liệu mở bất đồng bộ (Async Export Job Pattern)
CREATE TABLE IF NOT EXISTS open_data_export_jobs (
    id CHAR(36) NOT NULL,
    requested_by CHAR(36),
    requested_by_username VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    format VARCHAR(20),
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    error_message TEXT,
    created_at DATETIME NOT NULL,
    completed_at DATETIME,
    PRIMARY KEY (id),
    INDEX idx_open_data_jobs_status (status),
    INDEX idx_open_data_jobs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
