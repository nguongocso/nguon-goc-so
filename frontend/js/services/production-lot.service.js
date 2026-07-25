import {
    apiRequest
} from "../core/api-client.js";

/**
 * Create a new production lot.
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
 * Fetch all farm areas for the
 * authenticated organization.
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
 * Fetch all product categories.
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
 * Fetch all production lots for the
 * authenticated organization.
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
 * Update a production lot.
 *
 * Chỉ lô có trạng thái DRAFT
 * mới được chỉnh sửa.
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
 * Submit a production lot for approval.
 *
 * Chuyển trạng thái:
 * DRAFT -> PENDING
 *
 * POST /api/v1/production-lots/{id}/submit
 *
 * @param {string} id
 * @returns {Promise<Object>}
 */
export async function submitProductionLot(
    id
) {
    return apiRequest(
        `/production-lots/${id}/submit`,
        {
            method: "POST"
        }
    );
}

/**
 * Approve or reject a production lot.
 *
 * Chuyển trạng thái:
 * PENDING -> APPROVED (when approved: true)
 *
 * POST /api/v1/production-lots/{id}/approve
 *
 * @param {string} id
 * @returns {Promise<Object>}
 */
export async function approveProductionLot(id) {
    return apiRequest(
        `/production-lots/${id}/approve`,
        {
            method: "POST",
            body: JSON.stringify({
                approved: true,
                reason: ""
            })
        }
    );
}

/**
 * Return a production lot to draft.
 *
 * Chuyển trạng thái:
 * PENDING -> DRAFT
 *
 * POST /api/v1/production-lots/{id}/approve false
 *
 * @param {string} id
 * @returns {Promise<Object>}
 */
export async function returnToDraftProductionLot(id, reason) {
    return apiRequest(
        `/production-lots/${id}/approve`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                approved: false,
                reason: reason.trim()
            })
        }
    );
}

/**
 * Kiểm tra điều kiện đóng gói của lô sản xuất.
 *
 * Chỉ có hiệu lực khi lô ở trạng thái HARVESTED.
 *
 * GET /api/v1/production-lots/{lotId}/packaging-check
 *
 * @param {string} lotId
 * @returns {Promise<Object>}
 */
export async function checkPackagingReadiness(lotId) {
    return apiRequest(
        `/production-lots/${encodeURIComponent(lotId)}/packaging-check`,
        {
            method: "GET"
        }
    );
}

/**
 * Đóng gói lô sản xuất.
 *
 * Chuyển trạng thái: HARVESTED -> PACKAGED
 *
 * POST /api/v1/production-lots/{lotId}/package
 *
 * @param {string} lotId
 * @returns {Promise<Object>}
 */
export async function packageProductionLot(lotId) {
    return apiRequest(
        `/production-lots/${encodeURIComponent(lotId)}/package`,
        {
            method: "POST"
        }
    );
}
