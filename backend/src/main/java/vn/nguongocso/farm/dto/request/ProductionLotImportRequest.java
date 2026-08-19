package vn.nguongocso.farm.dto.request;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu nhập dữ liệu lô sản xuất từ tệp.
 */
@Getter
@Setter
public class ProductionLotImportRequest {
    private MultipartFile file; // Tệp CSV chứa dữ liệu lô sản xuất.

    private UUID organizationId; // Tổ chức được nhập dữ liệu (chỉ áp dụng cho VT-01).

}