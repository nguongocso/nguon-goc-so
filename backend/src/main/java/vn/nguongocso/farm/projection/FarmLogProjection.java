package vn.nguongocso.farm.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Hình chiếu dữ liệu nhật ký canh tác.
*/
public interface FarmLogProjection {
    /** Lấy mã nhật ký. */
    UUID getId();

    /** Lấy mã lô sản xuất. */
    UUID getProductionLotId();

    /** Lấy tên lô sản xuất. */
    String getProductionLotName();

    /** Lấy loại hoạt động canh tác. */
    FarmActivityType getActivityType();

    /** Lấy vật tư sử dụng. */
    String getMaterial();

    /** Lấy số lượng sử dụng. */
    Double getQuantity();

    /** Lấy đơn vị tính. */
    String getUnit();

    /** Lấy ngày thực hiện. */
    LocalDate getExecutedDate();

    /** Lấy ghi chú. */
    String getNotes();

    /** Lấy tên người tạo. */
    String getCreatedByName();

    /** Lấy thời điểm tạo. */
    LocalDateTime getCreatedAt();
}
