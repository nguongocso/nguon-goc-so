package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LockTraceCodeRequest {
    @NotBlank(message = "Vui lòng nhập lý do khóa")
    @Size(min = 10, max = 500, message = "Lý do khóa phải từ 10 đến 500 ký tự")
    private String reason;
}