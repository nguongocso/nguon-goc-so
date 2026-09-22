package vn.nguongocso.trace.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Phản hồi tải lên chứng từ giao hàng. */
@Getter
@Setter
@Builder
public class HandoverAttachmentUploadResponse {

    private String filePath;
}