package vn.nguongocso.common;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/** Kết quả chuẩn trả về từ API. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {
    private boolean success;

    private int status;

    private String message;

    private T data;

    private Object errors;

    private String path;

    @Builder.Default
    private Instant timestamp = Instant.now();

    /** Tạo ApiResult thành công với dữ liệu trả về mặc định mã 200. */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .success(true)
                .status(200)
                .data(data)
                .build();
    }

    /** Tạo ApiResult thành công với mã trạng thái HTTP và dữ liệu trả về. */
    public static <T> ApiResult<T> success(int status, T data) {
        return ApiResult.<T>builder()
                .success(true)
                .status(status)
                .data(data)
                .build();
    }

    /** Tạo ApiResult lỗi với mã trạng thái và thông điệp lỗi. */
    public static <T> ApiResult<T> error(int status, String message) {
        return ApiResult.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .build();
    }

    /** Tạo ApiResult lỗi với mã trạng thái, thông điệp, chi tiết lỗi và đường dẫn. */
    public static <T> ApiResult<T> error(int status, String message, Object errors, String path) {
        return ApiResult.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .errors(errors)
                .path(path)
                .build();
    }

    /** Tạo ApiResult lỗi với mã trạng thái, thông điệp và đường dẫn. */
    public static <T> ApiResult<T> error(int status, String message, String path) {
        return ApiResult.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .path(path)
                .build();
    }

    /** Tạo ApiResult lỗi với mã trạng thái, thông điệp và chi tiết lỗi. */
    public static <T> ApiResult<T> error(int status, String message, Object errors) {
        return ApiResult.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .errors(errors)
                .build();
    }
}
