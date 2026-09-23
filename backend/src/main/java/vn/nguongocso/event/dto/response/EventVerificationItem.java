package vn.nguongocso.event.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Item kết quả kiểm chứng của từng sự kiện trong chuỗi.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
public class EventVerificationItem {
    private Integer index;
    private UUID eventId;
    private String eventType;
    private LocalDateTime recordedAt;
    private String hash;
    private String previousHash;
    private Boolean isValid;
    private String expectedHash; // chỉ có khi isValid = false
}