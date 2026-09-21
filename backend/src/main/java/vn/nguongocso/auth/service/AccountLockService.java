package vn.nguongocso.auth.service;

import java.util.UUID;

import vn.nguongocso.auth.entity.User;

/**
 * Service quản lý khoá/mở khoá tạm tài khoản.
 */
public interface AccountLockService {
    /**
     * Khoá tạm một tài khoản.
     */
    UUID lockAccount(
            UUID accountId,
            UUID anomalyId,
            String reason,
            User lockedBy);

    /*
     * Khoá tạm một tài khoản theo thời gian
     */
    UUID lockAccount(
            UUID accountId,
            UUID anomalyId,
            String reason,
            User lockedBy,
            Integer days,
            Integer hours,
            Integer minutes,
            boolean permanent);

    /**
     * Mở khóa một tài khoản.
     */
    UUID unlockAccount(UUID accountId, User unlockedBy);

    /**
     * Vô hiệu hoá tất cả access token của một tài khoản.
     */
    void invalidateAllTokens(UUID userId);
}
