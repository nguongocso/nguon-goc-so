import {
    apiRequest
} from "../core/api-client.js";

/**
 * Kích hoạt toàn bộ mã QR của một lô hàng.
 *
 * POST /api/v1/shipments/{shipmentId}/activate
 *
 * @param {string} shipmentId
 * @returns {Promise<Object>}
 */
export async function activateShipmentTraceCodes(
    shipmentId
) {
    const normalizedShipmentId =
        String(shipmentId || "").trim();

    if (!normalizedShipmentId) {
        throw new Error(
            "Mã lô hàng không hợp lệ."
        );
    }

    return apiRequest(
        `/shipments/${
            encodeURIComponent(
                normalizedShipmentId
            )
        }/activate`,
        {
            method: "POST"
        }
    );
}

/**
 * Cấp dải mã truy xuất cho một tổ chức.
 *
 * POST /api/v1/admin/code-ranges
 *
 * @param {{
 *   organizationId: string,
 *   prefix: string,
 *   totalLimit: number
 * }} codeRangeData
 * @returns {Promise<Object>}
 */
export async function createCodeRange(
    codeRangeData
) {
    return apiRequest(
        "/admin/code-ranges",
        {
            method: "POST",
            body: JSON.stringify(
                codeRangeData
            )
        }
    );
}