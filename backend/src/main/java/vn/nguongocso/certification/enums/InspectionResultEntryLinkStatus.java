package vn.nguongocso.certification.enums;

/**
 * Trạng thái vòng đời của liên kết nhập kết quả kiểm nghiệm.
 */
public enum InspectionResultEntryLinkStatus {
    ACTIVE, // Liên kết đang hoạt động và chưa được sử dụng.

    USED, // Liên kết đã được sử dụng để nhập kết quả kiểm nghiệm.

    REVOKED, // Liên kết đã bị thu hồi và không còn hiệu lực.

    EXPIRED // Liên kết đã hết hạn sử dụng.
}
