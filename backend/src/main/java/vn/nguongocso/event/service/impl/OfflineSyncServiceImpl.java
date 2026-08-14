package vn.nguongocso.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.OfflineEventSyncRequest;
import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.event.dto.response.OfflineEventSyncResponse;
import vn.nguongocso.event.dto.response.OfflineEventSyncResultDto;
import vn.nguongocso.event.service.OfflineSyncService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
/** Đồng bộ các sự kiện ngoại tuyến. */
public class OfflineSyncServiceImpl implements OfflineSyncService {

    private final OfflineSyncEventProcessor eventProcessor;

    /** Đồng bộ danh sách sự kiện ngoại tuyến. */
    @Override
    public OfflineEventSyncResponse syncOfflineEvents(OfflineEventSyncRequest request, CustomUserDetails currentUser) {
        log.info("Bắt đầu xử lý đồng bộ ngoại tuyến cho syncId: {}", request.getSyncId());

        List<OfflineEventSyncResultDto> results = new ArrayList<>();
        int successCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;

        for (RecordOfflineEventDto eventDto : request.getEvents()) {
            // Gọi processor để xử lý từng event (mỗi event trong transaction riêng)
            OfflineEventSyncResultDto result = eventProcessor.processEvent(eventDto, request.getSyncId(), currentUser);
            results.add(result);

            // Cập nhật thống kê
            if ("SUCCESS".equals(result.getStatus())) {
                successCount++;
            } else if ("DUPLICATE".equals(result.getStatus())) {
                duplicateCount++;
            } else {
                failedCount++;
            }
        }

        return OfflineEventSyncResponse.builder()
                .syncId(request.getSyncId())
                .totalEvents(request.getEvents().size())
                .successCount(successCount)
                .duplicateCount(duplicateCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }
}