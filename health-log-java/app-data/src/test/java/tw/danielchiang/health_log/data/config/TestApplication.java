package tw.danielchiang.health_log.data.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 測試用的 Spring Boot 應用程式類
 * 用於 @DataJpaTest 找到 @SpringBootConfiguration
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "tw.danielchiang.health_log.data.repository")
@EntityScan(basePackages = "tw.danielchiang.health_log.model.entity")
public class TestApplication {
}

