package vn.nguongocso.config;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;

/**
 * Lớp Facade cung cấp các phương thức tiện ích xử lý JWT cho hệ thống.
 * <p>
 * Ủy quyền việc sinh và ký token cho {@link JwtTokenSigner},
 * và việc phân tích cú pháp, xác thực token cho {@link JwtTokenParser}.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String TOKEN_TYPE_SELECTION = JwtTokenSigner.TOKEN_TYPE_SELECTION;
    public static final String TOKEN_TYPE_ACCESS = JwtTokenSigner.TOKEN_TYPE_ACCESS;

    private final JwtTokenParser jwtTokenParser;
    private final JwtTokenSigner jwtTokenSigner;

    /**
     * Sinh JWT ngắn hạn phục vụ bước chọn tổ chức (ORG_SELECTION).
     *
     * @param user người dùng đã xác thực thông tin đăng nhập
     * @return chuỗi JWT đã ký
     */
    public String generateSelectionToken(User user) {
        return jwtTokenSigner.generateSelectionToken(user);
    }

    /**
     * Sinh JWT truy cập đầy đủ (ACCESS) chứa ngữ cảnh tổ chức và vai trò.
     *
     * @param userDetails thông tin chi tiết người dùng
     * @return chuỗi JWT đã ký
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        return jwtTokenSigner.generateAccessToken(userDetails);
    }

    /**
     * Kiểm tra tính hợp lệ của token (chữ ký đúng định dạng, chưa hết hạn).
     *
     * @param token chuỗi JWT
     * @return true nếu token hợp lệ, ngược lại false
     */
    public boolean validateToken(String token) {
        return jwtTokenParser.validateToken(token);
    }

    /**
     * Lấy loại token (ORG_SELECTION hoặc ACCESS).
     *
     * @param token chuỗi JWT
     * @return loại token
     */
    public String getTokenTypeFromToken(String token) {
        return jwtTokenParser.getTokenType(token);
    }

    /**
     * Lấy định danh người dùng từ token.
     *
     * @param token chuỗi JWT
     * @return UUID người dùng
     */
    public UUID getUserIdFromToken(String token) {
        return jwtTokenParser.getUserId(token);
    }

    /**
     * Lấy định danh tổ chức từ token.
     *
     * @param token chuỗi JWT
     * @return UUID tổ chức
     */
    public UUID getOrganizationIdFromToken(String token) {
        return jwtTokenParser.getOrganizationId(token);
    }

    /**
     * Lấy thời hạn của access token (tính bằng giây).
     *
     * @return thời hạn token theo giây
     */
    public long getExpirationInSeconds() {
        return jwtTokenSigner.getExpirationInSeconds();
    }

    /**
     * Lấy thời hạn của selection token (tính bằng giây).
     *
     * @return thời hạn token theo giây
     */
    public long getSelectionTokenExpirationInSeconds() {
        return jwtTokenSigner.getSelectionTokenExpirationInSeconds();
    }
}