package vn.nguongocso.farm.util;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;
/**
 * Đọc tệp nhập lô sản xuất.
*/
public interface ProductionLotImportFileParser {
    /** Phân tích tệp Excel thành danh sách dòng nhập lô sản xuất. */
    List<ProductionLotImportRow> parse(MultipartFile file);
}