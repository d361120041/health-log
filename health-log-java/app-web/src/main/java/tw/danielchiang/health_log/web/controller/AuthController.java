package tw.danielchiang.health_log.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.danielchiang.health_log.model.dto.AuthResponseDTO;
import tw.danielchiang.health_log.model.dto.LoginRequestDTO;
import tw.danielchiang.health_log.service.AuthService;

/**
 * 認證控制器
 * 處理登入、登出、Token 刷新等認證相關請求
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 天（秒）

    private final AuthService authService;

    /**
     * 使用者登入
     * @param loginRequest 登入請求
     * @param response HTTP 響應（用於設置 Cookie）
     * @return 認證響應（包含 Access Token）
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            HttpServletResponse response) {
        try {
            AuthResponseDTO authResponse = authService.login(loginRequest);

            // 設置 Refresh Token 到 HTTP-only Cookie
            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, authResponse.getRefreshTokenId());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(false); // 生產環境應設置為 true（HTTPS）
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(COOKIE_MAX_AGE);
            response.addCookie(refreshTokenCookie);

            // 不返回 Refresh Token ID（已在 Cookie 中）
            authResponse.setRefreshTokenId(null);

            return ResponseEntity.ok(authResponse);
        } catch (BadCredentialsException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 刷新 Access Token
     * @param refreshToken Refresh Token ID（從 Cookie 中獲取）
     * @param response HTTP 響應（用於設置新的 Cookie）
     * @return 新的認證響應（包含新的 Access Token）
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AuthResponseDTO authResponse = authService.refreshToken(refreshToken);

            // 設置新的 Refresh Token 到 HTTP-only Cookie
            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, authResponse.getRefreshTokenId());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(false); // 生產環境應設置為 true（HTTPS）
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(COOKIE_MAX_AGE);
            response.addCookie(refreshTokenCookie);

            // 不返回 Refresh Token ID（已在 Cookie 中）
            authResponse.setRefreshTokenId(null);

            return ResponseEntity.ok(authResponse);
        } catch (BadCredentialsException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 使用者登出
     * @param refreshToken Refresh Token ID（從 Cookie 中獲取）
     * @param response HTTP 響應（用於清除 Cookie）
     * @return 成功響應
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            authService.logout(refreshToken);
        }

        // 清除 Refresh Token Cookie
        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok().build();
    }
}

