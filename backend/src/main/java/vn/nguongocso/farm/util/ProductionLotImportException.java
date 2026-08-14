package vn.nguongocso.farm.util;

/**
 * Exception xảy ra trong quá trình đọc tệp nhập.
 */
public class ProductionLotImportException extends RuntimeException {
    /**
     * Tạo một ProductionLotImportException với thông điệp lỗi.
     *
     * @param message thông điệp lỗi
     */
    public ProductionLotImportException(String message) {
        super(message);
    }
}