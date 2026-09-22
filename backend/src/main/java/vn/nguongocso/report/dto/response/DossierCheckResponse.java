package vn.nguongocso.report.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO phản hồi kiểm tra tính đầy đủ hồ sơ. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierCheckResponse {

    private UUID shipmentId;

    private boolean eligible;

    private List<String> missingDocuments;
}