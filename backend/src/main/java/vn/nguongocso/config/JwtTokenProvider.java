package vn.nguongocso.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Provides utility methods for generating, parsing and validating JWT tokens.
 *
 * <p>
 * The application uses two types of JWT:
 *
 * <ul>
 * <li>
 * {@code ORG_SELECTION}: short-lived token issued after successful
 * username/password authentication. This token is used only for
 * selecting an organization.
 * </li>
 *
 * <li>
 * {@code ACCESS}: full access token issued after the user has selected
 * an organization. This token contains organization and role context
 * used for authorization.
 * </li>
 * </ul>
 *
 * <p>
 * The generated access token contains authentication and
 * organization-related information that will be used for authorization
 * in subsequent requests.
 */
@Component
public class JwtTokenProvider {

    /*
     * ============================================================
     * JWT CLAIMS
     * ============================================================
     */

    private static final String CLAIM_USER_ID = "userId";

    private static final String CLAIM_ORG_ID = "orgId";

    private static final String CLAIM_ORG_NAME = "orgName";

    private static final String CLAIM_ORG_CODE = "orgCode";

    private static final String CLAIM_ROLE = "role";

    private static final String CLAIM_FULL_NAME = "fullName";

    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    /*
     * ============================================================
     * TOKEN TYPES
     * ============================================================
     */

    /**
     * Short-lived token used between login and organization selection.
     */
    public static final String TOKEN_TYPE_SELECTION = "ORG_SELECTION";

    /**
     * Full access token used to access protected APIs.
     */
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";

    /*
     * ============================================================
     * CONFIGURATION
     * ============================================================
     */

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    /*
     * ============================================================
     * SECRET KEY
     * ============================================================
     */

    /**
     * Creates the secret key used to sign and verify JWT tokens.
     *
     * @return HMAC secret key derived from the configured application secret
     */
    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /*
     * ============================================================
     * ACCESS TOKEN
     * ============================================================
     */

    /**
     * Generates a signed access JWT for the authenticated user.
     *
     * <p>
     * The access token contains:
     *
     * <ul>
     * <li>User identity</li>
     * <li>Organization identity</li>
     * <li>Organization code</li>
     * <li>Organization name</li>
     * <li>User role</li>
     * <li>Full name</li>
     * <li>Token type</li>
     * </ul>
     *
     * @param authentication authenticated user with organization context
     * @return signed access JWT string
     */
    public String generateToken(Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return generateAccessToken(userDetails);
    }

    /*
     * ============================================================
     * SELECTION TOKEN
     * ============================================================
     */

    /**
     * Generates a short-lived JWT token for organization selection.
     *
     * <p>
     * This token is issued after the user successfully authenticates
     * with username and password.
     *
     * <p>
     * At this stage the organization has not been selected yet,
     * therefore the token does not contain organization or role data.
     *
     * <p>
     * The token expires after 5 minutes.
     *
     * @param user authenticated user
     * @return signed organization-selection JWT
     */
    public String generateSelectionToken(User user) {

        Date now = new Date();

        /*
         * Selection token lifetime:
         * 5 minutes
         */
        Date expiryDate = new Date(now.getTime() + 5 * 60 * 1000L);

        return Jwts.builder()

                /*
                 * Subject
                 */
                .subject(user.getUserName())

                /*
                 * User identity
                 */
                .claim(
                        CLAIM_USER_ID,
                        user.getUserId().toString())

                /*
                 * User information
                 */
                .claim(
                        CLAIM_FULL_NAME,
                        user.getFullName())

                /*
                 * Token type
                 */
                .claim(
                        CLAIM_TOKEN_TYPE,
                        TOKEN_TYPE_SELECTION)

                /*
                 * Token timestamps
                 */
                .issuedAt(now)
                .expiration(expiryDate)

                /*
                 * Signature
                 */
                .signWith(getKey())

                .compact();
    }

    /*
     * ============================================================
     * PARSE TOKEN
     * ============================================================
     */

