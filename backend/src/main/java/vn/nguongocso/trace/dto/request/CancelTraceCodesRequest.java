package vn.nguongocso.trace.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelTraceCodesRequest {

    @NotBlank(message = "Loại hình hủy không được để trống ('RANGE' hoặc 'SINGLE')")
    private String cancelType;

    private String fromCode;

    private String toCode;

    private List<String> codeValues;

    @NotBlank(message = "Lý do hủy không được để trống")
    private String reasonType;

    private String reasonNote;
}
