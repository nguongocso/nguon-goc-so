package vn.nguongocso.farm.enums;

import lombok.Getter;

/**
 * Enum đại diện cho 9 giai đoạn tiến độ trong chuỗi sản xuất và lưu thông của lô (NCL-10-CN-013).
 */
@Getter
public enum ChainProgressStage {
    DRAFT("Nháp"),
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    HARVESTED("Đã thu hoạch"),
    PREPROCESSED("Đã sơ chế"),
    WAITING_TEST_RESULT("Chờ kết quả kiểm nghiệm"),
    PACKAGED("Đã đóng gói"),
    TAG_ACTIVATED("Đã kích hoạt tem"),
    IN_CIRCULATION("Đang lưu thông");

    private final String stageName;

    ChainProgressStage(String stageName) {
        this.stageName = stageName;
    }
}
