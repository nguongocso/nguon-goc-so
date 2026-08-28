package vn.nguongocso.farm.service;

import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;

import java.util.UUID;

/**
 * Dịch vụ kiểm tra và tính toán thời gian cách ly đủ điều kiện thu hoạch (NCL-681 / NCL-843).
 */
public interface HarvestEligibilityService {

    /**
     * Tính toán ngày sớm nhất đủ điều kiện thu hoạch dựa trên toàn bộ nhật ký PESTICIDE của lô.
     *
     * @param productionLotId ID của lô sản xuất
     * @return thông tin đánh giá tính đủ điều kiện thu hoạch
     */
    HarvestEligibilityResponse calculateHarvestEligibility(UUID productionLotId);
}
