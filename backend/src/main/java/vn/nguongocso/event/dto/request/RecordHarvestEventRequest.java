package vn.nguongocso.event.dto.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class RecordHarvestEventRequest {

    @NotNull(message = "Vui lòng chọn lô sản xuất")
    private UUID productionLotId;

    @NotNull(message = "Vui lòng chọn ngày thu hoạch")
    private LocalDate harvestDate;

    @NotNull(message = "Sản lượng thu hoạch không được để trống")
    @Positive(message = "Sản lượng thu hoạch phải lớn hơn 0")
    private Double quantity;

    private Double latitude;
    private Double longitude;
}

