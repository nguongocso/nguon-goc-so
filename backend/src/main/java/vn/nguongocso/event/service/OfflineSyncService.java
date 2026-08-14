package vn.nguongocso.event.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.OfflineEventSyncRequest;
import vn.nguongocso.event.dto.response.OfflineEventSyncResponse;

/** Đồng bộ các sự kiện ghi nhận ngoại tuyến. */
public interface OfflineSyncService {

    /** Đồng bộ danh sách sự kiện ngoại tuyến được gửi lên. */
    OfflineEventSyncResponse syncOfflineEvents(OfflineEventSyncRequest request, CustomUserDetails currentUser);
}
