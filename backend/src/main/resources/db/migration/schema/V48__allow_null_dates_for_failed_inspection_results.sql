-- ============================================================
-- V39: Cho phép null ngày cấp / ngày hết hạn khi chỉ tiêu không đạt
-- ============================================================
--
-- Khi người dùng chọn "Không đạt" (passed = false) cho một chỉ tiêu
-- kiểm nghiệm, ngày cấp (result_date) và ngày hết hạn (expiry_date)
-- sẽ được gửi lên backend là null vì chỉ tiêu không đạt không có
-- hiệu lực thời gian. Hai cột này chỉ bắt buộc khi passed = true.
--
ALTER TABLE inspection_criterion_results MODIFY result_date DATE NULL;
ALTER TABLE inspection_criterion_results MODIFY expiry_date DATE NULL;
