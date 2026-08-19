package vn.nguongocso.event.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordProcurementEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;

/** Ghi nhận sự kiện thu mua. */
public interface ProcurementEventService {

    /** Tạo sự kiện thu mua mới. */
    ChainEventResponse recordProcurementEvent(RecordProcurementEventRequest request, CustomUserDetails currentUser);
}
