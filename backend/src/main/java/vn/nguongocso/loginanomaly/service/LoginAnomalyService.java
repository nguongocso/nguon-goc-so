package vn.nguongocso.loginanomaly.service;

import java.time.LocalDate;
import java.util.UUID;

import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.loginanomaly.dto.response.LockLoginAnomalyResponse;
import vn.nguongocso.loginanomaly.dto.response.LoginAnomalyResponse;
import vn.nguongocso.loginanomaly.enums.LoginAnomalySeverity;

/** Dịch vụ theo dõi đăng nhập bất thường và khóa tạm tài khoản. */
public interface LoginAnomalyService {

    /**
     * Lấy danh sách đăng nhập bất thường.
     *
     * <p>
     * Quản trị viên (VT-01) xem toàn nền tảng; quản lý hợp tác xã (VT-02)
     * chỉ thấy dữ liệu thuộc tổ chức của mình.
     * </p>
     */
    PageResponse<LoginAnomalyResponse> getAnomalies(
            LoginAnomalySeverity severity,
            UserStatus accountStatus,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size);

    /**
     * Khóa tạm tài khoản của một bản ghi bất thường.
     *
     * <p>
     * Hành động được ghi vào lịch sử hoạt động và chủ tài khoản
     * nhận được thông báo.
     * </p>
     */
    LockLoginAnomalyResponse lockAnomaly(UUID anomalyId);

    /**
     * Ghi nhận một lần đăng nhập thất bại và phát hiện bất thường
     * khi số lần thất bại trong cửa sổ 2 phút đạt ngưỡng 5.
     */
    void recordFailedLogin(String username, String ipAddress);
}
