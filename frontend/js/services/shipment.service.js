import {
    apiRequest
} from "../core/api-client.js";

/**
 * Create a new shipment from a PACKAGED
 * production lot and generate its
 * trace codes (QR).
 *
 * POST /api/v1/shipments
 *
 * @param {Object} shipmentData
 * @param {string} shipmentData.productionLotId
 * @param {string} shipmentData.name
 * @param {number} shipmentData.totalQuantity
 * @param {string} [shipmentData.packagingInfo]
 * @returns {Promise<Object>}
 */
export async function createShipment(
    shipmentData
) {
    return apiRequest(
        "/shipments",
        {
            method: "POST",
            body: JSON.stringify(
                shipmentData
            )
        }
    );
}

/**
 * Fetch all shipments for the
 * authenticated organization.
 *
 * GET /api/v1/shipments
 *
 * @returns {Promise<Object>}
 */
export async function getShipments() {
    return apiRequest(
        "/shipments",
        {
            method: "GET"
        }
    );
}