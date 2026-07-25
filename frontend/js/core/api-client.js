import {
    getToken,
    clearAuth
} from "./storage.js";

const API_BASE_URL =
    "http://localhost:8080/api/v1";

export async function apiRequest(
    endpoint,
    options = {}
) {
    const token = getToken();

    // For FormData (file uploads), do NOT set Content-Type;
    // the browser will set it to multipart/form-data with boundary.
    const isFormData = options.body instanceof FormData;

    const headers = {
        ...(!isFormData && {
            "Content-Type": "application/json"
        }),
        ...options.headers
    };

    if (token) {
        headers.Authorization =
            `Bearer ${token}`;
    }

    let response;

    try {
        response = await fetch(
            `${API_BASE_URL}${endpoint}`,
            {
                ...options,
                headers
            }
        );
    } catch (error) {
        throw new Error(
            "Không thể kết nối đến máy chủ."
        );
    }

    if (response.status === 401) {
        clearAuth();
        window.location.href =
            "/frontend/pages/auth/login.html";


        throw new Error(
            "Phiên đăng nhập đã hết hạn."
        );
    }

    const responseText =
        await response.text();

    let data = null;

    if (responseText) {
        try {
            data = JSON.parse(
                responseText
            );
        } catch (error) {
            console.error(
                "Invalid JSON response:",
                responseText
            );

            if (!response.ok) {
                throw new Error(
                    `Lỗi máy chủ: ${response.status}`
                );
            }

            return null;
        }
    }

    if (!response.ok) {
        const message =
            data && data.message
                ? data.message
                : `Yêu cầu thất bại: ${response.status}`;

        console.error(
            `API Error [${response.status}]:`,
            message
        );

        throw new Error(message);
    }

    return data;
}
