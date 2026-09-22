package vn.nguongocso.farm.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

/**
 * Yêu cầu nhập dữ liệu lô sản xuất từ tệp.
*/
@Getter
@Setter
public class ProductionLotImportRequest {
    private MultipartFile file;

    private UUID organizationId;
}