package vn.nguongocso.loginanomaly.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.dto.request.LoginRequest;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.loginanomaly.service.LoginAnomalyService;

/**
 * Ghi nhận các lần đăng nhập thất bại (sai mật khẩu) bằng AOP,
 * không sửa đổi bất kỳ class xác thực nào có sẵn.
 *
 * <p>
 * Khi {@code AuthService.login} ném {@code BusinessException("Sai mật khẩu")},
 * lần thất bại được lưu và hệ thống kiểm tra ngưỡng 5 lần trong 2 phút (TC-01).
 * </p>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoginAttemptAspect {

    private static final String WRONG_PASSWORD_MESSAGE = "Sai mật khẩu";

    private final LoginAnomalyService loginAnomalyService;

    /**
     * Theo dõi phương thức login của AuthService.
     */
    @Around("execution(* vn.nguongocso.auth.service.AuthService.login(..))")
    public Object trackLoginFailure(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (BusinessException e) {
            if (WRONG_PASSWORD_MESSAGE.equals(e.getMessage())) {
                recordFailure(joinPoint);
            }
            throw e;
        }
    }

    private void recordFailure(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0 || !(args[0] instanceof LoginRequest request)) {
                log.debug("Không thể xác định username trong yêu cầu đăng nhập");
                return;
            }

            loginAnomalyService.recordFailedLogin(
                    request.getUsername(),
                    IpUtils.getClientIp());
        } catch (Exception e) {
            log.error("Không thể ghi nhận lần đăng nhập thất bại", e);
        }
    }
}
