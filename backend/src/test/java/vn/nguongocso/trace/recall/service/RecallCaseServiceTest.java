package vn.nguongocso.trace.recall.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RecallCaseServiceTest {

    @Autowired
    private RecallCaseService recallCaseService;

    @Test
    void testCloseRequiresRemediationMeasures() {
        // TC: thiếu remediationMeasures → lỗi 400 (được thực hiện qua controller/service)
        CloseRecallCaseRequest req = new CloseRecallCaseRequest();
        req.setLots(java.util.Collections.emptyList());
        // TC-02: thiếu remediationMeasures → lỗi 400
        assertThrows(Exception.class, () -> recallCaseService.close(UUID.randomUUID(), req));
    }

    @Test
    void testCloseCaseNotFound() {
        // TC-01: case không tồn tại → lỗi 404
        CloseRecallCaseRequest req = new CloseRecallCaseRequest();
        req.setRemediationMeasures("Biện pháp khắc phục phòng ngừa: kiểm soát nguồn nguyên liệu.");
        assertThrows(Exception.class, () -> recallCaseService.close(UUID.randomUUID(), req));
    }
}
