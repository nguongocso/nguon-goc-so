package vn.nguongocso.event.service;


import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;

public interface ChainEventService {
    ChainEventResponse recordHarvestEvent(RecordHarvestEventRequest request, CustomUserDetails currentUser);
}

