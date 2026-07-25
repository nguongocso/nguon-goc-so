import {
    apiRequest
} from "../core/api-client.js";

/**
 * Tạo một nhật ký canh tác.
 *
 * POST /api/v1/farm-logs
 *
 * Yêu cầu role EVENT_RECODER (VT-03).
 *
 * @param {Object} farmLogData
 * @returns {Promise<Object>}
 */
export async function createFarmLog(
    farmLogData
) {
    return apiRequest(
        "/farm-logs",
        {
            method: "POST",
            body: JSON.stringify(
                farmLogData
            )
        }
    );
}

/**
 * Lấy lịch sử nhật ký canh tác theo lô sản xuất.
 *
 * GET /api/v1/farm-logs?productionLotId=...&page=...&size=...
 *
 * Yêu cầu role VT-02 (Quản lý hợp tác xã).
 *
 * @param {string} productionLotId
 * @param {number} page
 * @param {number} size
 * @returns {Promise<Object>}
 */
export async function getFarmLogHistory(
    productionLotId,
    page = 0,
    size = 10
) {
    if (!productionLotId) {
        throw new Error(
            "Thiếu productionLotId."
        );
    }

    const queryParams =
        new URLSearchParams({
            productionLotId,
            page: String(page),
            size: String(size)
        });

    return apiRequest(
        `/farm-logs?${queryParams.toString()}`,
        {
            method: "GET"
        }
    );
}

/**
 * Upload attachment cho một nhật ký canh tác.
 *
 * POST /api/v1/farm-logs/{logId}/attachments
 *
 * Content-Type: multipart/form-data
 *
 * @param {string} logId
 * @param {FormData} formData
 * @returns {Promise<Object>}
 */
export async function uploadFarmLogAttachment(
    logId,
    formData
) {
    return apiRequest(
        `/farm-logs/${encodeURIComponent(logId)}/attachments`,
        {
            method: "POST",
            body: formData,
            headers: {}
        }
    );
}

/**
 * Lấy danh sách attachments của một nhật ký canh tác.
 *
 * GET /api/v1/farm-logs/{logId}/attachments
 *
 * @param {string} logId
 * @returns {Promise<Object>}
 */
export async function getFarmLogAttachments(
    logId
) {
    return apiRequest(
        `/farm-logs/${encodeURIComponent(logId)}/attachments`,
        {
            method: "GET"
        }
    );
}