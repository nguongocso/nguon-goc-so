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