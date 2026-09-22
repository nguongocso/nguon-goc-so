package vn.nguongocso.help.service;

import vn.nguongocso.help.dto.response.HelpContentResponse;

/**
 * Dịch vụ quản lý nội dung hướng dẫn sử dụng trong ứng dụng.
*/
public interface HelpService {
    HelpContentResponse getHelp(String screenKey);
}