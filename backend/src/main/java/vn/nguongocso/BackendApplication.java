package vn.nguongocso;

import java.lang.management.ManagementFactory;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	/**
	 * Múi giờ nghiệp vụ mặc định của hệ thống (Việt Nam).
	 */
	private static final String DEFAULT_BUSINESS_TIMEZONE = "Asia/Ho_Chi_Minh";

	public static void main(String[] args) {
		configureBusinessTimeZone();
		SpringApplication.run(BackendApplication.class, args);
	}

	/**
	 * Căn chỉnh múi giờ mặc định của JVM theo múi giờ nghiệp vụ
	 * (app.timezone, mặc định Asia/Ho_Chi_Minh).
	 *
	 * <p>
	 * Container/JVM thường chạy ở UTC, trong khi dữ liệu thời gian của hệ
	 * thống (LocalDateTime trong các cột DATETIME) được hiển thị theo giờ
	 * Việt Nam. Nếu không căn chỉnh, mọi lệnh gọi LocalDateTime.now() và
	 * các callback @PrePersist/@PreUpdate sẽ ghi giờ UTC, khiến createdAt /
	 * updatedAt / thời gian sự kiện bị lệch 7 giờ so với giờ nghiệp vụ.
	 *
	 * <p>
	 * Thứ tự ưu tiên: -Duser.timezone truyền tường minh khi khởi động &gt;
	 * biến môi trường APP_TIMEZONE &gt; Asia/Ho_Chi_Minh. Giữ cùng nguồn cấu
	 * hình với thuộc tính Spring app.timezone=${APP_TIMEZONE:Asia/Ho_Chi_Minh}
	 * để Clock nghiệp vụ và múi giờ JVM luôn thống nhất.
	 */
	private static void configureBusinessTimeZone() {
		// JVM luôn tự đặt user.timezone theo hệ điều hành khi khởi động, nên
		// chỉ tôn trọng giá trị này nếu nó được truyền tường minh qua -D.
		boolean explicitJvmTimezone = ManagementFactory.getRuntimeMXBean()
				.getInputArguments()
				.stream()
				.anyMatch(arg -> arg.startsWith("-Duser.timezone="));

		if (explicitJvmTimezone) {
			return;
		}

		String zoneId = System.getenv().getOrDefault("APP_TIMEZONE", DEFAULT_BUSINESS_TIMEZONE);

		try {
			TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zoneId)));
		} catch (DateTimeException e) {
			TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(DEFAULT_BUSINESS_TIMEZONE)));
		}
	}

}
