package tw.danielchiang.health_log.web.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import tw.danielchiang.health_log.service.JwtTokenUtil;

/**
 * 安全工具類
 * 用於從 SecurityContext 和 JWT Token 中獲取當前用戶信息
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 從請求中獲取當前用戶的 ID
     * @param request HTTP 請求
     * @return 用戶 ID
     * @throws IllegalStateException 如果無法獲取用戶 ID
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            if (userId != null) {
                return userId;
            }
        }
        throw new IllegalStateException("無法獲取當前用戶 ID");
    }

    /**
     * 獲取當前用戶的用戶名（email）
     * @return 用戶名
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("用戶未認證");
        }

        if (authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }

        return authentication.getName();
    }

    /**
     * 從請求中提取 JWT Token
     * @param request HTTP 請求
     * @return JWT Token 或 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

