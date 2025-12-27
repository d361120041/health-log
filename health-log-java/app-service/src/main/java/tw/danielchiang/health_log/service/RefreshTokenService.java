package tw.danielchiang.health_log.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token 服務
 * 負責處理 Refresh Token 在 Redis 中的儲存、查詢和失效
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token.expiration:604800000}")
    private long refreshTokenExpiration; // 預設 7 天（毫秒）

    /**
     * 儲存 Refresh Token
     * @param userId 使用者 ID
     * @return Refresh Token ID (UUID)
     */
    public String saveRefreshToken(Long userId) {
        String tokenId = UUID.randomUUID().toString();
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        String value = userId.toString();
        
        // 儲存到 Redis，設定過期時間
        redisTemplate.opsForValue().set(key, value, refreshTokenExpiration, TimeUnit.MILLISECONDS);
        log.debug("Refresh token saved: tokenId={}, userId={}", tokenId, userId);
        
        return tokenId;
    }

    /**
     * 根據 Refresh Token ID 查詢使用者 ID
     * @param tokenId Refresh Token ID
     * @return 使用者 ID，如果不存在則返回 null
     */
    public Long getUserIdByTokenId(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        String userIdStr = redisTemplate.opsForValue().get(key);
        
        if (userIdStr == null) {
            log.debug("Refresh token not found: tokenId={}", tokenId);
            return null;
        }
        
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.error("Invalid userId format in Redis: tokenId={}, userIdStr={}", tokenId, userIdStr, e);
            return null;
        }
    }

    /**
     * 刪除 Refresh Token（用於登出或刷新時撤銷舊 Token）
     * @param tokenId Refresh Token ID
     */
    public void deleteRefreshToken(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Refresh token deleted: tokenId={}, success={}", tokenId, deleted);
    }

    /**
     * 檢查 Refresh Token 是否存在且有效
     * @param tokenId Refresh Token ID
     * @return 是否存在
     */
    public boolean exists(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}

