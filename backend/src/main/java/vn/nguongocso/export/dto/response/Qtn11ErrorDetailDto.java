package vn.nguongocso.export.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO chứa thông tin chi tiết các mục bị thiếu theo quy tắc QTN-11 của một lô hàng/shipment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Qtn11ErrorDetailDto {
    private UUID id;
    private String name;
    private String lotCode;
    private List<String> missingEvents;
    private Boolean missingDocs;
    private List<String> missingDocDetails;
}
