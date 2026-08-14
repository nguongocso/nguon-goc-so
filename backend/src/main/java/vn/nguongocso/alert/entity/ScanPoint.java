package vn.nguongocso.alert.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * Điểm quét với tọa độ và thời gian.
 */
@Getter
@Setter
public class ScanPoint {
    private Double latitude;

    private Double longitude;

    private LocalDateTime scannedAt;
}