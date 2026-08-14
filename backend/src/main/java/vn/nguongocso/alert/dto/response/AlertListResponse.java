package vn.nguongocso.alert.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** Danh sách cảnh báo. */
@Getter
@Setter
public class AlertListResponse {
    private List<AlertResponse> content;

    private Integer totalElements;

    private Integer totalPages;

    private Integer page;

    private Integer size;
}