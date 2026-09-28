package vn.nguongocso.config;

import java.util.Date;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;

/** Thành phần khởi tạo và ký mã xác thực JWT. */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenSigner {
    public static final String TOKEN_TYPE_SELECTION = "ORG_SELECTION";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";

    private final JwtTokenParser jwtTokenParser;
    private final JwtProperties jwtProperties;

    /** Sinh mã JWT ngắn hạn phục vụ bước chọn tổ chức (thời hạn 5 phút). */
    public String generateSelectionToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + getSelectionTokenExpirationInSeconds() * 1000L);

        return Jwts.builder()
                .subject(user.getUserName())
                .claim(JwtTokenParser.CLAIM_USER_ID, user.getUserId().toString())
                .claim(JwtTokenParser.CLAIM_FULL_NAME, user.getFullName())
                .claim(JwtTokenParser.CLAIM_TOKEN_TYPE, TOKEN_TYPE_SELECTION)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtTokenParser.getKey())
                .compact();
    }

    /** Sinh mã JWT truy cập đầy đủ chứa ngữ cảnh tổ chức và quyền hạn. */
    public String generateAccessToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(JwtTokenParser.CLAIM_USER_ID, userDetails.getUserId().toString())
                .claim(JwtTokenParser.CLAIM_ORG_ID, userDetails.getOrganizationId().toString())
                .claim(JwtTokenParser.CLAIM_ORG_NAME, userDetails.getOrganizationName())
                .claim(JwtTokenParser.CLAIM_ORG_CODE, userDetails.getOrganizationCode())
                .claim(JwtTokenParser.CLAIM_ROLE, userDetails.getRoleCode())
                .claim(JwtTokenParser.CLAIM_FULL_NAME, userDetails.getFullName())
                .claim(JwtTokenParser.CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtTokenParser.getKey())
                .compact();
    }

    /** Thời gian hết hạn của mã ACCESS tính theo giây. */
    public long getExpirationInSeconds() {
        return jwtProperties.getExpiration() / 1000;
    }

    /** Thời gian hết hạn của mã ORG_SELECTION tính theo giây (5 phút). */
    public long getSelectionTokenExpirationInSeconds() {
        return 5 * 60L;
    }
}
