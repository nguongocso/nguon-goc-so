import {
    apiRequest
} from "../core/api-client.js";

export async function getCodeRangeStatus() {
    return apiRequest(
        "/admin/code-ranges/status",
        {
            method: "GET"
        }
    );
}

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

/**
 * Fetch code ranges belonging to the currently authenticated user's organization.
 *
 * GET /api/v1/organization/code-ranges
 *
 * The endpoint resolves the organization from the JWT security context.
 * No organization ID is sent from the frontend.
 *
 * @returns {Promise<Object>} API response with data containing CodeRangeResponse[]
 */
export async function getOrganizationCodeRanges() {
    return apiRequest(
        "/organization/code-ranges",
        {
            method: "GET"
        }
    );
}