-- Thêm cột entity_id để thông báo có thể liên kết tới thực thể nghiệp vụ
-- (VD: phiếu bàn giao lô hàng). Người dùng bấm vào thông báo sẽ được
-- chuyển tới trang chi tiết của thực thể đó (NCL-05-CN-008/CN-009).
ALTER TABLE notifications
    ADD COLUMN entity_id CHAR(36) NULL AFTER content;

CREATE INDEX idx_notifications_entity_id
    ON notifications(entity_id);