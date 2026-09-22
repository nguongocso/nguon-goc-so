package vn.nguongocso.farm.util;
/**
 * Ngoại lệ khi đọc tệp nhập lô sản xuất.
*/
public class ProductionLotImportException extends RuntimeException {
    /** Khởi tạo ngoại lệ nhập lô sản xuất. */
    public ProductionLotImportException(String message) {
        super(message);
    }
}