package vn.nguongocso.config;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;

/**
 * Lớp điều phối cung cấp các tiện ích xử lý mã JWT cho hệ thống.
 * Ủy quyền ký mã cho {@link JwtTokenSigner} và phân tích cú pháp cho {@link JwtTokenParser}.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    public static final String TOKEN_TYPE_SELECTION = JwtTokenSigner.TOKEN_TYPE_SELECTION;
    public static final String TOKEN_TYPE_ACCESS = JwtTokenSigner.TOKEN_TYPE_ACCESS;

    private final JwtTokenParser jwtTokenParser;
    private final JwtTokenSigner jwtTokenSigner;

    /** Sinh mã JWT ngắn hạn phục vụ bước chọn tổ chức. */
    public String generateSelectionToken(User user) {
        return jwtTokenSigner.generateSelectionToken(user);
    }

    /** Sinh mã JWT truy cập đầy đủ chứa ngữ cảnh tổ chức và quyền hạn. */
    public String generateAccessToken(CustomUserDetails userDetails) {
        return jwtTokenSigner.generateAccessToken(userDetails);
    }

    /** Kiểm tra tính hợp lệ của mã JWT. */
    public boolean validateToken(String token) {
        return jwtTokenParser.validateToken(token);
    }

    /** Lấy loại mã JWT (ORG_SELECTION hoặc ACCESS). */
    public String getTokenTypeFromToken(String token) {
        return jwtTokenParser.getTokenType(token);
    }

    /** Lấy định danh người dùng từ mã JWT. */
    public UUID getUserIdFromToken(String token) {
        return jwtTokenParser.getUserId(token);
    }

    /** Lấy định danh tổ chức từ mã JWT. */
    public UUID getOrganizationIdFromToken(String token) {
        return jwtTokenParser.getOrganizationId(token);
    }

    /** Lấy thời hạn của mã ACCESS tính theo giây. */
    public long getExpirationInSeconds() {
        return jwtTokenSigner.getExpirationInSeconds();
    }

    /** Lấy thời hạn của mã ORG_SELECTION tính theo giây. */
    public long getSelectionTokenExpirationInSeconds() {
        return jwtTokenSigner.getSelectionTokenExpirationInSeconds();
    }
}
