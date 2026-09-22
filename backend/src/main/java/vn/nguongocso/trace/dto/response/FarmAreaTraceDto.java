package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** DTO response thông tin vùng trồng khi truy xuất. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmAreaTraceDto {
    private UUID id;

    private String code;

    private String name;

    private String location;

    private Double areaSize;
}
