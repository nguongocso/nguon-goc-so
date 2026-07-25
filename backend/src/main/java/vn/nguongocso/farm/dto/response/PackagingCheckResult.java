package vn.nguongocso.farm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.enums.ProductionLotStatus;

import java.util.List;
import java.util.UUID;

/**
 * Kết quả kiểm tra điều kiện đóng gói của lô sản xuất.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagingCheckResult {

    private UUID lotId;

    private ProductionLotStatus status;

    private boolean canPackage;

    private List<String> missingLogs;

    private String message;
}