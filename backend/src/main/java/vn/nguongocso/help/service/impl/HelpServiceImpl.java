package vn.nguongocso.help.service.impl;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
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
 * Triển khai dịch vụ quản lý nội dung hướng dẫn sử dụng trong ứng dụng.
*/
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HelpServiceImpl implements HelpService {
    private static final String GENERAL_ROLE_CODE = "GENERAL";

    private final HelpContentRepository helpContentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Lấy nội dung hướng dẫn cho một màn hình theo vai trò người dùng hiện tại.
     */
    @Override
    public HelpContentResponse getHelp(String screenKey) {
        if (screenKey == null || screenKey.isBlank()) {
            return null;
        }

        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        String roleCode = currentUser.getRoleCode();

        List<HelpContent> roleSpecificContents = helpContentRepository
                .findByScreenKeyAndRoleCodeOrderBySortOrderAsc(screenKey, roleCode);
        if (!roleSpecificContents.isEmpty()) {
            return toResponse(roleSpecificContents.get(0));
        }

        List<HelpContent> generalContents = helpContentRepository
                .findByScreenKeyAndRoleCodeOrderBySortOrderAsc(screenKey, GENERAL_ROLE_CODE);
        if (!generalContents.isEmpty()) {
            return toResponse(generalContents.get(0));
        }

        return null;
    }

    /**
     * Chuyển đổi entity sang response DTO.
     */
    private HelpContentResponse toResponse(HelpContent entity) {
        List<String> steps = parseSteps(entity.getScreenKey(), entity.getSteps());

        return HelpContentResponse.builder()
                .screenKey(entity.getScreenKey())
                .roleCode(entity.getRoleCode())
                .title(entity.getTitle())
                .steps(steps)
                .exampleData(entity.getExampleData())
                .build();
    }

    /**
     * Phân tích chuỗi JSON các bước hướng dẫn sang danh sách.
     */
    private List<String> parseSteps(String screenKey, String stepsJson) {
        if (stepsJson == null || stepsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(stepsJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("Không parse được steps JSON của help content, screenKey={}, stepsJson={}",
                    screenKey, stepsJson, e);
            return List.of();
        }
    }
}