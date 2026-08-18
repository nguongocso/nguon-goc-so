package vn.nguongocso.help.service;

import vn.nguongocso.help.dto.response.HelpContentResponse;

/**
 * Dịch vụ quản lý nội dung hướng dẫn sử dụng trong ứng dụng (NCL-01-CN-006).
 */
public interface HelpService {

    /**
     * Lấy nội dung hướng dẫn cho một màn hình theo vai trò người dùng hiện tại.
     *
     * <p>
     * Mức ưu tiên:
     * <ol>
     *   <li>Nội dung khớp {@code screenKey} + {@code roleCode}.</li>
     *   <li>Nội dung chung ({@code roleCode = "GENERAL"}) của {@code screenKey}.</li>
     *   <li>{@code null} nếu không có nội dung nào (frontend hiển thị thông báo mặc định).</li>
     * </ol>
     * </p>
     *
     * @param screenKey mã định danh màn hình
     * @return nội dung hướng dẫn, hoặc {@code null} nếu không có
     */
    HelpContentResponse getHelp(String screenKey);
}