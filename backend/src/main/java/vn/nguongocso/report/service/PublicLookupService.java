package vn.nguongocso.report.service;

import vn.nguongocso.report.dto.response.LookupResponse;

public interface PublicLookupService {
    LookupResponse lookupCode(String codeValue, Double latitude, Double longitude, String location, String ipAddress, String userAgent);
}
