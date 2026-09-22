package vn.nguongocso.farm.service;

import java.util.UUID;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.CorrectFarmLogRequest;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
/**
 * Nghiệp vụ nhật ký canh tác.
*/
public interface FarmLogService {
    /** Tạo nhật ký canh tác. */
    FarmLogResponse create(CreateFarmLogRequest request);

    /** Đính chính nhật ký canh tác. */
    FarmLogResponse correctFarmLog(UUID id, CorrectFarmLogRequest request);

    /** Lấy chi tiết nhật ký canh tác theo ID. */
    FarmLogResponse getFarmLog(UUID id);

    /** Lấy danh sách nhật ký canh tác của lô sản xuất. */
    PageResponse<FarmLogResponse> getFarmLogsByProductionLot(
            UUID productionLotId,
            int page,
            int size);
}
