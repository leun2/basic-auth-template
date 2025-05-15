package com.leun;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leun.auth.dto.AuthDto;
import com.leun.auth.dto.OAuthDto;
import com.leun.auth.dto.OAuthDto.GoogleRequest;
import com.leun.auth.dto.OAuthDto.NaverRequest;
import com.leun.auth.service.OAuthService;
import com.leun.user.dto.UserDto;
import com.leun.user.repository.UserRepository;
import com.leun.user.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {"jwt.secret=thisistestingsecretkeyforjwtauthenticationanditissolongenough"})
public class ApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private OAuthService oAuthService;

    private static final String TEST_USER_EMAIL = "test@user";
    private static final String TEST_USER_PASSWORD = "password1234!";
    private static final String TEST_USER_NAME = "User";

    private static final String TEST_ADMIN_EMAIL = "test@admin";
    private static final String TEST_ADMIN_PASSWORD = "password1234!";
    private static final String TEST_ADMIN_NAME = "Admin";

    @BeforeEach
    void setUp() throws Exception {
        if (userRepository.findByEmail(TEST_USER_EMAIL).isEmpty()) {
            userService.register(new UserDto.Request(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_NAME));
        }
        if (userRepository.findByEmail(TEST_ADMIN_EMAIL).isEmpty()) {
            userService.register(new UserDto.Request(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD, TEST_ADMIN_NAME));
        }
    }

    // --- Local Login Endpoint Tests ---

    @Test
    @DisplayName("POST /v1/auth/login - 유효한 자격증명으로 로그인 성공")
    void login_withValidCredentials_success() throws Exception {
        // Given
        AuthDto.Request loginRequest = new AuthDto.Request(TEST_USER_EMAIL, TEST_USER_PASSWORD);

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.name").value(TEST_USER_NAME))
            .andExpect(jsonPath("$.image").value("/default"));
    }

    @Test
    @DisplayName("POST /v1/auth/login - 유효하지 않은 비밀번호로 로그인 시도 시 401 Unauthorized")
    void login_withInvalidPassword_returns401() throws Exception {
        // Given
        AuthDto.Request loginRequest = new AuthDto.Request(TEST_USER_EMAIL, "wrongpassword");

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /v1/auth/login - 존재하지 않는 이메일로 로그인 시도 시 401 Unauthorized")
    void login_withNonExistingEmail_returns401() throws Exception {
        // Given
        AuthDto.Request loginRequest = new AuthDto.Request("nonexistent@example.com", "password");

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // --- OAuth Endpoint Tests ---

    @Test
    @DisplayName("POST /v1/auth/google/login - 유효한 인증 코드로 구글 로그인 시 성공")
    void googleLogin_withValidCode_success() throws Exception {
        // Given
        OAuthDto.GoogleRequest googleRequest = new GoogleRequest("some_valid_google_auth_code");

        AuthDto.Response mockAuthResponse = new AuthDto.Response(
            "Google User", "/google-image.jpg", "mock_google_jwt_token");

        // given(oAuthService.googleLoginWithAuthCode(anyString())).willReturn(mockAuthResponse);

        // SpyBean (oAuthService) 스텁 시 doReturn().when() 구문을 사용하는 것이 권장
        doReturn(mockAuthResponse)
            .when(oAuthService)
            .googleLoginWithAuthCode(anyString());

        // When & Then
        mockMvc.perform(post("/v1/auth/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(googleRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(mockAuthResponse.getName()))
            .andExpect(jsonPath("$.image").value(mockAuthResponse.getImage()))
            .andExpect(jsonPath("$.token").value(mockAuthResponse.getToken()));

        verify(oAuthService, times(1)).googleLoginWithAuthCode(anyString());
    }

    @Test
    @DisplayName("POST /v1/auth/google/login - 빈 인증 코드로 구글 로그인 시 400 Bad Request")
    void googleLogin_withEmptyCode_returns400() throws Exception {
        // Given
        OAuthDto.GoogleRequest googleRequest = new OAuthDto.GoogleRequest("");

        // When & Then
        mockMvc.perform(post("/v1/auth/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(googleRequest)))
            .andExpect(status().isBadRequest());

        verify(oAuthService, never()).googleLoginWithAuthCode(anyString());
    }

    @Test
    @DisplayName("POST /v1/auth/naver/login - 유효한 인증 코드로 네이버 로그인 시 성공")
    void naverLogin_withValidCode_success() throws Exception {
        // Given
        OAuthDto.NaverRequest naverRequest = new NaverRequest("some_valid_naver_auth_code");

        AuthDto.Response mockAuthResponse = new AuthDto.Response(
            "Naver User", "/naver-image.png", "mock_naver_jwt_token");

        doReturn(mockAuthResponse)
            .when(oAuthService)
            .naverLoginWithAuthCode(anyString());

        // When & Then
        mockMvc.perform(post("/v1/auth/naver/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(naverRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(mockAuthResponse.getName()))
            .andExpect(jsonPath("$.image").value(mockAuthResponse.getImage()))
            .andExpect(jsonPath("$.token").value(mockAuthResponse.getToken()));

        verify(oAuthService, times(1)).naverLoginWithAuthCode(anyString());
    }

    @Test
    @DisplayName("POST /v1/auth/naver/login - 빈 인증 코드로 네이버 로그인 시 400 Bad Request")
    void naverLogin_withEmptyCode_returns400() throws Exception {
        // Given
        OAuthDto.NaverRequest naverRequest = new OAuthDto.NaverRequest("");

        // When & Then
        mockMvc.perform(post("/v1/auth/naver/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(naverRequest)))
            .andExpect(status().isBadRequest());

        verify(oAuthService, never()).naverLoginWithAuthCode(anyString());
    }

    // --- Protected Endpoint Security Tests ---

    @Test
    @DisplayName("GET /v1/user/profile - 유효한 JWT 토큰과 함께 요청 시 성공")
    void accessProtectedEndpoint_withValidToken_success() throws Exception {
        // Given
        AuthDto.Request loginRequest = new AuthDto.Request(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        // When & Then
        mockMvc.perform(get("/v1/user/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(TEST_USER_EMAIL));
    }

    @Test
    @DisplayName("GET /v1/user/profile - JWT 토큰 없이 요청 시 401 Unauthorized")
    void accessProtectedEndpoint_withoutToken_returns401() throws Exception {
        // Given (No Authorization header)

        // When & Then
        mockMvc.perform(get("/v1/user/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/user/profile - 유효하지 않은 JWT 토큰으로 요청 시 401 Unauthorized")
    void accessProtectedEndpoint_withInvalidToken_returns401() throws Exception {
        // Given
        String invalidToken = "invalid.fake.token";

        // When & Then
        mockMvc.perform(get("/v1/user/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
            .andExpect(status().isUnauthorized());
    }

    // --- Role-Based Access Control Tests ---

    @Test
    @DisplayName("GET /v1/admin/profile - ROLE_USER로 관리자 엔드포인트 요청 시 403 Forbidden")
    void accessAdminEndpoint_asUser_returns403() throws Exception {
        // Given
        AuthDto.Request loginRequest = new AuthDto.Request(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        // When & Then
        mockMvc.perform(get("/v1/admin/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/admin/profile - ROLE_ADMIN로 관리자 엔드포인트 요청 시 성공")
    void accessAdminEndpoint_asAdmin_success() throws Exception {
        // Given
        AuthDto.Request loginRequest = new AuthDto.Request(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        // When & Then
        mockMvc.perform(get("/v1/admin/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(TEST_ADMIN_EMAIL))
            .andExpect(jsonPath("$.name").value(TEST_ADMIN_NAME));
    }
}
