package vn.nguongocso.publicapi.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.dto.request.LatLngDto;

/**
 * Thông tin ranh giới vùng trồng hiển thị công khai trên trang tra cứu tem.
 *
 * <p>
 * Chỉ phơi bày các trường an toàn theo QTN-12: tên vùng trồng, diện tích
 * tính toán và danh sách tọa độ để vẽ polygon. Không lộ ID người quản lý,
 * ghi chú nội bộ hay dữ liệu quản trị tổ chức.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicFarmAreaBoundaryDto {

    /** ID vùng trồng (dùng làm key trên UI). */
    private UUID id;

    /** Tên vùng trồng. */
    private String name;

    /** Diện tích tính toán từ ranh giới, đơn vị hecta. */
    private BigDecimal calculatedArea;

    /**
     * Danh sách tọa độ đỉnh polygon theo thứ tự nối vòng.
     * Rỗng khi vùng trồng chưa được khoanh ranh giới.
     */
    private List<LatLngDto> points;
}
