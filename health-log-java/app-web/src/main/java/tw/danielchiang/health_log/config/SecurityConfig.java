package tw.danielchiang.health_log.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import tw.danielchiang.health_log.service.AuthService;
import tw.danielchiang.health_log.web.filter.JwtRequestFilter;

/**
 * Spring Security 配置類
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final AuthService authService;

    /**
     * 配置安全過濾鏈
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（使用 JWT，不需要 CSRF 保護）
            .csrf(csrf -> csrf.disable())
            // 配置授權規則
            .authorizeHttpRequests(auth -> auth
                // 公開端點
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers("/api/settings/fields").permitAll() // 公開端點，用於動態表單渲染
                // Admin 專用端點
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他端點需要認證
                .anyRequest().authenticated()
            )
            // 配置用戶詳情服務
            .userDetailsService(authService)
            // 禁用 Session（使用 JWT，無狀態）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 添加 JWT 過濾器
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 認證管理器 Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

