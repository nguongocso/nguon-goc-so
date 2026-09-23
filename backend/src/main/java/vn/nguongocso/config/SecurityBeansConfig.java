package vn.nguongocso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Cấu hình các bean hạ tầng bảo mật cơ bản như PasswordEncoder và AuthenticationManager. */
@Configuration
public class SecurityBeansConfig {
    /** Khởi tạo bean mã hóa mật khẩu sử dụng thuật toán BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Khởi tạo bean quản lý xác thực từ cấu hình Spring Security. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
