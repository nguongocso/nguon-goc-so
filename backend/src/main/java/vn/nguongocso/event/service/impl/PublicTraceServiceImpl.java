package vn.nguongocso.event.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.PublicTraceResponse;
import vn.nguongocso.event.service.PublicTraceService;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.TraceCodeRepository;

@RequiredArgsConstructor
@Service
public class PublicTraceServiceImpl implements PublicTraceService {

    private final TraceCodeRepository traceCodeRepository;
    private final ChainEventResponse chainEventResponse;

    @Override
    public PublicTraceResponse getPublicTrace(String shipmentCode) {
        return null;
    }

    private TraceCode findTraceCode(String codeValue){
        return traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND),
            );
    } 
    
}
