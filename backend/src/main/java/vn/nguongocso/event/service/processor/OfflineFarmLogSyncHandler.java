package vn.nguongocso.event.service.processor;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.event.service.mapper.OfflineFarmLogPayloadMapper;
import vn.nguongocso.farm.dto.request.CreateFarmLogRequest;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.service.FarmLogService;
import vn.nguongocso.permission.service.PermissionChecker;

/** Handler xử lý đồng bộ nhật ký canh tác (FARM_LOG) ghi nhận ngoại tuyến. */
@Component
@RequiredArgsConstructor
public class OfflineFarmLogSyncHandler {

    private final PermissionChecker permissionChecker;
    private final OfflineFarmLogPayloadMapper offlineFarmLogPayloadMapper;
    private final FarmLogService farmLogService;

    /**
     * Xử lý nhật ký canh tác ghi khi ngoại tuyến (NCL-10-CN-012).
     *
     * @param eventDto sự kiện ngoại tuyến loại FARM_LOG
     * @return ID bản ghi farm_logs vừa tạo
     */
    public UUID processFarmLogOffline(RecordOfflineEventDto eventDto) {
        permissionChecker.check("FARM_LOG", "CREATE");
        CreateFarmLogRequest farmLogRequest = offlineFarmLogPayloadMapper.toCreateFarmLogRequest(eventDto);
        FarmLogResponse created = farmLogService.create(farmLogRequest);
        return created != null ? created.getId() : null;
    }
}
