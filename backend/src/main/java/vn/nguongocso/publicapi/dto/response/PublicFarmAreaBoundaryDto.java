package vn.nguongocso.publicapi.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import vn.nguongocso.farm.dto.request.LatLngDto;

/** Thông tin ranh giới vùng trồng hiển thị công khai trên trang tra cứu tem. */
@Getter
@Builder
public class PublicFarmAreaBoundaryDto {
    /** ID vùng trồng (dùng làm key trên UI). */
    private UUID id;

    /** Tên vùng trồng. */
    private String name;

    /** Diện tích tính toán từ ranh giới, đơn vị hecta. */
    private BigDecimal calculatedArea;

    /** Danh sách tọa độ đỉnh polygon theo thứ tự nối vòng. */
    private List<LatLngDto> points;
}
