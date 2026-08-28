package vn.nguongocso.auth.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ứng viên thay thế hợp lệ cho thành viên sắp bị vô hiệu hóa
 * (API GET /members/{userId}/replacement-candidates).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplacementCandidateResponse {

    private UUID userId;

    private String username;

    private String fullName;

    private String roleCode;

    private String roleName;

    /** Các lô chưa hoàn thành mà ứng viên đủ điều kiện tiếp nhận. */
    private List<UUID> eligibleLotIds;
}
