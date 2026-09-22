package vn.nguongocso.certification.service;

import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.farm.entity.ProductionLot;

import java.time.LocalDate;

/**
 * Dịch vụ tính toán và cung cấp thông tin hiệu lực kiểm nghiệm của lô sản xuất (NCL-11-CN-004).
 */
public interface InspectionValidityService {
        /**
         * Tính toán thông tin hiệu lực kiểm nghiệm của lô sản xuất theo ngày hiện tại của hệ thống.
         */
        InspectionValidityResponse calculateValidity(
                        ProductionLot lot);

        /**
         * Tính toán thông tin hiệu lực kiểm nghiệm của lô sản xuất theo ngày chỉ định.
         */
        InspectionValidityResponse calculateValidity(
                        ProductionLot lot,
                        LocalDate today);
}
