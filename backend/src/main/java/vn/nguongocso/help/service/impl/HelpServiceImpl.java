package vn.nguongocso.help.service.impl;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.help.dto.response.HelpContentResponse;
import vn.nguongocso.help.entity.HelpContent;
import vn.nguongocso.help.repository.HelpContentRepository;
import vn.nguongocso.help.service.HelpService;

/**
 * Triển khai dịch vụ lấy nội dung hướng dẫn sử dụng (NCL-01-CN-006).
 *
 * <p>
 * Vai trò người dùng được lấy từ {@link SecurityUtils#getCurrentUserDetails()}
 * nên người dùng chỉ nhận được nội dung hướng dẫn của chính vai trò mình
 * (hoặc nội dung chung {@code GENERAL}).
 * </p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HelpServiceImpl implements HelpService {

    /** Mã vai trò dùng chung cho nội dung hướng dẫn áp dụng với mọi vai trò. */
    private static final String GENERAL_ROLE_CODE = "GENERAL";

    private final HelpContentRepository helpContentRepository;
    private final ObjectMapper objectMapper;

    @Override
    public HelpContentResponse getHelp(String screenKey) {
        if (screenKey == null || screenKey.isBlank()) {
            return null;
        }

        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        String roleCode = currentUser.getRoleCode();

        // 1. Nội dung khớp đúng screenKey + roleCode
        List<HelpContent> roleSpecific = helpContentRepository
                .findByScreenKeyAndRoleCodeOrderBySortOrderAsc(screenKey, roleCode);
        if (!roleSpecific.isEmpty()) {
            return toResponse(roleSpecific.get(0));
        }

        // 2. Nội dung chung (GENERAL) cho màn hình
        List<HelpContent> general = helpContentRepository
                .findByScreenKeyAndRoleCodeOrderBySortOrderAsc(screenKey, GENERAL_ROLE_CODE);
        if (!general.isEmpty()) {
            return toResponse(general.get(0));
        }

        // 3. Không có nội dung -> null (frontend hiển thị thông báo mặc định)
        return null;
    }

    /**
     * Chuyển đổi entity sang response DTO, parse {@code steps} JSON array sang
     * {@code List<String>}.
     */
    private HelpContentResponse toResponse(HelpContent entity) {
        List<String> steps = parseSteps(entity.getSteps());

        return HelpContentResponse.builder()
                .screenKey(entity.getScreenKey())
                .roleCode(entity.getRoleCode())
                .title(entity.getTitle())
                .steps(steps)
                .exampleData(entity.getExampleData())
                .build();
    }

    private List<String> parseSteps(String stepsJson) {
        if (stepsJson == null || stepsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(stepsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Không parse được steps JSON của help content: {}", e.getMessage());
            return List.of();
        }
    }
}