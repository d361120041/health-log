package tw.danielchiang.health_log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "tw.danielchiang.health_log")
@EnableJpaRepositories(basePackages = "tw.danielchiang.health_log")
public class HealthLogApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthLogApplication.class, args);
	}

}

