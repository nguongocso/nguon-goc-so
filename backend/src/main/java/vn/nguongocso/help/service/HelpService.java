package vn.nguongocso.help.service;

import vn.nguongocso.help.dto.response.HelpContentResponse;

/**
 * Dịch vụ quản lý nội dung hướng dẫn sử dụng trong ứng dụng.
 */
public interface HelpService {
    /*
     * Lấy nội dung hướng dẫn theo màn hình.
     */
    HelpContentResponse getHelp(String screenKey);
}