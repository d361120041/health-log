package tw.danielchiang.health_log.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
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
import tw.danielchiang.health_log.data.repository.RoleRepository;
import tw.danielchiang.health_log.data.repository.UserRepository;
import tw.danielchiang.health_log.model.dto.reponse.AuthResponseDTO;
import tw.danielchiang.health_log.model.dto.request.LoginRequestDTO;
import tw.danielchiang.health_log.model.dto.request.RegisterRequestDTO;
import tw.danielchiang.health_log.model.dto.request.VerifyEmailRequestDTO;

/**
 * 認證服務
 * 處理登入、登出、Token 刷新等認證相關業務邏輯
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private static final SecureRandom secureRandom = new SecureRandom();

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

        // 檢查使用者是否有密碼（OAuth2 使用者沒有密碼）
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            log.warn("Login attempt with OAuth2 account (no password): {}", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

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
     * 使用者註冊
     * @param registerRequest 註冊請求 DTO
     * @throws IllegalArgumentException 如果 Email 已存在或密碼不匹配
     */
    @Transactional
    public void register(RegisterRequestDTO registerRequest) {
        // 檢查密碼確認
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("密碼與確認密碼不匹配");
        }

        // 檢查 Email 是否已存在
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("此 Email 已被使用");
        }

        // 取得 USER 角色
        tw.danielchiang.health_log.model.entity.Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new IllegalStateException("USER 角色不存在，請檢查資料庫初始化"));

        // 創建新使用者
        tw.danielchiang.health_log.model.entity.User user = new tw.danielchiang.health_log.model.entity.User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(userRole);
        user.setIsActive(false); // 未驗證前設為未啟用
        user.setEmailVerificationToken(generateVerificationToken());
        user.setEmailVerifiedAt(null);

        // 儲存使用者
        user = userRepository.save(user);
        
        // 發送驗證郵件
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());
            log.info("User registered successfully: userId={}, email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email for user: {}", user.getEmail(), e);
            // 註冊成功但郵件發送失敗，記錄錯誤但不拋出異常（使用者可以稍後重新發送）
        }
    }

    /**
     * 驗證 Email
     * @param verifyEmailRequest Email 驗證請求 DTO
     * @throws BadCredentialsException 如果 Token 無效或已過期
     */
    @Transactional
    public void verifyEmail(VerifyEmailRequestDTO verifyEmailRequest) {
        // 根據 Token 查找使用者
        Optional<tw.danielchiang.health_log.model.entity.User> userOpt = 
                userRepository.findByEmailVerificationToken(verifyEmailRequest.getToken());
        
        if (userOpt.isEmpty()) {
            log.warn("Email verification failed: invalid token");
            throw new BadCredentialsException("驗證 Token 無效或已過期");
        }

        tw.danielchiang.health_log.model.entity.User user = userOpt.get();

        // 檢查是否已經驗證過
        if (Boolean.TRUE.equals(user.getIsActive()) && user.getEmailVerifiedAt() != null) {
            log.info("Email already verified: userId={}, email={}", user.getId(), user.getEmail());
            return; // 已經驗證過，直接返回成功
        }

        // 啟用帳號並記錄驗證時間
        user.setIsActive(true);
        user.setEmailVerifiedAt(OffsetDateTime.now());
        user.setEmailVerificationToken(null); // 清除 Token

        userRepository.save(user);
        log.info("Email verified successfully: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * 生成安全的驗證 Token
     * @return 驗證 Token（Base64 編碼）
     */
    private String generateVerificationToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
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

        // Spring Security UserDetails 要求 password 不能為 null
        // 如果使用者沒有密碼（OAuth2 使用者），設置一個空字串（實際上這些使用者不會通過 UserDetailsService 登入）
        String password = user.getPasswordHash() != null 
            ? user.getPasswordHash() 
            : "{noop}"; // noop 表示無操作（這些使用者應該通過 OAuth2 登入）

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(password)
                .authorities("ROLE_" + user.getRole().getRoleName())
                .build();
    }
}

