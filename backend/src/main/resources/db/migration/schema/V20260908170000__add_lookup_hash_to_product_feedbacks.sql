-- NCL-06-CN-005: lưu duy nhất SHA-256 của mã tra cứu phản ánh công khai.
-- Dữ liệu cũ để NULL vì hệ thống không thể cấp lại mã rõ cho người gửi một cách an toàn.
ALTER TABLE product_feedbacks
    ADD COLUMN lookup_code_hash CHAR(64) NULL;

CREATE UNIQUE INDEX uk_product_feedback_lookup_code_hash
    ON product_feedbacks(lookup_code_hash);
