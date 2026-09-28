-- NCL-08-CN-008 / NCL-08-CN-009: phạm vi thu hồi là một lô hàng, không phải
-- toàn bộ lô sản xuất. Giữ production_lot_id để hiển thị ngữ cảnh và tương thích
-- với dữ liệu cũ.
ALTER TABLE recall_requests
    ADD COLUMN shipment_id CHAR(36) NULL AFTER production_lot_id;

-- Phản ánh có mã tem xác định chính xác lô hàng cần thu hồi.
UPDATE recall_requests rr
JOIN product_feedbacks pf ON pf.id = rr.source_feedback_id
JOIN trace_codes tc ON tc.id = pf.trace_code_id
SET rr.shipment_id = tc.shipment_id
WHERE rr.shipment_id IS NULL;

-- Với yêu cầu cũ không đi từ phản ánh, chỉ tự ánh xạ khi lô sản xuất có đúng
-- một lô hàng. Trường hợp mơ hồ được giữ NULL và backend sẽ yêu cầu tạo lại.
UPDATE recall_requests rr
JOIN (
    SELECT production_lot_id, MIN(id) AS shipment_id
    FROM shipments
    GROUP BY production_lot_id
    HAVING COUNT(*) = 1
) single_shipment ON single_shipment.production_lot_id = rr.production_lot_id
SET rr.shipment_id = single_shipment.shipment_id
WHERE rr.shipment_id IS NULL;

ALTER TABLE recall_requests
    ADD CONSTRAINT fk_recall_request_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipments(id);

CREATE INDEX idx_recall_request_shipment_status
    ON recall_requests(shipment_id, status);
