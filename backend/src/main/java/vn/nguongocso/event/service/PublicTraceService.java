package vn.nguongocso.event.service;

import vn.nguongocso.event.dto.response.PublicTraceResponse;

public interface PublicTraceService {
    PublicTraceResponse getPublicTrace(String shipmentCode);
}
