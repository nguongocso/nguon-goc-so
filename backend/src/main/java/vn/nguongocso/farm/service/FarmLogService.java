package vn.nguongocso.farm.service;

import java.util.UUID;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;

/**
 * Nghiệp vụ quản lý nhật ký canh tác.
 */
public interface FarmLogService {

    /**
     * Tạo nhật ký canh tác.
     *
     * @param request thông tin nhật ký
     * @return thông tin nhật ký đã tạo
     */
    FarmLogResponse create(CreateFarmLogRequest request);

    /**
     * Lấy danh sách nhật ký canh tác của lô sản xuất theo phân trang.
     *
     * @param productionLotId mã lô sản xuất
     * @param page            số trang, bắt đầu từ 0
     * @param size            số bản ghi trên mỗi trang
     * @return dữ liệu nhật ký canh tác theo phân trang
     */
    PageResponse<FarmLogResponse> getFarmLogsByProductionLot(
            UUID productionLotId,
            int page,
            int size);
}
