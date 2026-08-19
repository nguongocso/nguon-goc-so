package vn.nguongocso.help.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.help.dto.response.HelpContentResponse;
import vn.nguongocso.help.service.HelpService;

/**
 * Controller cung cấp nội dung hướng dẫn sử dụng trong ứng dụng (NCL-01-CN-006).
 *
 * <p>
 * Endpoint chỉ hoạt động với người dùng đã xác thực (có ACCESS JWT hợp lệ —
 * được đảm bảo bởi {@code JwtAuthenticationFilter} trong {@code SecurityConfig}).
 * Vai trò được suy ra từ token, do đó người dùng chỉ nhận được nội dung
 * hướng dẫn của chính vai trò mình (hoặc nội dung chung {@code GENERAL}).
 * </p>
 */
@RestController
@RequestMapping("/api/v1/help")
@RequiredArgsConstructor
@Validated
public class HelpController {

    private final HelpService helpService;

    /**
     * Lấy nội dung hướng dẫn cho một màn hình theo vai trò người dùng hiện tại.
     *
     * <p>
     * GET /api/v1/help?screenKey=farm-log-create
     * </p>
     *
     * @param screenKey mã định danh màn hình
     * @return nội dung hướng dẫn hoặc {@code data = null} nếu chưa có
     */
    @GetMapping
    public ResponseEntity<ApiResult<HelpContentResponse>> getHelp(
            @RequestParam("screenKey") String screenKey) {

        HelpContentResponse response = helpService.getHelp(screenKey);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }
}