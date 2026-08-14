package vn.nguongocso.common;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/**
 * ApiResult là một lớp đại diện cho kết quả trả về từ API.
 * Nó chứa thông tin về trạng thái thành công, mã trạng thái HTTP,
 * thông điệp, dữ liệu trả về, lỗi (nếu có), đường dẫn và thời gian tạo.
 *
 * @param <T> Kiểu dữ liệu của dữ liệu trả về.
 */
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

	public static <T> ApiResult<T> success(T data) {
		return ApiResult.<T>builder()
				.success(true)
				.status(200)
				.data(data)
				.build();
	}

	/**
	 * Tạo một ApiResult thành công với mã trạng thái và dữ liệu trả về.
	 *
	 * @param status Mã trạng thái HTTP.
	 * @param data   Dữ liệu trả về.
	 * @param <T>    Kiểu dữ liệu của dữ liệu trả về.
	 * @return ApiResult thành công.
	 */
	public static <T> ApiResult<T> success(int status, T data) {
		return ApiResult.<T>builder()
				.success(true)
				.status(status)
				.data(data)
				.build();
	}

	/**
	 * Tạo một ApiResult lỗi với mã trạng thái và thông điệp lỗi.
	 *
	 * @param status  Mã trạng thái HTTP.
	 * @param message Thông điệp lỗi.
	 * @param <T>     Kiểu dữ liệu của dữ liệu trả về (thường là null).
	 * @return ApiResult lỗi.
	 */
	public static <T> ApiResult<T> error(int status, String message) {
		return ApiResult.<T>builder()
				.success(false)
				.status(status)
				.message(message)
				.build();
	}

	/**
	 * Tạo một ApiResult lỗi với mã trạng thái, thông điệp lỗi, chi tiết lỗi và
	 * đường dẫn.
	 * 
	 * @param <T>
	 * @param status
	 * @param message
	 * @param errors
	 * @param path
	 * @return
	 */
	public static <T> ApiResult<T> error(int status, String message, Object errors, String path) {
		return ApiResult.<T>builder()
				.success(false)
				.status(status)
				.message(message)
				.errors(errors)
				.path(path)
				.build();
	}

	/**
	 * Tạo một ApiResult lỗi với mã trạng thái, thông điệp lỗi và đường dẫn.
	 *
	 * @param status  Mã trạng thái HTTP.
	 * @param message Thông điệp lỗi.
	 * @param path    Đường dẫn của yêu cầu gây ra lỗi.
	 * @param <T>     Kiểu dữ liệu của dữ liệu trả về (thường là null).
	 * @return ApiResult lỗi.
	 */
	public static <T> ApiResult<T> error(int status, String message, String path) {
		return ApiResult.<T>builder()
				.success(false)
				.status(status)
				.message(message)
				.path(path)
				.build();
	}

	/**
	 * Tạo một ApiResult lỗi với mã trạng thái, thông điệp lỗi và chi tiết lỗi.
	 *
	 * @param status  Mã trạng thái HTTP.
	 * @param message Thông điệp lỗi.
	 * @param errors  Chi tiết lỗi (có thể là danh sách lỗi hoặc đối tượng lỗi).
	 * @param <T>     Kiểu dữ liệu của dữ liệu trả về (thường là null).
	 * @return ApiResult lỗi.
	 */
	public static <T> ApiResult<T> error(int status,
			String message,
			Object errors) {
		return ApiResult.<T>builder()
				.success(false)
				.status(status)
				.message(message)
				.errors(errors)
				.build();
	}
}
