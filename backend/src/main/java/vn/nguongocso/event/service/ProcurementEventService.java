package vn.nguongocso.event.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordProcurementEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;

public interface ProcurementEventService {

    ChainEventResponse recordProcurementEvent(RecordProcurementEventRequest request, CustomUserDetails currentUser);
}
