import {
    apiRequest
} from "../core/api-client.js";

/**
 * Tạo lô sản xuất mới.
 *
 * POST /api/v1/production-lots
 *
 * @param {Object} productionLotData
 * @returns {Promise<Object>}
 */
export async function createProductionLot(
    productionLotData
) {
    return apiRequest(
        "/production-lots",
        {
            method: "POST",
            body: JSON.stringify(
                productionLotData
            )
        }
    );
}

/**
 * Lấy danh sách vùng trồng của tổ chức.
 *
 * GET /api/v1/farm-areas
 *
 * @returns {Promise<Object>}
 */
export async function getFarmAreas() {
    return apiRequest(
        "/farm-areas",
        {
            method: "GET"
        }
    );
}

/**
 * Lấy danh sách loại nông sản.
 *
 * GET /api/v1/product-categories
 *
 * @returns {Promise<Object>}
 */
export async function getProductCategories() {
    return apiRequest(
        "/product-categories",
        {
            method: "GET"
        }
    );
}

/**
 * Lấy danh sách lô sản xuất.
 *
 * GET /api/v1/production-lots
 *
 * @returns {Promise<Object>}
 */
export async function getProductionLots() {
    return apiRequest(
        "/production-lots",
        {
            method: "GET"
        }
    );
}

/**
 * Cập nhật lô sản xuất.
 *
 * Chỉ lô có trạng thái DRAFT mới được sửa.
 *
 * PUT /api/v1/production-lots/{id}
 *
 * @param {string} id
 * @param {Object} productionLotData
 * @returns {Promise<Object>}
 */
export async function updateProductionLot(
    id,
    productionLotData
) {
    return apiRequest(
        `/production-lots/${encodeURIComponent(id)}`,
        {
            method: "PUT",
            body: JSON.stringify(
                productionLotData
            )
        }
    );
}

/**
 * Gửi lô sản xuất để duyệt.
 *
 * DRAFT -> PENDING
 *
 * POST /api/v1/production-lots/{id}/submit
 *
 * @param {string} id
 * @returns {Promise<Object>}
 */
export async function submitProductionLot(id) {
    return apiRequest(
        `/production-lots/${encodeURIComponent(id)}/submit`,
        {
            method: "POST"
        }
    );
}

/**
 * Duyệt lô sản xuất.
 *
 * PENDING -> APPROVED
 *
 * POST /api/v1/production-lots/{id}/approve
 *
 * @param {string} id
 * @returns {Promise<Object>}
 */
export async function approveProductionLot(id) {
    return apiRequest(
        `/production-lots/${encodeURIComponent(id)}/approve`,
        {
            method: "POST",
            body: JSON.stringify({
                approved: true
            })
        }
    );
}

/**
 * Từ chối và trả lô về trạng thái DRAFT.
 *
 * PENDING -> DRAFT
 *
 * POST /api/v1/production-lots/{id}/approve
 *
 * Backend nhận trường reason.
 *
 * @param {string} id
 * @param {string} reason
 * @returns {Promise<Object>}
 */
export async function returnToDraftProductionLot(
    id,
    reason
) {
    const normalizedReason =
        String(reason || "").trim();

    if (!normalizedReason) {
        throw new Error(
            "Vui lòng nhập lý do từ chối."
        );
    }

    return apiRequest(
        `/production-lots/${encodeURIComponent(id)}/approve`,
        {
            method: "POST",
            body: JSON.stringify({
                approved: false,
                reason: normalizedReason
            })
        }
    );
}