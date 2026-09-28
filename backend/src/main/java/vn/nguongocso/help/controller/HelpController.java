package vn.nguongocso.help.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.nguongocso.common.ApiResult;
import vn.nguongocso.help.dto.response.HelpContentResponse;
import vn.nguongocso.help.service.HelpService;

/**
 * Controller cung cấp nội dung hướng dẫn sử dụng trong ứng dụng.
*/
@RestController
@RequestMapping("/api/v1/help")
@RequiredArgsConstructor
@Validated
public class HelpController {
    private final HelpService helpService;

    /**
     * Lấy nội dung hướng dẫn cho một màn hình theo vai trò người dùng hiện tại.
     */
    @GetMapping
    public ResponseEntity<ApiResult<HelpContentResponse>> getHelp(
            @RequestParam("screenKey") String screenKey) {
        HelpContentResponse response = helpService.getHelp(screenKey);

        return ResponseEntity.ok(ApiResult.success(response));
    }
}