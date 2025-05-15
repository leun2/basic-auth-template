package com.leun.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

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

    private final String TEST_GOOGLE_CLIENT_ID = "googleClientId";
    private final String TEST_GOOGLE_CLIENT_SECRET = "googleClientSecret";
    private final String TEST_GOOGLE_REDIRECT_URI = "googleRedirectUri";

    @BeforeEach
    void setUp() {

    }

    @Test
    @DisplayName("Google Login - 새로운 사용자 성공")
    void googleLoginWithAuthCode_NewUser_Success() throws Exception {
        String authCode = "validAuthCode";
        String testEmail = "newuser@google.com";
        String testName = "New User";
        String testImageUrl = "http://example.com/newuser.jpg";

        // Mock GoogleTokenResponse
        GoogleTokenResponse mockTokenResponse = mock(GoogleTokenResponse.class);
        when(oauthConfig.getGoogleClientId()).thenReturn(TEST_GOOGLE_CLIENT_ID);
        when(oauthConfig.getGoogleClientSecret()).thenReturn(TEST_GOOGLE_CLIENT_SECRET);
        when(oauthConfig.getGoogleRedirectUri()).thenReturn(TEST_GOOGLE_REDIRECT_URI);

        when(mockTokenResponse.getIdToken()).thenReturn("validIdTokenString");

        // Mock GoogleIdToken.Payload
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockPayload.getEmail()).thenReturn(testEmail);
        when(mockPayload.get("name")).thenReturn(testName);
        when(mockPayload.get("picture")).thenReturn(testImageUrl);

        // Mock GoogleIdToken
        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);

        // Mock GoogleIdTokenVerifier
        GoogleIdTokenVerifier mockIdTokenVerifier = mock(GoogleIdTokenVerifier.class);
        when(mockIdTokenVerifier.verify(anyString())).thenReturn(mockIdToken);

        // Use mockConstruction to mock the creation of GoogleAuthorizationCodeTokenRequest and GoogleIdTokenVerifier
        try (MockedConstruction<GoogleAuthorizationCodeTokenRequest> reqMock = Mockito.mockConstruction(
            GoogleAuthorizationCodeTokenRequest.class,
            (mock, context) -> {
                // Assert constructor arguments if needed
                // assertEquals(TEST_GOOGLE_CLIENT_ID, context.arguments().get(2));
                when(mock.execute()).thenReturn(mockTokenResponse);
            });
            MockedConstruction<GoogleIdTokenVerifier.Builder> verifierBuilderMock = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    when(mock.setAudience(anyList())).thenReturn(mock);
                    when(mock.build()).thenReturn(mockIdTokenVerifier);
                })) {

            // Mock UserRepository to simulate no existing user
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
            // Mock UserRepository save to return the user object
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
            when(userProfileRepository.save(any())).thenAnswer(
                invocation -> invocation.getArgument(0));
            when(userSettingRepository.save(any())).thenAnswer(
                invocation -> invocation.getArgument(0));
            when(jwtUtil.generateToken(anyString())).thenReturn("testJwtToken");

            // Call the method under test
            AuthDto.Response response = oAuthService.googleLoginWithAuthCode(authCode);

            // Verify the result
            assertNotNull(response);
            assertEquals(testName, response.getName());
            assertEquals(testImageUrl, response.getImage());
            assertEquals("testJwtToken", response.getToken());

            // Verify interactions
            verify(userRepository, times(1)).findByEmail(testEmail);
            // Capture the User object saved
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals(testEmail, savedUser.getEmail());
            assertEquals(ProviderType.GOOGLE, savedUser.getProvider());
            assertEquals(UserRole.ROLE_USER, savedUser.getUserRole());
            assertEquals("encodedPassword", savedUser.getPassword()); // Verify password encoding

            // Capture UserProfile and UserSetting objects saved
            ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(
                UserProfile.class);
            verify(userProfileRepository, times(1)).save(userProfileCaptor.capture());
            UserProfile savedProfile = userProfileCaptor.getValue();
            assertEquals(testName, savedProfile.getName());
            assertEquals(testImageUrl, savedProfile.getImage());
            assertEquals(savedUser, savedProfile.getUser());

            ArgumentCaptor<UserSetting> userSettingCaptor = ArgumentCaptor.forClass(
                UserSetting.class);
            verify(userSettingRepository, times(1)).save(userSettingCaptor.capture());
            UserSetting savedSetting = userSettingCaptor.getValue();
            assertEquals(savedUser, savedSetting.getUser());

            verify(passwordEncoder, times(1)).encode(anyString()); // Check if encoding was called
            verify(jwtUtil, times(1)).generateToken(testEmail);
        }
    }

    @Test
    @DisplayName("Google Login - 기존 Google 사용자 성공")
    void googleLoginWithAuthCode_ExistingGoogleUser_Success() throws Exception {
        String authCode = "validAuthCode";
        String testEmail = "existinguser@google.com";
        String testName = "Existing User";
        String testImageUrl = "http://example.com/existinguser.jpg";

        // Create an existing user entity
        User existingUser = new User(testEmail, "hashedPassword", ProviderType.GOOGLE,
            UserRole.ROLE_USER);
        UserProfile existingProfile = new UserProfile(existingUser, testName, testImageUrl);
        UserSetting existingSetting = new UserSetting(existingUser, "Korean", "South Korea",
            "KST +09:00");

        // Mock GoogleTokenResponse
        GoogleTokenResponse mockTokenResponse = mock(GoogleTokenResponse.class);
        when(oauthConfig.getGoogleClientId()).thenReturn(TEST_GOOGLE_CLIENT_ID);
        when(oauthConfig.getGoogleClientSecret()).thenReturn(TEST_GOOGLE_CLIENT_SECRET);
        when(oauthConfig.getGoogleRedirectUri()).thenReturn(TEST_GOOGLE_REDIRECT_URI);

        when(mockTokenResponse.getIdToken()).thenReturn("validIdTokenString");

        // Mock GoogleIdToken.Payload
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockPayload.getEmail()).thenReturn(testEmail);
        when(mockPayload.get("name")).thenReturn(testName);
        when(mockPayload.get("picture")).thenReturn(testImageUrl);

        // Mock GoogleIdToken
        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);

        // Mock GoogleIdTokenVerifier
        GoogleIdTokenVerifier mockIdTokenVerifier = mock(GoogleIdTokenVerifier.class);
        when(mockIdTokenVerifier.verify(anyString())).thenReturn(mockIdToken);

        // Use mockConstruction to mock the creation
        try (MockedConstruction<GoogleAuthorizationCodeTokenRequest> reqMock = Mockito.mockConstruction(
            GoogleAuthorizationCodeTokenRequest.class,
            (mock, context) -> when(mock.execute()).thenReturn(mockTokenResponse));
            MockedConstruction<GoogleIdTokenVerifier.Builder> verifierBuilderMock = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    when(mock.setAudience(anyList())).thenReturn(mock);
                    when(mock.build()).thenReturn(mockIdTokenVerifier);
                })) {

            // Mock UserRepository to simulate an existing Google user
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));
            when(jwtUtil.generateToken(anyString())).thenReturn("testJwtToken");

            // Call the method under test
            AuthDto.Response response = oAuthService.googleLoginWithAuthCode(authCode);

            // Verify the result
            assertNotNull(response);

            assertEquals(testName, response.getName());
            assertEquals(testImageUrl, response.getImage());
            assertEquals("testJwtToken", response.getToken());

            // Verify interactions
            verify(userRepository, times(1)).findByEmail(testEmail);
            verify(userRepository, never()).save(any(User.class)); // Should not save
            verify(userProfileRepository, never()).save(
                any(UserProfile.class)); // Should not save profile
            verify(userSettingRepository, never()).save(
                any(UserSetting.class)); // Should not save settings
            verify(passwordEncoder, never()).encode(
                anyString()); // Should not encode password for existing user
            verify(jwtUtil, times(1)).generateToken(testEmail);
        }
    }

    @Test
    @DisplayName("Google Login - 다른 Provider 기존 사용자 실패")
    void googleLoginWithAuthCode_ExistingUserDifferentProvider_ThrowsException() throws Exception {
        String authCode = "validAuthCode";
        String testEmail = "existinguser@naver.com";
        String testName = "Existing User";
        String testImageUrl = "http://example.com/existinguser.jpg";

        // Create an existing user entity with a different provider (Naver)
        User existingUser = new User(testEmail, "hashedPassword", ProviderType.NAVER,
            UserRole.ROLE_USER);

        // Mock GoogleTokenResponse
        GoogleTokenResponse mockTokenResponse = mock(GoogleTokenResponse.class);
        when(oauthConfig.getGoogleClientId()).thenReturn(TEST_GOOGLE_CLIENT_ID);
        when(oauthConfig.getGoogleClientSecret()).thenReturn(TEST_GOOGLE_CLIENT_SECRET);
        when(oauthConfig.getGoogleRedirectUri()).thenReturn(TEST_GOOGLE_REDIRECT_URI);

        when(mockTokenResponse.getIdToken()).thenReturn("validIdTokenString");

        // Mock GoogleIdToken.Payload
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockPayload.getEmail()).thenReturn(testEmail);
        when(mockPayload.get("name")).thenReturn(testName);
        when(mockPayload.get("picture")).thenReturn(testImageUrl);

        // Mock GoogleIdToken
        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);

        // Mock GoogleIdTokenVerifier
        GoogleIdTokenVerifier mockIdTokenVerifier = mock(GoogleIdTokenVerifier.class);
        when(mockIdTokenVerifier.verify(anyString())).thenReturn(mockIdToken);

        // Use mockConstruction to mock the creation
        try (MockedConstruction<GoogleAuthorizationCodeTokenRequest> reqMock = Mockito.mockConstruction(
            GoogleAuthorizationCodeTokenRequest.class,
            (mock, context) -> when(mock.execute()).thenReturn(mockTokenResponse));
            MockedConstruction<GoogleIdTokenVerifier.Builder> verifierBuilderMock = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    when(mock.setAudience(anyList())).thenReturn(mock);
                    when(mock.build()).thenReturn(mockIdTokenVerifier);
                })) {

            // Mock UserRepository to simulate an existing user with Naver provider
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));

            // Call the method under test and expect an exception
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> oAuthService.googleLoginWithAuthCode(authCode));

            // Verify the exception message
            assertEquals("이미 가입된 이메일입니다.", exception.getMessage());

            // Verify interactions
            verify(userRepository, times(1)).findByEmail(testEmail);
            verify(userRepository, never()).save(any(User.class)); // Should not save
            verify(jwtUtil, never()).generateToken(anyString()); // Should not generate token
        }
    }

    @Test
    @DisplayName("Google Login - 인증 코드 누락 또는 빈 값 실패")
    void googleLoginWithAuthCode_NullOrEmptyAuthCode_ThrowsException() {
        // Test with null
        IllegalArgumentException nullException = assertThrows(IllegalArgumentException.class,
            () -> oAuthService.googleLoginWithAuthCode(null));
        assertEquals("Authorization code cannot be null or empty.", nullException.getMessage());

        // Test with empty string
        IllegalArgumentException emptyException = assertThrows(IllegalArgumentException.class,
            () -> oAuthService.googleLoginWithAuthCode(""));
        assertEquals("Authorization code cannot be null or empty.", emptyException.getMessage());

        // Verify no interactions with dependencies
        verifyNoInteractions(oauthConfig, userRepository, userProfileRepository,
            userSettingRepository, passwordEncoder, jwtUtil, restTemplate);
    }


    @Test
    @DisplayName("Google Login - 토큰 교환 실패 (IOException)")
    void googleLoginWithAuthCode_TokenExchangeFails_ThrowsException() {
        String authCode = "authCode";

        when(oauthConfig.getGoogleClientId()).thenReturn(TEST_GOOGLE_CLIENT_ID);
        when(oauthConfig.getGoogleClientSecret()).thenReturn(TEST_GOOGLE_CLIENT_SECRET);
        when(oauthConfig.getGoogleRedirectUri()).thenReturn(TEST_GOOGLE_REDIRECT_URI);

        // Use mockConstruction to mock the creation of GoogleAuthorizationCodeTokenRequest and throw IOException
        try (MockedConstruction<GoogleAuthorizationCodeTokenRequest> reqMock = Mockito.mockConstruction(
            GoogleAuthorizationCodeTokenRequest.class,
            (mock, context) -> when(mock.execute()).thenThrow(
                new IOException("Simulated IO Error")))) {

            // Call the method under test and expect an exception
            Exception exception = assertThrows(Exception.class,
                () -> oAuthService.googleLoginWithAuthCode(authCode));

            // Verify the exception message
            assertTrue(exception.getMessage()
                .contains("Failed to exchange auth code for tokens with Google."));
            assertTrue(exception.getCause() instanceof IOException);

            // Verify minimal interactions
            verify(oauthConfig, atLeastOnce()).getGoogleClientId(); // Called in constructor
            verify(oauthConfig, atLeastOnce()).getGoogleClientSecret(); // Called in constructor
            verify(oauthConfig, atLeastOnce()).getGoogleRedirectUri(); // Called in constructor
            verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
                passwordEncoder, jwtUtil, restTemplate);
        }
    }

    @Test
    @DisplayName("Google Login - ID 토큰 응답 누락 실패")
    void googleLoginWithAuthCode_IdTokenMissing_ThrowsException() {
        String authCode = "authCode";

        // Mock GoogleTokenResponse with null ID token
        GoogleTokenResponse mockTokenResponse = mock(GoogleTokenResponse.class);
        when(oauthConfig.getGoogleClientId()).thenReturn(TEST_GOOGLE_CLIENT_ID);
        when(oauthConfig.getGoogleClientSecret()).thenReturn(TEST_GOOGLE_CLIENT_SECRET);
        when(oauthConfig.getGoogleRedirectUri()).thenReturn(TEST_GOOGLE_REDIRECT_URI);

        when(mockTokenResponse.getIdToken()).thenReturn(null); // Simulate missing ID token

        // Use mockConstruction to mock the creation of GoogleAuthorizationCodeTokenRequest
        try (MockedConstruction<GoogleAuthorizationCodeTokenRequest> reqMock = Mockito.mockConstruction(
            GoogleAuthorizationCodeTokenRequest.class,
            (mock, context) -> when(mock.execute()).thenReturn(mockTokenResponse))) {

            // Call the method under test and expect an exception
            Exception exception = assertThrows(Exception.class,
                () -> oAuthService.googleLoginWithAuthCode(authCode));

            // Verify the exception message
            assertEquals("ID token not received from Google token endpoint.",
                exception.getMessage());

            // Verify minimal interactions
            verify(oauthConfig, atLeastOnce()).getGoogleClientId(); // Called in constructor
            verify(oauthConfig, atLeastOnce()).getGoogleClientSecret(); // Called in constructor
            verify(oauthConfig, atLeastOnce()).getGoogleRedirectUri(); // Called in constructor
            verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
                passwordEncoder, jwtUtil, restTemplate);
        }
    }

    @Test
    @DisplayName("Google Login - ID 토큰 검증 실패 (GeneralSecurityException)")
    void googleLoginWithAuthCode_IdTokenVerificationFailsSecurity_ThrowsException()
        throws GeneralSecurityException, IOException {
        String authCode = "authCode";
        String validIdTokenString = "validIdTokenString";

        // Mock GoogleTokenResponse with a valid ID token string
        GoogleTokenResponse mockTokenResponse = mock(GoogleTokenResponse.class);
        when(oauthConfig.getGoogleClientId()).thenReturn(TEST_GOOGLE_CLIENT_ID);
        when(oauthConfig.getGoogleClientSecret()).thenReturn(TEST_GOOGLE_CLIENT_SECRET);
        when(oauthConfig.getGoogleRedirectUri()).thenReturn(TEST_GOOGLE_REDIRECT_URI);

        when(mockTokenResponse.getIdToken()).thenReturn(validIdTokenString);

        // Mock GoogleIdTokenVerifier to throw GeneralSecurityException on verify
        GoogleIdTokenVerifier mockIdTokenVerifier = mock(GoogleIdTokenVerifier.class);
        try {
            when(mockIdTokenVerifier.verify(validIdTokenString)).thenThrow(
                new GeneralSecurityException("Simulated Security Error"));
        } catch (GeneralSecurityException | IOException e) {
            // This block is just to satisfy the throws clause of verify, won't be executed
        }

        // Use mockConstruction to mock the creation
        try (MockedConstruction<GoogleAuthorizationCodeTokenRequest> reqMock = Mockito.mockConstruction(
            GoogleAuthorizationCodeTokenRequest.class,
            (mock, context) -> when(mock.execute()).thenReturn(mockTokenResponse));
            MockedConstruction<GoogleIdTokenVerifier.Builder> verifierBuilderMock = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    when(mock.setAudience(anyList())).thenReturn(mock);
                    when(mock.build()).thenReturn(mockIdTokenVerifier); // Return the mock verifier
                })) {

            // Call the method under test and expect an exception
            Exception exception = assertThrows(Exception.class,
                () -> oAuthService.googleLoginWithAuthCode(authCode));

            // Verify the exception message
            assertTrue(exception.getMessage().contains("Failed to verify Google ID token."));
            assertTrue(exception.getCause() instanceof GeneralSecurityException);

            // Verify interactions
            verify(oauthConfig,
                atLeastOnce()).getGoogleClientId(); // Called in constructor and verifier builder
            verify(oauthConfig,
                atLeastOnce()).getGoogleClientSecret(); // Called in request constructor
            verify(oauthConfig,
                atLeastOnce()).getGoogleRedirectUri(); // Called in request constructor
            verify(mockIdTokenVerifier, times(1)).verify(
                validIdTokenString); // Verify verify was called
            verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
                passwordEncoder, jwtUtil, restTemplate);
        }
    }

    @Test
    @DisplayName("Google Login - ID 토큰 검증 실패 (IOException)")
    void googleLoginWithAuthCode_IdTokenVerificationFailsIO_ThrowsException()
        throws GeneralSecurityException, IOException {
        String authCode = "authCode";
        String validIdTokenString = "validIdTokenString";

        // Mock GoogleTokenResponse with a valid ID token string
        GoogleTokenResponse mockTokenResponse = mock(GoogleTokenResponse.class);
        when(oauthConfig.getGoogleClientId()).thenReturn(TEST_GOOGLE_CLIENT_ID);
        when(oauthConfig.getGoogleClientSecret()).thenReturn(TEST_GOOGLE_CLIENT_SECRET);
        when(oauthConfig.getGoogleRedirectUri()).thenReturn(TEST_GOOGLE_REDIRECT_URI);

        when(mockTokenResponse.getIdToken()).thenReturn(validIdTokenString);

        // Mock GoogleIdTokenVerifier to throw IOException on verify
        GoogleIdTokenVerifier mockIdTokenVerifier = mock(GoogleIdTokenVerifier.class);
        try {
            when(mockIdTokenVerifier.verify(validIdTokenString)).thenThrow(
                new IOException("Simulated IO Error during verification"));
        } catch (GeneralSecurityException | IOException e) {
            // This block is just to satisfy the throws clause of verify, won't be executed
        }

        // Use mockConstruction to mock the creation
        try (MockedConstruction<GoogleAuthorizationCodeTokenRequest> reqMock = Mockito.mockConstruction(
            GoogleAuthorizationCodeTokenRequest.class,
            (mock, context) -> when(mock.execute()).thenReturn(mockTokenResponse));
            MockedConstruction<GoogleIdTokenVerifier.Builder> verifierBuilderMock = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    when(mock.setAudience(anyList())).thenReturn(mock);
                    when(mock.build()).thenReturn(mockIdTokenVerifier); // Return the mock verifier
                })) {

            // Call the method under test and expect an exception
            Exception exception = assertThrows(Exception.class,
                () -> oAuthService.googleLoginWithAuthCode(authCode));

            // Verify the exception message
            assertTrue(exception.getMessage().contains("Failed to verify Google ID token."));
            assertTrue(exception.getCause() instanceof IOException);

            // Verify interactions
            verify(oauthConfig,
                atLeastOnce()).getGoogleClientId(); // Called in constructor and verifier builder
            verify(oauthConfig,
                atLeastOnce()).getGoogleClientSecret(); // Called in request constructor
            verify(oauthConfig,
                atLeastOnce()).getGoogleRedirectUri(); // Called in request constructor
            verify(mockIdTokenVerifier, times(1)).verify(
                validIdTokenString); // Verify verify was called
            verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
                passwordEncoder, jwtUtil, restTemplate);
        }
    }

    @Test
    @DisplayName("Google Login - ID 토큰 검증 후 Null 반환 실패")
    void googleLoginWithAuthCode_IdTokenVerificationReturnsNull_ThrowsException()
        throws GeneralSecurityException, IOException {
        String authCode = "authCode";
        String validIdTokenString = "validIdTokenString";

        // Mock GoogleTokenResponse with a valid ID token string
        GoogleTokenResponse mockTokenResponse = mock(GoogleTokenResponse.class);
        when(oauthConfig.getGoogleClientId()).thenReturn(TEST_GOOGLE_CLIENT_ID);
        when(oauthConfig.getGoogleClientSecret()).thenReturn(TEST_GOOGLE_CLIENT_SECRET);
        when(oauthConfig.getGoogleRedirectUri()).thenReturn(TEST_GOOGLE_REDIRECT_URI);

        when(mockTokenResponse.getIdToken()).thenReturn(validIdTokenString);

        // Mock GoogleIdTokenVerifier to return null on verify
        GoogleIdTokenVerifier mockIdTokenVerifier = mock(GoogleIdTokenVerifier.class);
        try {
            when(mockIdTokenVerifier.verify(validIdTokenString)).thenReturn(
                null); // Simulate verification failure
        } catch (GeneralSecurityException | IOException e) {
            // This block is just to satisfy the throws clause of verify, won't be executed
        }

        // Use mockConstruction to mock the creation
        try (MockedConstruction<GoogleAuthorizationCodeTokenRequest> reqMock = Mockito.mockConstruction(
            GoogleAuthorizationCodeTokenRequest.class,
            (mock, context) -> when(mock.execute()).thenReturn(mockTokenResponse));
            MockedConstruction<GoogleIdTokenVerifier.Builder> verifierBuilderMock = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    when(mock.setAudience(anyList())).thenReturn(mock);
                    when(mock.build()).thenReturn(mockIdTokenVerifier); // Return the mock verifier
                })) {

            // Call the method under test and expect an exception
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> oAuthService.googleLoginWithAuthCode(authCode));

            // Verify the exception message
            assertEquals("Invalid or expired Google ID token.", exception.getMessage());

            // Verify interactions
            verify(oauthConfig,
                atLeastOnce()).getGoogleClientId(); // Called in constructor and verifier builder
            verify(oauthConfig,
                atLeastOnce()).getGoogleClientSecret(); // Called in request constructor
            verify(oauthConfig,
                atLeastOnce()).getGoogleRedirectUri(); // Called in request constructor
            verify(mockIdTokenVerifier, times(1)).verify(
                validIdTokenString);
            verifyNoInteractions(userRepository, userProfileRepository, userSettingRepository,
                passwordEncoder, jwtUtil, restTemplate);
        }
    }

}