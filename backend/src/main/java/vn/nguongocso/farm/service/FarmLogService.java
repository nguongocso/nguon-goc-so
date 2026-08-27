package vn.nguongocso.farm.service;

import java.util.UUID;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.CorrectFarmLogRequest;
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
     * NCL-03-CN-006: đính chính một nhật ký canh tác.
     *
     * <p>Bản gốc được giữ nguyên và đánh dấu đã đính chính; hệ thống tạo một
     * bản ghi mới liên kết tới bản gốc với lý do đính chính bắt buộc.</p>
     *
     * @param id      ID của nhật ký cần đính chính
     * @param request dữ liệu đính chính và lý do
     * @return thông tin bản ghi đính chính vừa tạo
     */
    FarmLogResponse correctFarmLog(UUID id, CorrectFarmLogRequest request);

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
