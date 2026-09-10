package vn.nguongocso.certification.service;

import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.farm.entity.ProductionLot;

import java.time.LocalDate;

/**
 * Dịch vụ tính toán và cung cấp thông tin hiệu lực kiểm nghiệm của lô sản xuất (NCL-11-CN-004).
 *
 * <p>
 * Tính toán trạng thái hiệu lực suy diễn tại thời điểm đọc (derived state):
 * NOT_REQUIRED, NO_VALID_RESULT, VALID, EXPIRING, EXPIRED.
 * </p>
 */
public interface InspectionValidityService {

    /**
     * Tính toán thông tin hiệu lực kiểm nghiệm của lô sản xuất theo ngày hiện tại của hệ thống.
     *
     * @param lot lô sản xuất cần tính toán
     * @return khối dữ liệu hiệu lực kiểm nghiệm
     */
    InspectionValidityResponse calculateValidity(ProductionLot lot);

    /**
     * Tính toán thông tin hiệu lực kiểm nghiệm của lô sản xuất theo ngày chỉ định.
     *
     * @param lot lô sản xuất cần tính toán
     * @param today ngày tính toán
     * @return khối dữ liệu hiệu lực kiểm nghiệm
     */
    InspectionValidityResponse calculateValidity(ProductionLot lot, LocalDate today);
}
