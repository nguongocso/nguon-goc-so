package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO dùng để hủy lô sản xuất (NCL-02-CN-006).
 */
@Getter
@Setter
public class CancelProductionLotRequest {

    @NotBlank(message = "Lý do hủy không được để trống")
    @Size(max = 100, message = "Lý do hủy không được vượt quá 100 ký tự")
    private String reason;

    /** "Tại sao?" — diễn giải lý do hủy, KHÔNG bắt buộc (quyết định 2026-09-04). */
    @Size(max = 1000, message = "Diễn giải lý do hủy không được vượt quá 1000 ký tự")
    private String note;
}