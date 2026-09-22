package vn.nguongocso.farm.enums;
/**
 * Trạng thái vòng đời của lô sản xuất.
*/
public enum ProductionLotStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    HARVESTED,
    PREPROCESSED,
    PACKAGED,
    CLOSED,
    RECALLED,
    CANCELLED,
    DISPOSED
}
