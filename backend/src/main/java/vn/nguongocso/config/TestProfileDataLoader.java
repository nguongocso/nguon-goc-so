package vn.nguongocso.config;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.organization.entity.Organization;

/**
 * Bộ nạp dữ liệu khởi tạo cho môi trường kiểm thử runtime (profile test).
 * <p>
 * Điều phối các bộ bootstrap tổ chức và người dùng, sau đó xuất thông tin token
 * và khóa API ra tệp tạm phục vụ kiểm thử tự động.
 */
@Slf4j
@Component
@Profile("runtime-test")
@RequiredArgsConstructor
public class TestProfileDataLoader implements CommandLineRunner {

    public static final String ACTIVE_TEST_RAW_KEY = "nks_test_e8a1b2c3d4e5f678901234567890abcdef";
    public static final String EXPIRED_TEST_RAW_KEY = "nks_test_expired1234567890abcdef12345678";

    private final TestOrganizationBootstrap organizationBootstrap;
    private final TestUserBootstrap userBootstrap;

    @Override
    public void run(String... args) throws Exception {
        log.info("==> [RUNTIME-TEST] Khởi tạo dữ liệu mẫu cho profile test...");

        Organization org = organizationBootstrap.bootstrapOrganization();
        TestUserBootstrap.BootstrapUserData userData = userBootstrap.bootstrapUsers(org);

        organizationBootstrap.bootstrapPartnerApiKeys(
                org,
                userData.getManagerUser(),
                ACTIVE_TEST_RAW_KEY,
                EXPIRED_TEST_RAW_KEY
        );

        ProductionLot lot = organizationBootstrap.bootstrapProductLot(org);
        writeTokensEnvFile(userData, lot);

        log.info("==> [RUNTIME-TEST] Khởi tạo dữ liệu mẫu hoàn tất!");
    }

    private void writeTokensEnvFile(TestUserBootstrap.BootstrapUserData userData, ProductionLot lot) {
        try (FileWriter writer = new FileWriter("/tmp/test_tokens.env", StandardCharsets.UTF_8)) {
            writer.write("COOPERATIVE_MANAGER_TOKEN=" + userData.getManagerToken() + "\n");
            writer.write("EVENT_RECORDER_TOKEN=" + userData.getRecorderToken() + "\n");
            writer.write("SAMPLE_LOT_ID=00000000-0000-0000-0000-000000000001\n");
            writer.write("REAL_LOT_ID=" + lot.getId().toString() + "\n");
            writer.write("ACTIVE_TEST_KEY=" + ACTIVE_TEST_RAW_KEY + "\n");
            writer.write("EXPIRED_TEST_KEY=" + EXPIRED_TEST_RAW_KEY + "\n");
            log.info("==> [RUNTIME-TEST] Đã lưu thông tin kiểm thử vào /tmp/test_tokens.env (realLotId={})", lot.getId());
        } catch (IOException e) {
            log.warn("==> [RUNTIME-TEST] Không thể ghi file /tmp/test_tokens.env: {}", e.getMessage());
        }
    }
}
