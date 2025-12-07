package tw.danielchiang.health_log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "tw.danielchiang.health_log")
@EnableJpaRepositories(basePackages = "tw.danielchiang.health_log.data.repository")
@EntityScan(basePackages = "tw.danielchiang.health_log.model.entity")
public class HealthLogApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthLogApplication.class, args);
	}

}

