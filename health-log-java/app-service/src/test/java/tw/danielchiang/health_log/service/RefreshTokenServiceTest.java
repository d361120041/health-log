package tw.danielchiang.health_log.service;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RefreshTokenService 測試
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L);
    }

    @Test
    void testSaveRefreshToken() {
        // Given
        Long userId = 1L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        String tokenId = refreshTokenService.saveRefreshToken(userId);

        // Then
        assertThat(tokenId).isNotNull();
        assertThat(tokenId).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        verify(valueOperations, times(1)).set(
            anyString(),
            eq(userId.toString()),
            eq(604800000L),
            eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testGetUserIdByTokenId_WhenTokenExists() {
        // Given
        String tokenId = "test-token-id";
        Long expectedUserId = 1L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(expectedUserId.toString());

        // When
        Long userId = refreshTokenService.getUserIdByTokenId(tokenId);

        // Then
        assertThat(userId).isEqualTo(expectedUserId);
        verify(valueOperations, times(1)).get(anyString());
    }

    @Test
    void testGetUserIdByTokenId_WhenTokenDoesNotExist() {
        // Given
        String tokenId = "non-existent-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        Long userId = refreshTokenService.getUserIdByTokenId(tokenId);

        // Then
        assertThat(userId).isNull();
        verify(valueOperations, times(1)).get(anyString());
    }

    @Test
    void testGetUserIdByTokenId_WhenInvalidFormat() {
        // Given
        String tokenId = "test-token-id";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("invalid-number");

        // When
        Long userId = refreshTokenService.getUserIdByTokenId(tokenId);

        // Then
        assertThat(userId).isNull();
        verify(valueOperations, times(1)).get(anyString());
    }

    @Test
    void testDeleteRefreshToken() {
        // Given
        String tokenId = "test-token-id";
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        refreshTokenService.deleteRefreshToken(tokenId);

        // Then
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    void testExists_WhenTokenExists() {
        // Given
        String tokenId = "test-token-id";
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When
        boolean exists = refreshTokenService.exists(tokenId);

        // Then
        assertThat(exists).isTrue();
        verify(redisTemplate, times(1)).hasKey(anyString());
    }

    @Test
    void testExists_WhenTokenDoesNotExist() {
        // Given
        String tokenId = "non-existent-token";
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When
        boolean exists = refreshTokenService.exists(tokenId);

        // Then
        assertThat(exists).isFalse();
        verify(redisTemplate, times(1)).hasKey(anyString());
    }
}

