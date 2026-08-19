package vn.nguongocso.alert.entity;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Chi tiết cảnh báo, gồm danh sách điểm quét và số lượng.
 */
@Getter
@Setter
public class AlertDetails {
    private List<ScanPoint> locations;

    private Integer scanCount;

    private Integer thresholdConfigured;
}