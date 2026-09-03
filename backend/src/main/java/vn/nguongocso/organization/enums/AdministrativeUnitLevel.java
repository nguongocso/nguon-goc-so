package vn.nguongocso.organization.enums;

/**
 * Cấp đơn vị hành chính trong danh mục dùng chung.
 *
 * <p>
 * Theo mô hình hành chính 2 cấp hiệu lực từ 01/07/2025: cấp tỉnh
 * (tỉnh/thành phố trực thuộc trung ương) và cấp xã (xã/phường/đặc khu).
 * Lưu VARCHAR trong DB ({@code @Enumerated(EnumType.STRING)}) để có thể mở
 * rộng thêm giá trị mới (ví dụ DISTRICT cho dữ liệu lịch sử 3 cấp) mà không
 * phải đổi kiểu cột.
 * </p>
 */
public enum AdministrativeUnitLevel {
	/** Cấp tỉnh / thành phố trực thuộc trung ương. */
	PROVINCE,

	/** Cấp xã / phường / đặc khu. */
	COMMUNE
}
