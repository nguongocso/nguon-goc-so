-- ============================================================
-- V20260924000000: Bổ sung các chỉ mục tổng hợp (Composite Indexes)
-- tối ưu hóa hiệu năng cho các truy vấn thống kê và xuất dữ liệu lớn
-- ============================================================

-- Tối ưu hóa thống kê quét mã và phát hiện quét bất thường theo thời gian
CREATE INDEX idx_scan_logs_abnormal_scanned_at ON trace_code_scan_logs(is_abnormal, scanned_at);

-- Tối ưu hóa gom nhóm thống kê quét mã theo địa điểm và thời gian
CREATE INDEX idx_scan_logs_location_scanned_at ON trace_code_scan_logs(location, scanned_at);

-- Tối ưu hóa đếm số lượng tem kích hoạt theo trạng thái và thời điểm kích hoạt
CREATE INDEX idx_trace_codes_status_activated_at ON trace_codes(status, activated_at);

-- Tối ưu hóa tra cứu tem truy xuất theo lô hàng và trạng thái
CREATE INDEX idx_trace_codes_shipment_status ON trace_codes(shipment_id, status);

-- Tối ưu hóa tra cứu sự kiện chuỗi cung ứng theo loại và thời gian ghi nhận
CREATE INDEX idx_chain_events_unassigned_type ON chain_events(event_type, recorded_at, shipment_id);
