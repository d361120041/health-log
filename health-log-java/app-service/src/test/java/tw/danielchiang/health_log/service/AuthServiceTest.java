package tw.danielchiang.health_log.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import tw.danielchiang.health_log.data.repository.UserRepository;
import tw.danielchiang.health_log.model.dto.reponse.AuthResponseDTO;
import tw.danielchiang.health_log.model.dto.request.LoginRequestDTO;
import tw.danielchiang.health_log.model.entity.Role;
import tw.danielchiang.health_log.model.entity.User;

/**
 * AuthService 測試
 * 重點測試階段一修改後的邏輯：password_hash 可為 null 的處理
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Role userRole;
    private User userWithPassword;
    private User userWithoutPassword; // OAuth2 使用者
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        // 建立測試角色
        userRole = new Role();
        userRole.setRoleName("USER");

        // 建立有密碼的使用者（傳統註冊使用者）
        userWithPassword = new User();
        userWithPassword.setId(1L);
        userWithPassword.setEmail("user@example.com");
        userWithPassword.setPasswordHash("$2a$10$hashedpassword");
        userWithPassword.setRole(userRole);
        userWithPassword.setIsActive(true);

        // 建立沒有密碼的使用者（OAuth2 使用者）
        userWithoutPassword = new User();
        userWithoutPassword.setId(2L);
        userWithoutPassword.setEmail("oauth@example.com");
        userWithoutPassword.setPasswordHash(null); // password_hash 為 null
        userWithoutPassword.setRole(userRole);
        userWithoutPassword.setIsActive(true);
        userWithoutPassword.setOauth2Provider("GOOGLE");
        userWithoutPassword.setOauth2Id("google-id-123");

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void testLogin_WithValidPassword_ShouldSucceed() {
        // Given: 使用者存在且有密碼
        when(userRepository.findByEmailAndIsActive("user@example.com", true))
                .thenReturn(Optional.of(userWithPassword));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);
        when(jwtTokenUtil.generateAccessToken(1L, "user@example.com", "USER"))
                .thenReturn("test-access-token");
        when(jwtTokenUtil.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenService.saveRefreshToken(1L))
                .thenReturn("test-refresh-token-id");

        // When: 執行登入
        AuthResponseDTO response = authService.login(loginRequest);

        // Then: 應該成功並返回 token
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("test-access-token");
        assertThat(response.getRefreshTokenId()).isEqualTo("test-refresh-token-id");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900000L);
    }

    @Test
    void testLogin_WithInvalidPassword_ShouldThrowBadCredentialsException() {
        // Given: 使用者存在但密碼錯誤
        when(userRepository.findByEmailAndIsActive("user@example.com", true))
                .thenReturn(Optional.of(userWithPassword));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword"))
                .thenReturn(false);
        loginRequest.setPassword("wrongpassword");

        // When/Then: 應該拋出 BadCredentialsException
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void testLogin_WithOAuth2User_ShouldThrowBadCredentialsException() {
        // Given: 使用者存在但沒有密碼（OAuth2 使用者）
        loginRequest.setEmail("oauth@example.com");
        when(userRepository.findByEmailAndIsActive("oauth@example.com", true))
                .thenReturn(Optional.of(userWithoutPassword));

        // When/Then: 應該拋出 BadCredentialsException（因為 OAuth2 使用者不能使用密碼登入）
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void testLogin_WithUserNotFound_ShouldThrowBadCredentialsException() {
        // Given: 使用者不存在
        when(userRepository.findByEmailAndIsActive("notfound@example.com", true))
                .thenReturn(Optional.empty());
        loginRequest.setEmail("notfound@example.com");

        // When/Then: 應該拋出 BadCredentialsException
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void testLogin_WithInactiveUser_ShouldThrowBadCredentialsException() {
        // Given: 使用者存在但未啟用
        User inactiveUser = new User();
        inactiveUser.setId(3L);
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPasswordHash("$2a$10$hashedpassword");
        inactiveUser.setRole(userRole);
        inactiveUser.setIsActive(false);

        when(userRepository.findByEmailAndIsActive("inactive@example.com", true))
                .thenReturn(Optional.empty());
        loginRequest.setEmail("inactive@example.com");

        // When/Then: 應該拋出 BadCredentialsException
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void testLogin_WithEmptyPasswordHash_ShouldThrowBadCredentialsException() {
        // Given: 使用者存在但 password_hash 為空字串
        User userWithEmptyPassword = new User();
        userWithEmptyPassword.setId(4L);
        userWithEmptyPassword.setEmail("empty@example.com");
        userWithEmptyPassword.setPasswordHash(""); // 空字串
        userWithEmptyPassword.setRole(userRole);
        userWithEmptyPassword.setIsActive(true);

        when(userRepository.findByEmailAndIsActive("empty@example.com", true))
                .thenReturn(Optional.of(userWithEmptyPassword));
        loginRequest.setEmail("empty@example.com");

        // When/Then: 應該拋出 BadCredentialsException（因為空字串視為沒有密碼）
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void testLoadUserByUsername_WithPasswordHash_ShouldReturnUserDetails() {
        // Given: 使用者有密碼
        when(userRepository.findByEmailAndIsActive("user@example.com", true))
                .thenReturn(Optional.of(userWithPassword));

        // When: 載入使用者詳情
        org.springframework.security.core.userdetails.UserDetails userDetails = 
                authService.loadUserByUsername("user@example.com");

        // Then: 應該返回正確的 UserDetails
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedpassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");
    }

    @Test
    void testLoadUserByUsername_WithoutPasswordHash_ShouldReturnUserDetailsWithNoop() {
        // Given: 使用者沒有密碼（OAuth2 使用者）
        when(userRepository.findByEmailAndIsActive("oauth@example.com", true))
                .thenReturn(Optional.of(userWithoutPassword));

        // When: 載入使用者詳情
        org.springframework.security.core.userdetails.UserDetails userDetails = 
                authService.loadUserByUsername("oauth@example.com");

        // Then: 應該返回 UserDetails，password 為 "{noop}"
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("oauth@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("{noop}"); // 沒有密碼時使用 {noop}
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");
    }

    @Test
    void testLoadUserByUsername_WithUserNotFound_ShouldThrowUsernameNotFoundException() {
        // Given: 使用者不存在
        when(userRepository.findByEmailAndIsActive("notfound@example.com", true))
                .thenReturn(Optional.empty());

        // When/Then: 應該拋出 UsernameNotFoundException
        assertThatThrownBy(() -> authService.loadUserByUsername("notfound@example.com"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessage("User not found: notfound@example.com");
    }
}

