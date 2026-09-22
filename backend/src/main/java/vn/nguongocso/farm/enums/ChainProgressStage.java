package vn.nguongocso.farm.enums;

import lombok.Getter;

/** Các giai đoạn tiến độ trong chuỗi sản xuất và lưu thông của lô. */
@Getter
public enum ChainProgressStage {
    DRAFT("Nháp"), // Giai đoạn nháp

    PENDING("Chờ duyệt"), // Chờ duyệt

    APPROVED("Đã duyệt"), // Đã duyệt

    HARVESTED("Đã thu hoạch"), // Đã thu hoạch

    PREPROCESSED("Đã sơ chế"), // Đã sơ chế

    WAITING_TEST_RESULT("Chờ kết quả kiểm nghiệm"), // Chờ kết quả kiểm nghiệm

    PACKAGED("Đã đóng gói"), // Đã đóng gói

    TAG_ACTIVATED("Đã kích hoạt tem"), // Đã kích hoạt tem

    IN_CIRCULATION("Đang lưu thông"); // Đang lưu thông

    private final String stageName;

    /** Khởi tạo giai đoạn tiến độ chuỗi. */
    ChainProgressStage(String stageName) {
        this.stageName = stageName;
    }
}
