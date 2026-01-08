package tw.danielchiang.health_log.config;

import java.util.Arrays;
import java.util.List;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
            // 配置 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 禁用 CSRF（使用 JWT，不需要 CSRF 保護）
            .csrf(csrf -> csrf.disable())
            // 配置授權規則
            .authorizeHttpRequests(auth -> auth
                // 公開端點
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout", 
                                 "/api/auth/register", "/api/auth/verify-email").permitAll()
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
     * CORS 配置
     * 允許前端跨域請求（支援 withCredentials）
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允許的前端來源（開發環境：支援多種訪問方式）
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://192.168.50.140:5173"  // 前端後端同機運行時的本機 IP
        ));
        
        // 允許的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 允許的請求頭
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允許發送 Cookie（必須設定，因為前端使用 withCredentials: true）
        configuration.setAllowCredentials(true);
        
        // 暴露的響應頭（前端可以訪問的響應頭）
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        // 預檢請求（OPTIONS）的緩存時間（秒）
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }

    /**
     * 認證管理器 Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

