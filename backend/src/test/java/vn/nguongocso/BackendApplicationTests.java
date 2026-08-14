package vn.nguongocso;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BackendApplicationTests {

	@Container
	static MySQLContainer<?> mysql =
			new MySQLContainer<>("mysql:8.0")
					.withDatabaseName("nguon_goc_so_test")
					.withUsername("test")
					.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add(
				"spring.datasource.driver-class-name",
				() -> "com.mysql.cj.jdbc.Driver"
		);
	}

	@Test
	void contextLoads() {
	}
}
