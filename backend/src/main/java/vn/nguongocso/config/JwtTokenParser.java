package vn.nguongocso.config;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Thành phần phân tích cú pháp, xác thực chữ ký và trích xuất claims từ mã JWT.
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenParser {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ORG_ID = "orgId";
    public static final String CLAIM_ORG_NAME = "orgName";
    public static final String CLAIM_ORG_CODE = "orgCode";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_FULL_NAME = "fullName";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    private final JwtProperties jwtProperties;

    /**
     * Tạo khóa bí mật HMAC từ cấu hình.
     *
     * @return khóa HMAC dùng cho ký và kiểm tra JWT
     */
    public SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Giải mã và xác thực chữ ký token.
     *
     * @param token chuỗi JWT
     * @return payload chứa các claims
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Kiểm tra tính hợp lệ của token (chữ ký đúng định dạng, chưa hết hạn).
     *
     * @param token chuỗi JWT
     * @return true nếu hợp lệ, ngược lại false
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy loại token (ORG_SELECTION hoặc ACCESS).
     */
    public String getTokenType(String token) {
        return parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    /**
     * Lấy username từ subject của token.
     */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Lấy ID người dùng từ claim userId (phục hồi hành vi nguyên thủy, throw nếu null).
     */
    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_USER_ID, String.class));
    }

    /**
     * Lấy ID tổ chức từ claim orgId (phục hồi hành vi nguyên thủy, throw nếu null).
     */
    public UUID getOrganizationId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_ORG_ID, String.class));
    }

    /**
     * Lấy mã tổ chức từ claim orgCode.
     */
    public String getOrganizationCode(String token) {
        return parseClaims(token).get(CLAIM_ORG_CODE, String.class);
    }

    /**
     * Lấy mã vai trò từ claim role.
     */
    public String getRoleCode(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }
}
