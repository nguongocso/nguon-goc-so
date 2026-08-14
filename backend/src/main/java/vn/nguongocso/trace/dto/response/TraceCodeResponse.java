package vn.nguongocso.trace.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.trace.enums.TraceCodeStatus;

/**
 * Thông tin mã truy xuất.
 */
@Data
@Builder
public class TraceCodeResponse {
	private UUID id;

	private String codeValue;

	private String qrImage;

	private TraceCodeStatus status;
}