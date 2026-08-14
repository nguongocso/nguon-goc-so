package vn.nguongocso.farm.enums;

/**
 * Trạng thái của một lần nhập dữ liệu lô sản xuất.
 */
public enum ProductionLotImportStatus {
    SUCCESS, // Thành công

    PARTIAL_SUCCESS, // Thành công một phần

    FAILED // Thất bại
}