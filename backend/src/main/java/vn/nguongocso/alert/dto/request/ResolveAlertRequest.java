package vn.nguongocso.alert.dto.request;

import lombok.Getter;
import lombok.Setter;

/** 
 * Yêu cầu xử lý cảnh báo.
 */
@Getter
@Setter
public class ResolveAlertRequest {
    private String resolutionNote;
}