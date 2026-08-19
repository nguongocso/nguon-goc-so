package vn.nguongocso.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;

/**
 * Lớp tiện ích cho các thao tác liên quan đến bảo mật và xác thực người dùng.
 */
public class SecurityUtils {
    /**
     * Lấy thông tin chi tiết của người dùng hiện tại từ ngữ cảnh bảo mật.
     *
     * @return Thông tin chi tiết của người dùng hiện tại.
     * @throws BusinessException nếu người dùng chưa đăng nhập hoặc xác thực không hợp lệ.
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Chưa đăng nhập");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            throw new BusinessException("Lỗi xác thực");
        }
        return (CustomUserDetails) principal;
    }
    
}
