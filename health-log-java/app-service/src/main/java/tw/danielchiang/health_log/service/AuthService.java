package tw.danielchiang.health_log.service;

import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.danielchiang.health_log.data.repository.UserRepository;
import tw.danielchiang.health_log.model.dto.AuthResponseDTO;
import tw.danielchiang.health_log.model.dto.LoginRequestDTO;

/**
 * 認證服務
 * 處理登入、登出、Token 刷新等認證相關業務邏輯
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 使用者登入
     * @param loginRequest 登入請求 DTO
     * @return 認證響應 DTO（包含 Access Token 和 Refresh Token ID）
     * @throws BadCredentialsException 如果認證失敗
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        // 查找使用者
        Optional<tw.danielchiang.health_log.model.entity.User> userOpt = userRepository.findByEmailAndIsActive(loginRequest.getEmail(), true);
        
        if (userOpt.isEmpty()) {
            log.warn("Login attempt with non-existent or inactive email: {}", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        tw.danielchiang.health_log.model.entity.User user = userOpt.get();

        // 驗證密碼
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            log.warn("Login attempt with incorrect password for email: {}", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        // 生成 Access Token
        String accessToken = jwtTokenUtil.generateAccessToken(
            user.getId(),
            user.getEmail(),
            user.getRole().getRoleName()
        );

        // 生成 Refresh Token ID 並儲存到 Redis
        String refreshTokenId = refreshTokenService.saveRefreshToken(user.getId());

        // 構建響應
        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshTokenId(refreshTokenId);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenUtil.getAccessTokenExpiration());

        log.info("User logged in successfully: userId={}, email={}", user.getId(), user.getEmail());
        return response;
    }

    /**
     * 刷新 Access Token
     * @param refreshTokenId Refresh Token ID
     * @return 新的認證響應 DTO
     * @throws BadCredentialsException 如果 Refresh Token 無效
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO refreshToken(String refreshTokenId) {
        // 從 Redis 獲取使用者 ID
        Long userId = refreshTokenService.getUserIdByTokenId(refreshTokenId);
        
        if (userId == null) {
            log.warn("Refresh token not found or invalid: {}", refreshTokenId);
            throw new BadCredentialsException("Invalid refresh token");
        }

        // 查找使用者
        Optional<tw.danielchiang.health_log.model.entity.User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getIsActive())) {
            log.warn("User not found or inactive for refresh token: userId={}", userId);
            throw new BadCredentialsException("User not found or inactive");
        }

        tw.danielchiang.health_log.model.entity.User user = userOpt.get();

        // 撤銷舊的 Refresh Token
        refreshTokenService.deleteRefreshToken(refreshTokenId);

        // 生成新的 Access Token
        String newAccessToken = jwtTokenUtil.generateAccessToken(
            user.getId(),
            user.getEmail(),
            user.getRole().getRoleName()
        );

        // 生成新的 Refresh Token ID
        String newRefreshTokenId = refreshTokenService.saveRefreshToken(user.getId());

        // 構建響應
        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(newAccessToken);
        response.setRefreshTokenId(newRefreshTokenId);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenUtil.getAccessTokenExpiration());

        log.info("Access token refreshed successfully: userId={}", userId);
        return response;
    }

    /**
     * 登出
     * @param refreshTokenId Refresh Token ID
     */
    public void logout(String refreshTokenId) {
        refreshTokenService.deleteRefreshToken(refreshTokenId);
        log.info("User logged out: refreshTokenId={}", refreshTokenId);
    }

    /**
     * 根據用戶名載入用戶詳情（用於 Spring Security）
     * @param username 用戶名 (email)
     * @return UserDetails
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<tw.danielchiang.health_log.model.entity.User> userOpt = userRepository.findByEmailAndIsActive(username, true);
        
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        tw.danielchiang.health_log.model.entity.User user = userOpt.get();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().getRoleName())
                .build();
    }
}

