/**
 * INTEGRATION CHECKLIST - Login Anomaly Detection (NCL-01-CN-005)
 * 
 * Files created:
 * ✅ Enums:
 *    - LoginResult.java
 *    - AnomalyReasonCode.java  
 *    - AnomalyStatus.java
 *    - AccountLockStatus.java
 *    - Updated NotificationType with LOGIN_ANOMALY_DETECTED, ACCOUNT_LOCKED, ACCOUNT_UNLOCKED
 *    - Updated UserStatus with LOCKED value (now ACTIVE, INACTIVE, LOCKED)
 * 
 * ✅ Entities:
 *    - LoginAttempt.java
 *    - LoginAnomaly.java
 *    - AccountLock.java
 * 
 * ✅ Repositories:
 *    - LoginAttemptRepository.java
 *    - LoginAnomalyRepository.java
 *    - AccountLockRepository.java
 * 
 * ✅ DTOs:
 *    - LockAccountRequest.java
 *    - LoginHistoryResponse.java
 *    - LoginAnomalyResponse.java
 *    - AccountLockResponse.java
 * 
 * ✅ Services:
 *    - LoginAnomalyDetectionService (interface)
 *    - LoginAnomalyDetectionServiceImpl
 *    - AccountLockService (interface)
 *    - AccountLockServiceImpl
 *    - LoginMonitoringService (interface)
 *    - LoginMonitoringServiceImpl
 * 
 * ✅ Controller:
 *    - LoginMonitoringController
 * 
 * ✅ Database:
 *    - V27__add_login_anomaly_tables.sql
 * 
 * ========================================================================
 * REMAINING TASKS
 * ========================================================================
 * 
 * 1. Integrate LoginAnomalyDetectionService into AuthService
 *    Location: backend/src/main/java/vn/nguongocso/auth/service/AuthService.java
 *    
 *    a) Add LoginAnomalyDetectionService dependency:
 *       private final LoginAnomalyDetectionService loginAnomalyDetectionService;
 *    
 *    b) In login() method:
 *       - After passwordEncoder.matches(), if password matches (SUCCESS):
 *         loginAnomalyDetectionService.recordLoginAttempt(
 *           user, 
 *           request.getUsername(), 
 *           true,  // isSuccess
 *           getClientIpAddress(request),  // Need to add parameter HttpServletRequest
 *           getCountryCodeFromIp(ipAddress)  // Placeholder for GeoIP
 *         );
 *       
 *       - If username not found or password doesn't match (FAILED):
 *         loginAnomalyDetectionService.recordLoginAttempt(
 *           user,  // Can be null if username not found
 *           request.getUsername(),
 *           false,  // isSuccess
 *           getClientIpAddress(request),
 *           getCountryCodeFromIp(ipAddress)
 *         );
 *    
 *    c) Before allowing login, check if account is locked:
 *       if (user.getStatus() == UserStatus.LOCKED) {
 *           throw new BusinessException("Tài khoản đã bị khóa tạm");
 *       }
 * 
 * 2. Add helper methods to AuthService:
 *    - getClientIpAddress(HttpServletRequest request): String
 *      Return X-Forwarded-For or remote address
 *    
 *    - getCountryCodeFromIp(String ipAddress): String
 *      Placeholder implementation (return null or "VN")
 *      Later integrate with GeoIP service
 * 
 * 3. Update AuthController.login() signature to accept HttpServletRequest
 *    @PostMapping("/login")
 *    public ResponseEntity<ApiResult<LoginResponse>> login(
 *        @Valid @RequestBody LoginRequest request,
 *        HttpServletRequest httpRequest
 *    )
 * 
 * 4. Add notification methods to NotificationService interface
 *    Location: backend/src/main/java/vn/nguongocso/notification/service/NotificationService.java
 *    
 *    Add these method signatures:
 *    
 *    /**
 *     * Gửi thông báo khi phát hiện đăng nhập bất thường.
 *     */
 *    void sendLoginAnomalyNotification(LoginAnomaly anomaly);
 *    
 *    /**
 *     * Gửi thông báo khi tài khoản bị khóa tạm.
 *     */
 *    void sendAccountLockedNotification(AccountLock accountLock);
 *    
 *    /**
 *     * Gửi thông báo khi tài khoản được mở khóa.
 *     */
 *    void sendAccountUnlockedNotification(AccountLock accountLock);
 * 
 * 5. Implement notification methods in NotificationServiceImpl
 *    Location: backend/src/main/java/vn/nguongocso/notification/service/impl/NotificationServiceImpl.java
 *    
 *    Implementation pattern (see sendLotRecallNotification):
 *    - Get list of recipient users using getNotificationRecipients(organizationId)
 *    - Filter by permission notification:READ
 *    - Create Notification records with appropriate type and message
 *    - Save all notifications
 * 
 * 6. Implement complete filters in LoginMonitoringServiceImpl
 *    Location: backend/src/main/java/vn/nguongocso/auth/service/impl/LoginMonitoringServiceImpl.java
 *    
 *    a) getLoginHistory():
 *       - Get current user
 *       - If VT-01 (Admin): can see all, organizationId optional
 *       - If VT-02 (Org Manager): force organizationId from token
 *       - Others: 403 Forbidden
 *       - Apply filters: userId, result, dateRange
 *       - Order by createdAt DESC
 *    
 *    b) getLoginAnomalies():
 *       - Get current user
 *       - If VT-01 (Admin): can see all, organizationId optional
 *       - If VT-02 (Org Manager): force organizationId from token
 *       - Others: 403 Forbidden
 *       - Apply filters: status, reasonCode
 *       - Order by detectedAt DESC
 *    
 *    c) lockAccount():
 *       - Get current user
 *       - Check permission: VT-01 or VT-02
 *       - If VT-02: verify target account belongs to their org
 *       - Call accountLockService.lockAccount()
 *       - Send notification (via AccountLockServiceImpl)
 *    
 *    d) unlockAccount():
 *       - Similar permission checks as lockAccount
 *       - Call accountLockService.unlockAccount()
 *       - Send notification
 * 
 * 7. Add notification calls to AccountLockServiceImpl
 *    Location: backend/src/main/java/vn/nguongocso/auth/service/impl/AccountLockServiceImpl.java
 *    
 *    After successful lock:
 *    List<UUID> recipientIds = List.of(account.getUserId());
 *    notificationService.sendAccountLockedNotification(accountLock);
 *    
 *    After successful unlock:
 *    notificationService.sendAccountUnlockedNotification(accountLock);
 * 
 * 8. Add GeoIP integration (Optional enhancement)
 *    - Integrate MaxMind GeoIP2 or similar service
 *    - Extract country code from IP in AuthService
 *    - Add as new service or utility class
 * 
 * 9. Testing
 *    - Unit tests for LoginAnomalyDetectionServiceImpl
 *    - Unit tests for AccountLockServiceImpl
 *    - Integration tests for login flow
 *    - Integration tests for anomaly detection
 *    - Integration tests for lock/unlock API
 * 
 * ========================================================================
 * KEY DESIGN NOTES
 * ========================================================================
 * 
 * - One-way dependency: auth -> notification (no circular dependency)
 * - Permission checks via PermissionChecker service
 * - Timestamps use OffsetDateTime for timezone support
 * - Organization lookup via OrganizationUserRepository.findFirstByUser()
 * - Notifications use existing NotificationService pattern
 * - Token invalidation placeholder in AccountLockServiceImpl.invalidateAllTokens()
 * 
 * ========================================================================
 */