    /**
     * Parses and verifies a JWT token.
     *
     * <p>
     * This method verifies the token signature and parses its claims.
     *
     * @param token JWT string
     * @return parsed JWT claims
     * @throws io.jsonwebtoken.JwtException
     *                                      if the token is invalid
     */
    public Claims parseToken(String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /*
     * ============================================================
     * TOKEN VALIDATION
     * ============================================================
     */

    /**
     * Validates a JWT token.
     *
     * <p>
     * The token is considered valid if:
     *
     * <ul>
     * <li>The signature is valid</li>
     * <li>The token is correctly formatted</li>
     * <li>The token has not expired</li>
     * </ul>
     *
     * @param token JWT string
     * @return {@code true} if the token is valid;
     *         {@code false} otherwise
     */
    public boolean validateToken(String token) {

        try {

            parseToken(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /*
     * ============================================================
     * TOKEN TYPE
     * ============================================================
     */

    /**
     * Gets the token type from a JWT.
     *
     * @param token JWT string
     * @return token type
     */
    public String getTokenTypeFromToken(String token) {

        return parseToken(token)
                .get(
                        CLAIM_TOKEN_TYPE,
                        String.class);
    }

    /**
     * Checks whether the JWT is an organization-selection token.
     *
     * @param token JWT string
     * @return {@code true} if the token is a selection token
     */
    public boolean isSelectionToken(String token) {

        return TOKEN_TYPE_SELECTION.equals(
                getTokenTypeFromToken(token));
    }

    /**
     * Checks whether the JWT is a full access token.
     *
     * @param token JWT string
     * @return {@code true} if the token is an access token
     */
    public boolean isAccessToken(String token) {

        return TOKEN_TYPE_ACCESS.equals(
                getTokenTypeFromToken(token));
    }

    /*
     * ============================================================
     * USERNAME
     * ============================================================
     */

    /**
     * Extracts the username from a JWT token.
     *
     * @param token JWT string
     * @return username stored in the token
     */
    public String getUsernameFromToken(String token) {

        return parseToken(token)
                .getSubject();
    }

    /*
     * ============================================================
     * USER ID
     * ============================================================
     */

    /**
     * Extracts the user identifier from a JWT token.
     *
     * @param token JWT string
     * @return user identifier
     */
    public UUID getUserIdFromToken(String token) {

        return UUID.fromString(
                parseToken(token)
                        .get(
                                CLAIM_USER_ID,
                                String.class));
    }

    /*
     * ============================================================
     * ORGANIZATION ID
     * ============================================================
     */

    /**
     * Extracts the organization identifier from an access JWT.
     *
     * @param token JWT string
     * @return organization identifier
     */
    public UUID getOrganizationIdFromToken(String token) {

        return UUID.fromString(
                parseToken(token)
                        .get(
                                CLAIM_ORG_ID,
                                String.class));
    }

    /*
     * ============================================================
     * ORGANIZATION CODE
     * ============================================================
     */

    /**
     * Extracts the organization code from an access JWT.
     *
     * @param token JWT string
     * @return organization code
     */
    public String getOrganizationCodeFromToken(String token) {

        return parseToken(token)
                .get(
                        CLAIM_ORG_CODE,
                        String.class);
    }

    /*
     * ============================================================
     * ROLE
     * ============================================================
     */

    /**
     * Extracts the user's role code from an access JWT.
     *
     * @param token JWT string
     * @return role code
     */
    public String getRoleCodeFromToken(String token) {

        return parseToken(token)
                .get(
                        CLAIM_ROLE,
                        String.class);
    }

    /*
     * ============================================================
     * EXPIRATION
     * ============================================================
     */

    /**
     * Returns the configured access-token expiration time.
     *
     * @return token expiration time in seconds
     */
    public long getExpirationInSeconds() {

        return jwtExpirationMs / 1000;
    }

    /**
     * Returns the selection-token expiration time.
     *
     * @return selection token expiration time in seconds
     */
    public long getSelectionTokenExpirationInSeconds() {

        return 5 * 60L;
    }

    /**
     * Generates an access JWT for a user with organization context.
     *
     * @param userDetails authenticated user with organization and role information
     * @return signed access JWT
     */
    public String generateAccessToken(CustomUserDetails userDetails) {

        Date now = new Date();

        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()

                /*
                 * Subject
                 */
                .subject(userDetails.getUsername())

                /*
                 * User information
                 */
                .claim(
                        CLAIM_USER_ID,
                        userDetails.getUserId().toString())

                /*
                 * Organization information
                 */
                .claim(
                        CLAIM_ORG_ID,
                        userDetails.getOrganizationId().toString())

                .claim(
                        CLAIM_ORG_NAME,
                        userDetails.getOrganizationName())

                .claim(
                        CLAIM_ORG_CODE,
                        userDetails.getOrganizationCode())

                /*
                 * Authorization
                 */
                .claim(
                        CLAIM_ROLE,
                        userDetails.getRoleCode())

                /*
                 * User information
                 */
                .claim(
                        CLAIM_FULL_NAME,
                        userDetails.getFullName())

                /*
                 * Token type
                 */
                .claim(
                        CLAIM_TOKEN_TYPE,
                        TOKEN_TYPE_ACCESS)

                /*
                 * Timestamps
                 */
                .issuedAt(now)
                .expiration(expiryDate)

                /*
                 * Signature
                 */
                .signWith(getKey())

                .compact();
    }
}