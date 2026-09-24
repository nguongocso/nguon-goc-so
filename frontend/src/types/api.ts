import type { ApiResult } from "./auth";

/**
 * Bí danh chuẩn cho phản hồi API backend.
 * Backend trả về ApiResult với các trường success/status/message/data.
 * Dùng `Promise<ApiResponse<T>>` cho mọi service để thống nhất kiểm tra checklist.
 */
export type ApiResponse<T> = ApiResult<T>;
