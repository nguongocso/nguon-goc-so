package vn.nguongocso.auth.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.enums.LoginResult;

/**
 * Ghi nhận mỗi lần đăng nhập của người dùng (thành công hoặc thất bại).
 * 
 * <p>
 * Bản ghi này được tạo cho mỗi request đăng nhập, bất kể vai trò của người dùng.
 * Dùng để phát hiện bất thường đăng nhập và xây dựng lịch sử đăng nhập.
 * </p>
 */
@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {
    
    @Id
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;
    
    /**
     * Người dùng thực hiện đăng nhập. Null nếu username không khớp tài khoản nào.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
    
    /**
     * Username được nhập vào, giữ lại kể cả khi sai.
     */
    @Column(nullable = false, length = 255)
    private String usernameInput;
    
    /**
     * Kết quả đăng nhập: SUCCESS hoặc FAILED.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginResult result;
    
    /**
     * Địa chỉ IP nguồn của request đăng nhập.
     */
    @Column(nullable = false, length = 45)
    private String ipAddress;
    
    /**
     * Mã quốc gia suy ra từ IP qua GeoIP (mã country code 2 chữ).
     * Nullable vì không phải request nào cũng có GeoIP match.
     */
    @Column(nullable = true, length = 2)
    private String countryCode;
    
    /**
     * Đánh dấu nếu đây là quốc gia chưa từng ghi nhận SUCCESS cho tài khoản.
     * Chỉ có ý nghĩa khi result = SUCCESS.
     */
    @Column(nullable = false)
    private Boolean isNewCountry = false;
    
    /**
     * Thời điểm xảy ra lần đăng nhập, lấy từ server.
     */
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (isNewCountry == null) {
            isNewCountry = false;
        }
    }
}
