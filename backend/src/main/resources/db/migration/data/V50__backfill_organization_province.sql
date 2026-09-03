-- ============================================================
-- V37: Backfill organizations.province_id từ địa chỉ hiện có (NCL-740)
-- Depends on: administrative_units (V35), V36 (seed danh mục), organizations
-- ============================================================
--
-- Khớp TÊN tỉnh/thành phố (đã bỏ tiền tố "Tỉnh"/"Thành phố" trong danh mục,
-- xem V36) xuất hiện trong chuỗi organizations.address. So khớp một chiều
-- (tên đơn vị nằm TRONG địa chỉ) để giữ tính bảo thủ: không khớp chắc chắn
-- thì KHÔNG đoán.
--
-- Các dòng không khớp được giữ province_id = NULL và sẽ được VT-01 sửa tay
-- qua endpoint cập nhật mapping tổ chức (story API của NCL-670) — migration
-- này cố tình không suy diễn thêm.
--
-- Chỉ cập nhật các dòng chưa có mapping (province_id IS NULL) nên chạy lại
-- vẫn an toàn.

UPDATE organizations o
    JOIN administrative_units u
        ON u.level = 'PROVINCE'
       AND LOWER(o.address) LIKE LOWER(CONCAT('%', u.name, '%'))
SET o.province_id = u.id
WHERE o.province_id IS NULL;
