package tw.danielchiang.health_log.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密碼編碼器配置類
 * 獨立配置以避免循環依賴
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 密碼編碼器 Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

