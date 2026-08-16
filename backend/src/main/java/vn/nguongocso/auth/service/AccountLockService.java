package vn.nguongocso.auth.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.entity.User;

/**
 * Service quản lý khoá/mở khoá tạm tài khoản.
 */
public interface AccountLockService {
    
    /**
     * Khoá tạm một tài khoản.
     * 
     * <p>
     * Yêu cầu:
     * - Tài khoản phải tồn tại và đang ở trạng thái ACTIVE
     * - Người thực hiện khoá phải có quyền (VT-01 hoặc VT-02 trong tổ chức)
     * - Ngay khi khoá, mọi access token hiện có của tài khoản bị vô hiệu hoá
     * </p>
     * 
     * @param accountId ID tài khoản cần khoá
     * @param anomalyId ID bản ghi bất thường (nullable)
     * @param reason    lý do khoá tạm (tối đa 500 ký tự)
     * @param lockedBy  người thực hiện khoá
     * @return ID tài khoản sau khi khoá thành công
     */
    UUID lockAccount(
        UUID accountId,
        UUID anomalyId,
        String reason,
        User lockedBy
    );
    
    /**
     * Mở khóa một tài khoản.
     * 
     * <p>
     * Yêu cầu:
     * - Tài khoản phải đang ở trạng thái LOCKED
     * - Người thực hiện mở khóa phải có quyền (VT-01 hoặc VT-02 trong tổ chức)
     * </p>
     * 
     * @param accountId ID tài khoản cần mở khóa
     * @param unlockedBy người thực hiện mở khóa
     * @return ID tài khoản sau khi mở khóa thành công
     */
    UUID unlockAccount(UUID accountId, User unlockedBy);
    
    /**
     * Vô hiệu hoá tất cả access token của một tài khoản.
     * 
     * @param userId ID tài khoản
     */
    void invalidateAllTokens(UUID userId);
}
