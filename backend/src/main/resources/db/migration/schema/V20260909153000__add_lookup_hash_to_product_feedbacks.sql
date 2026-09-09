-- NCL-06-CN-005: lưu duy nhất SHA-256 của mã tra cứu phản ánh công khai.
-- Dữ liệu cũ để NULL vì hệ thống không thể cấp lại mã rõ cho người gửi một cách an toàn.
SET @lookup_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'product_feedbacks'
      AND COLUMN_NAME = 'lookup_code_hash'
);
SET @lookup_column_sql = IF(
    @lookup_column_exists = 0,
    'ALTER TABLE product_feedbacks ADD COLUMN lookup_code_hash CHAR(64) NULL',
    'SELECT 1'
);
PREPARE lookup_column_statement FROM @lookup_column_sql;
EXECUTE lookup_column_statement;
DEALLOCATE PREPARE lookup_column_statement;

SET @lookup_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'product_feedbacks'
      AND INDEX_NAME = 'uk_product_feedback_lookup_code_hash'
);
SET @lookup_index_sql = IF(
    @lookup_index_exists = 0,
    'CREATE UNIQUE INDEX uk_product_feedback_lookup_code_hash ON product_feedbacks(lookup_code_hash)',
    'SELECT 1'
);
PREPARE lookup_index_statement FROM @lookup_index_sql;
EXECUTE lookup_index_statement;
DEALLOCATE PREPARE lookup_index_statement;
