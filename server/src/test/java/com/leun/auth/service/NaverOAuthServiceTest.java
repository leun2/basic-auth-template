package com.leun.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.leun.auth.config.OAuthConfig;
import com.leun.auth.dto.AuthDto;
import com.leun.auth.util.JwtUtil;
import com.leun.user.entity.User;
import com.leun.user.entity.User.ProviderType;
import com.leun.user.entity.User.UserRole;
import com.leun.user.entity.UserProfile;
import com.leun.user.entity.UserSetting;
import com.leun.user.repository.UserProfileRepository;
import com.leun.user.repository.UserRepository;
import com.leun.user.repository.UserSettingRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class NaverOAuthServiceTest {


    @Mock
    private OAuthConfig oauthConfig;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OAuthService oAuthService;

    private final String TEST_NAVER_CLIENT_ID = "naverClientId";
    private final String TEST_NAVER_CLIENT_SECRET = "naverClientSecret";
    private final String TEST_NAVER_REDIRECT_URI = "naverRedirectUri";
    private final String TEST_NAVER_TOKEN_URI = "naverTokenUri";
    private final String TEST_NAVER_USER_INFO_URI = "naverUserInfoUri";

    @BeforeEach
    void setUp() {

    }

    @Test
    @DisplayName("Naver Login - 새로운 사용자 성공")
    void naverLoginWithAuthCode_NewUser_Success() throws Exception {
        String authCode = "validNaverAuthCode";
        String testEmail = "newnaveruser@naver.com";
        String testName = "New Naver User";
        String testImageUrl = "http://example.com/newnaveruser.jpg";
        String testNaverUserId = "123456789";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Mock Naver Token Response
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("access_token", "naverAccessToken");
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(tokenResponseMap,
            HttpStatus.OK);

        // Mock Naver User Info Response
        Map<String, Object> userInfoResponseMap = new HashMap<>();
        Map<String, Object> responseBodyMap = new HashMap<>();
        responseBodyMap.put("id", testNaverUserId);
        responseBodyMap.put("email", testEmail);
        responseBodyMap.put("name", testName);
        responseBodyMap.put("profile_image", testImageUrl);
        userInfoResponseMap.put("response", responseBodyMap);
        ResponseEntity<Map> userInfoResponseEntity = new ResponseEntity<>(userInfoResponseMap,
            HttpStatus.OK);

        // Mock RestTemplate exchanges
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_TOKEN_URI),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(tokenResponseEntity);

        when(oauthConfig.getNaverUserInfoUri()).thenReturn(TEST_NAVER_USER_INFO_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_USER_INFO_URI),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(userInfoResponseEntity);

        // Mock UserRepository to simulate no existing user
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        // Mock UserRepository save to return the user object
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(
            invocation -> invocation.getArgument(0));
        when(userProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSettingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateAccessToken(anyString())).thenReturn("testJwtToken");

        // Call the method under test
        AuthDto.Response response = oAuthService.naverLoginWithAuthCode(authCode);

        // Verify the result
        assertNotNull(response);
        assertEquals(testName, response.getName());
        assertEquals(testImageUrl, response.getImage());
        assertEquals("testJwtToken", response.getAccessToken());

        // Verify interactions
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class));
        verify(userRepository, times(1)).findByEmail(testEmail);

        // Capture the User object saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(testEmail, savedUser.getEmail());
        assertEquals(ProviderType.NAVER, savedUser.getProvider());
        assertEquals(UserRole.ROLE_USER, savedUser.getUserRole());
        assertEquals("encodedPassword",
            savedUser.getPassword()); // Verify password encoding (placeholder)

        // Capture UserProfile and UserSetting objects saved
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository, times(1)).save(userProfileCaptor.capture());
        UserProfile savedProfile = userProfileCaptor.getValue();
        assertEquals(testName, savedProfile.getName());
        assertEquals(testImageUrl, savedProfile.getImage());
        assertEquals(savedUser, savedProfile.getUser());

        ArgumentCaptor<UserSetting> userSettingCaptor = ArgumentCaptor.forClass(UserSetting.class);
        verify(userSettingRepository, times(1)).save(userSettingCaptor.capture());
        UserSetting savedSetting = userSettingCaptor.getValue();
        assertEquals(savedUser, savedSetting.getUser());

        verify(passwordEncoder, times(1)).encode(anyString()); // Check if encoding was called
        verify(jwtUtil, times(1)).generateAccessToken(testEmail);
    }

    @Test
    @DisplayName("Naver Login - 기존 Naver 사용자 성공")
    void naverLoginWithAuthCode_ExistingNaverUser_Success() throws Exception {
        String authCode = "validNaverAuthCode";
        String testEmail = "existingnaveruser@naver.com";
        String testName = "Existing Naver User";
        String testImageUrl = "http://example.com/existingnaveruser.jpg";
        String testNaverUserId = "987654321";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Create an existing user entity
        User existingUser = new User(testEmail, "hashedPassword", ProviderType.NAVER,
            UserRole.ROLE_USER);
        UserProfile existingProfile = new UserProfile(existingUser, testName, testImageUrl);
        UserSetting existingSetting = new UserSetting(existingUser, "Korean", "South Korea",
            "KST +09:00");

        // Mock Naver Token Response
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("access_token", "naverAccessToken");
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(tokenResponseMap,
            HttpStatus.OK);

        // Mock Naver User Info Response
        Map<String, Object> userInfoResponseMap = new HashMap<>();
        Map<String, Object> responseBodyMap = new HashMap<>();
        responseBodyMap.put("id", testNaverUserId);
        responseBodyMap.put("email", testEmail);
        responseBodyMap.put("name", testName);
        responseBodyMap.put("profile_image", testImageUrl);
        userInfoResponseMap.put("response", responseBodyMap);
        ResponseEntity<Map> userInfoResponseEntity = new ResponseEntity<>(userInfoResponseMap,
            HttpStatus.OK);

        // Mock RestTemplate exchanges
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_TOKEN_URI),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(tokenResponseEntity);

        when(oauthConfig.getNaverUserInfoUri()).thenReturn(TEST_NAVER_USER_INFO_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_USER_INFO_URI),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(userInfoResponseEntity);

        // Mock UserRepository to simulate an existing Naver user
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateAccessToken(anyString())).thenReturn("testJwtToken");



        // Call the method under test
        AuthDto.Response response = oAuthService.naverLoginWithAuthCode(authCode);

        // Verify the result
        assertNotNull(response);
        // Based on the current code, it uses the payload's name/image for the response.
        assertEquals(testName, response.getName());
        assertEquals(testImageUrl, response.getImage());
        assertEquals("testJwtToken", response.getAccessToken());

        // Verify interactions
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class));
        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class)); // Should not save
        verify(userProfileRepository, never()).save(
            any(UserProfile.class)); // Should not save profile
        verify(userSettingRepository, never()).save(
            any(UserSetting.class)); // Should not save settings
        verify(passwordEncoder, never()).encode(
            anyString()); // Should not encode password for existing user
        verify(jwtUtil, times(1)).generateAccessToken(testEmail);
    }

    @Test
    @DisplayName("Naver Login - 다른 Provider 기존 사용자 실패")
    void naverLoginWithAuthCode_ExistingUserDifferentProvider_ThrowsException() throws Exception {
        String authCode = "validNaverAuthCode";
        String testEmail = "existinguser@google.com";
        String testName = "Existing User";
        String testImageUrl = "http://example.com/existinguser.jpg";
        String testNaverUserId = "987654321";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Create an existing user entity with a different provider (Google)
        User existingUser = new User(testEmail, "hashedPassword", ProviderType.GOOGLE,
            UserRole.ROLE_USER);

        // Mock Naver Token Response
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("access_token", "naverAccessToken");
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(tokenResponseMap,
            HttpStatus.OK);

        // Mock Naver User Info Response
        Map<String, Object> userInfoResponseMap = new HashMap<>();
        Map<String, Object> responseBodyMap = new HashMap<>();
        responseBodyMap.put("id", testNaverUserId);
        responseBodyMap.put("email", testEmail); // Email matches the existing Google user
        responseBodyMap.put("name", testName);
        responseBodyMap.put("profile_image", testImageUrl);
        userInfoResponseMap.put("response", responseBodyMap);
        ResponseEntity<Map> userInfoResponseEntity = new ResponseEntity<>(userInfoResponseMap,
            HttpStatus.OK);

        // Mock RestTemplate exchanges
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_TOKEN_URI),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(tokenResponseEntity);

        when(oauthConfig.getNaverUserInfoUri()).thenReturn(TEST_NAVER_USER_INFO_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_USER_INFO_URI),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(userInfoResponseEntity);

        // Mock UserRepository to simulate an existing user with Google provider
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));

        // Call the method under test and expect an exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        // Verify the exception message
        assertEquals("이미 가입된 이메일입니다.", exception.getMessage());

        // Verify interactions
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class));
        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class)); // Should not save
        verify(jwtUtil, never()).generateAccessToken(anyString()); // Should not generate token
    }

    @Test
    @DisplayName("Naver Login - 인증 코드 누락 또는 빈 값 실패")
    void naverLoginWithAuthCode_NullOrEmptyAuthCode_ThrowsException() {
        // Test with null
        IllegalArgumentException nullException = assertThrows(IllegalArgumentException.class,
            () -> oAuthService.naverLoginWithAuthCode(null));
        assertEquals("Authorization code cannot be null or empty.", nullException.getMessage());

        // Test with empty string
        IllegalArgumentException emptyException = assertThrows(IllegalArgumentException.class,
            () -> oAuthService.naverLoginWithAuthCode(""));
        assertEquals("Authorization code cannot be null or empty.", emptyException.getMessage());

        // Verify no interactions with dependencies
        verifyNoInteractions(oauthConfig, userRepository, userProfileRepository,
            userSettingRepository, passwordEncoder, jwtUtil, restTemplate);
    }

    @Test
    @DisplayName("Naver Login - 토큰 교환 실패 (RestTemplate Exception)")
    void naverLoginWithAuthCode_TokenExchangeFails_ThrowsException() {
        String authCode = "authCode";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Mock RestTemplate exchange for token to throw an exception
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_TOKEN_URI),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenThrow(new RuntimeException("Simulated RestTemplate Error"));

        // Call the method under test and expect an exception
        Exception exception = assertThrows(Exception.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        // Verify the exception message
        assertTrue(
            exception.getMessage().contains("Failed to exchange auth code for tokens with Naver."));
        assertTrue(exception.getCause() instanceof RuntimeException);

        // Verify interactions
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verifyNoMoreInteractions(restTemplate); // No further RestTemplate calls
        verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
            passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Naver Login - 토큰 응답 본문이 null인 경우 예외 발생")
    void naverLoginWithAuthCode_NullTokenResponseBody_ThrowsException() {
        String authCode = "authCode";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        ResponseEntity<Map> nullBodyResponseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(nullBodyResponseEntity);

        Exception exception = assertThrows(Exception.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        assertEquals("Failed to get access token from Naver token endpoint.",
            exception.getMessage());

        verify(restTemplate).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verifyNoMoreInteractions(restTemplate);
        verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
            passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Naver Login - 응답 본문에 access_token 키가 없는 경우 예외 발생")
    void naverLoginWithAuthCode_ResponseWithoutAccessToken_ThrowsException() {
        String authCode = "authCode";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        Map<String, Object> noTokenResponseMap = new HashMap<>();

        noTokenResponseMap.put("some_other_key", "value");
        ResponseEntity<Map> noTokenResponseEntity = new ResponseEntity<>(noTokenResponseMap, HttpStatus.OK);

        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(noTokenResponseEntity);

        Exception exception = assertThrows(Exception.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        assertEquals("Failed to get access token from Naver token endpoint.",
            exception.getMessage());

        verify(restTemplate).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verifyNoMoreInteractions(restTemplate);
        verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
            passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Naver Login - 사용자 정보 조회 실패 (RestTemplate Exception)")
    void naverLoginWithAuthCode_UserInfoFails_ThrowsException() {
        String authCode = "authCode";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Mock Naver Token Response (successful)
        Map<String, Object> tokenResponseMap = new HashMap<>();

        tokenResponseMap.put("access_token", "naverAccessToken");
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(tokenResponseMap,
            HttpStatus.OK);

        // Mock RestTemplate exchange for token to be successful
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_TOKEN_URI),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(tokenResponseEntity);

        // Mock RestTemplate exchange for user info to throw an exception
        when(oauthConfig.getNaverUserInfoUri()).thenReturn(TEST_NAVER_USER_INFO_URI);

        when(restTemplate.exchange(
            eq(TEST_NAVER_USER_INFO_URI),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenThrow(new RuntimeException("Simulated User Info Error"));

        // Call the method under test and expect an exception
        Exception exception = assertThrows(Exception.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Failed to get user info from Naver API."));
        assertTrue(exception.getCause() instanceof RuntimeException);

        // Verify interactions
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class));
        verifyNoMoreInteractions(restTemplate);
        verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
            passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Naver Login - 사용자 정보 응답 형식 오류 (response 키 누락)")
    void naverLoginWithAuthCode_UserInfoMissingResponseKey_ThrowsException() {
        String authCode = "authCode";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Mock Naver Token Response (successful)
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("access_token", "naverAccessToken");
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(tokenResponseMap,
            HttpStatus.OK);

        // Mock Naver User Info Response with missing 'response' key
        Map<String, Object> userInfoResponseMap = new HashMap<>();
        userInfoResponseMap.put("some_other_key", "value"); // Missing "response" key
        ResponseEntity<Map> userInfoResponseEntity = new ResponseEntity<>(userInfoResponseMap,
            HttpStatus.OK);

        // Mock RestTemplate exchanges
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(tokenResponseEntity);

        when(oauthConfig.getNaverUserInfoUri()).thenReturn(TEST_NAVER_USER_INFO_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(userInfoResponseEntity);

        // Call the method under test and expect an exception
        Exception exception = assertThrows(Exception.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        // Verify the exception message
        assertEquals("Invalid user info response from Naver API.", exception.getMessage());

        // Verify interactions
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class));
        verifyNoMoreInteractions(restTemplate);
        verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
            passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Naver Login - 사용자 정보 응답 response 필드 Null")
    void naverLoginWithAuthCode_UserInfoResponseFieldNull_ThrowsException() {
        String authCode = "authCode";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Mock Naver Token Response (successful)
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("access_token", "naverAccessToken");
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(tokenResponseMap,
            HttpStatus.OK);

        // Mock Naver User Info Response with null 'response' field
        Map<String, Object> userInfoResponseMap = new HashMap<>();
        userInfoResponseMap.put("response", null); // Null "response" field
        ResponseEntity<Map> userInfoResponseEntity = new ResponseEntity<>(userInfoResponseMap,
            HttpStatus.OK);

        // Mock RestTemplate exchanges
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(tokenResponseEntity);

        when(oauthConfig.getNaverUserInfoUri()).thenReturn(TEST_NAVER_USER_INFO_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(userInfoResponseEntity);

        // Call the method under test and expect an exception
        Exception exception = assertThrows(Exception.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        // Verify the exception message
        assertEquals("Naver user info 'response' field is null.", exception.getMessage());

        // Verify interactions
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, times(1)).exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class));
        verifyNoMoreInteractions(restTemplate);
        verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
            passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Naver Login - 사용자 정보 응답에서 id 누락 시 예외 발생")
    void naverLoginWithAuthCode_UserInfoMissingId_ThrowsException() {
        String authCode = "authCode";
        String testName = "Test User";
        String testImageUrl = "http://example.com/testuser.jpg";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Mock Token 응답
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("access_token", "naverAccessToken");
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(tokenResponseMap, HttpStatus.OK);

        // Mock User Info 응답 (id 누락)
        Map<String, Object> userInfoResponseMap = new HashMap<>();
        Map<String, Object> responseBodyMap = new HashMap<>();
        responseBodyMap.put("email", "test@example.com");
        responseBodyMap.put("name", testName);
        responseBodyMap.put("profile_image", testImageUrl);
        userInfoResponseMap.put("response", responseBodyMap);
        ResponseEntity<Map> userInfoResponseEntity = new ResponseEntity<>(userInfoResponseMap, HttpStatus.OK);

        // Mock 설정
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(tokenResponseEntity);

        when(oauthConfig.getNaverUserInfoUri()).thenReturn(TEST_NAVER_USER_INFO_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(userInfoResponseEntity);

        Exception exception = assertThrows(Exception.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        assertEquals("Required user info (id or email) missing from Naver API response.", exception.getMessage());

        verify(restTemplate).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verify(restTemplate).exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class));
        verifyNoMoreInteractions(restTemplate);
        verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
            passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Naver Login - 사용자 정보 응답에서 email 누락 시 예외 발생")
    void naverLoginWithAuthCode_UserInfoMissingEmail_ThrowsException() {
        String authCode = "authCode";
        String testName = "Test User";
        String testImageUrl = "http://example.com/testuser.jpg";

        when(oauthConfig.getNaverClientId()).thenReturn(TEST_NAVER_CLIENT_ID);
        when(oauthConfig.getNaverClientSecret()).thenReturn(TEST_NAVER_CLIENT_SECRET);
        when(oauthConfig.getNaverRedirectUri()).thenReturn(TEST_NAVER_REDIRECT_URI);

        // Mock Token 응답
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("access_token", "naverAccessToken");
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(tokenResponseMap, HttpStatus.OK);

        // Mock User Info 응답 (email 누락)
        Map<String, Object> userInfoResponseMap = new HashMap<>();
        Map<String, Object> responseBodyMap = new HashMap<>();
        responseBodyMap.put("id", "12345");
        responseBodyMap.put("name", testName);
        responseBodyMap.put("profile_image", testImageUrl);
        userInfoResponseMap.put("response", responseBodyMap);
        ResponseEntity<Map> userInfoResponseEntity = new ResponseEntity<>(userInfoResponseMap, HttpStatus.OK);

        // Mock 설정
        when(oauthConfig.getNaverTokenUri()).thenReturn(TEST_NAVER_TOKEN_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(tokenResponseEntity);

        when(oauthConfig.getNaverUserInfoUri()).thenReturn(TEST_NAVER_USER_INFO_URI);

        when(restTemplate.exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class)))
            .thenReturn(userInfoResponseEntity);

        Exception exception = assertThrows(Exception.class,
            () -> oAuthService.naverLoginWithAuthCode(authCode));

        assertEquals("Required user info (id or email) missing from Naver API response.", exception.getMessage());

        verify(restTemplate).exchange(eq(TEST_NAVER_TOKEN_URI), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(Map.class));
        verify(restTemplate).exchange(eq(TEST_NAVER_USER_INFO_URI), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Map.class));
        verifyNoMoreInteractions(restTemplate);
        verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
            passwordEncoder, jwtUtil);
    }
}
