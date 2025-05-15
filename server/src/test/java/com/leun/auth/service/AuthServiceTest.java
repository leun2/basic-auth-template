package com.leun.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.leun.auth.dto.AuthDto;
import com.leun.auth.util.JwtUtil;
import com.leun.user.entity.User;
import com.leun.user.entity.UserProfile;
import com.leun.user.repository.UserProfileRepository;
import com.leun.user.service.UserService;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login - 유효한 이메일로 로그인 성공 시 토큰과 사용자 정보 반환")
    void login_Success_WithValidEmail() throws Exception {
        // Given
        String email = "test@example.com";
        String password = "password123";
        String userName = "Test User";
        String userImage = "/path/to/image.jpg";
        String jwtToken = "mock-jwt-token";

        AuthDto.Request loginRequest = new AuthDto.Request(email, password);
        User mockUser = new User();
        mockUser.setEmail(email);

        UserProfile mockProfile = new UserProfile();
        mockProfile.setName(userName);
        mockProfile.setImage(userImage);
        mockProfile.setUser(mockUser);

        given(userService.findUserByEmail(email)).willReturn(mockUser);
        given(jwtUtil.generateToken(email)).willReturn(jwtToken);
        given(userProfileRepository.findByUser(mockUser)).willReturn(Optional.of(mockProfile));

        // When
        AuthDto.Response loginResponse = authService.login(loginRequest);

        // Then
        assertNotNull(loginResponse);
        assertEquals(userName, loginResponse.getName());
        assertEquals(userImage, loginResponse.getImage());
        assertEquals(jwtToken, loginResponse.getToken());

        verify(userService, times(1)).findUserByEmail(email);
        verify(userProfileRepository, times(1)).findByUser(mockUser);
        verify(jwtUtil, times(1)).generateToken(email);
    }

    @Test
    @DisplayName("login - 사용자 프로필을 찾을 수 없을 때 NoSuchElementException 발생")
    void login_ThrowsNoSuchElementException_WhenUserProfileNotFound() throws Exception {
        // Given
        String email = "test@example.com";
        String password = "password123";
        AuthDto.Request loginRequest = new AuthDto.Request(email, password);
        User mockUser = new User();
        mockUser.setEmail(email);

        given(userService.findUserByEmail(email)).willReturn(mockUser);
        given(userProfileRepository.findByUser(mockUser)).willReturn(Optional.empty());

        // When & Then
        NoSuchElementException thrown = assertThrows(NoSuchElementException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("User Does Not Exist", thrown.getMessage());

        verify(userService, times(1)).findUserByEmail(email);
        verify(userProfileRepository, times(1)).findByUser(mockUser);
        verify(jwtUtil, times(0)).generateToken(email);
    }

    @Test
    @DisplayName("login - 사용자를 찾을 수 없을 때 UserService의 예외가 전파됨")
    void login_PropagatesException_WhenUserNotFoundByUserService() throws Exception {
        // Given
        String email = "nonexistent@example.com";
        String password = "password";
        AuthDto.Request loginRequest = new AuthDto.Request(email, password);

        RuntimeException userNotFoundException = new RuntimeException("User not found in UserService");
        given(userService.findUserByEmail(email)).willThrow(userNotFoundException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("User not found in UserService", thrown.getMessage());

        verify(userService, times(1)).findUserByEmail(email);
        verify(userProfileRepository, times(0)).findByUser(any(User.class));
        verify(jwtUtil, times(0)).generateToken(anyString());
    }
}
