import {
    apiRequest
} from "../core/api-client.js";

/**
 * Kích hoạt tem QR cho một lô sản xuất.
 *
 * POST /api/v1/trace-codes/activate
 *
 * @param {Object} activationData
 * @param {string} activationData.productionLotId
 * @param {string} activationData.codeRangeId
 * @param {number} activationData.quantity
 * @returns {Promise<Object>}
 */
export async function activateTraceCodes(
    activationData
) {
    return apiRequest(
        "/trace-codes/activate",
        {
            method: "POST",
            body: JSON.stringify(
                activationData
            )
        }
    );
}
