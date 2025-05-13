package com.leun.user.service;

import com.leun.user.dto.UserDto;
import com.leun.user.dto.UserProfileDto;
import com.leun.user.dto.UserSettingDto;
import com.leun.user.entity.User;
import com.leun.user.entity.User.ProviderType;
import com.leun.user.entity.User.UserRole;
import com.leun.user.entity.UserProfile;
import com.leun.user.entity.UserSetting;
import com.leun.user.repository.UserProfileRepository;
import com.leun.user.repository.UserRepository;
import com.leun.user.repository.UserSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserSettingRepository userSettingRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // --- register 메서드 테스트 ---
    @Test
    @DisplayName("회원가입 성공 - 일반 사용자")
    void register_Success_UserRole() throws Exception {
        // Given (준비)
        UserDto.Request request = new UserDto.Request("test@example.com", "password123", "TestUser");
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty()); // 이메일 중복 없음
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword"); // 비밀번호 인코딩

        // When (실행)
        userService.register(request);

        // Then (검증)
        // UserRepository의 save 메서드가 호출되었는지 확인
        verify(userRepository, times(1)).save(any(User.class));
        // UserProfileRepository의 save 메서드가 호출되었는지 확인
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
        // UserSettingRepository의 save 메서드가 호출되었는지 확인
        verify(userSettingRepository, times(1)).save(any(UserSetting.class));
    }

    @Test
    @DisplayName("회원가입 성공 - 관리자 (leun@admin)")
    void register_Success_AdminRole() throws Exception {
        // Given
        UserDto.Request request = new UserDto.Request("leun@admin", "adminpassword", "AdminUser");
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedAdminPassword");

        // When
        userService.register(request);

        // Then
        // userRepository.save 호출 시 전달된 User 객체의 UserRole이 ADMIN인지 확인
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository).save(argThat(user -> user.getUserRole() == UserRole.ROLE_ADMIN));
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
        verify(userSettingRepository, times(1)).save(any(UserSetting.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    void register_Failure_EmailExists() {
        // Given
        UserDto.Request request = new UserDto.Request("existing@example.com", "password123", "ExistingUser");
        // userRepository.findByEmail이 이미 존재하는 User를 반환하도록 설정
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(new User()));

        // When & Then (예외 발생 검증)
        assertThrows(IllegalArgumentException.class, () -> userService.register(request),
            "User Already Exists");

        // save 메서드들이 호출되지 않았는지 확인
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(userSettingRepository, never()).save(any(UserSetting.class));
    }

    // --- getUserProfileByEmail 메서드 테스트 ---
    @Test
    @DisplayName("프로필 조회 성공")
    void getUserProfileByEmail_Success() throws Exception {
        // Given
        String email = "test@example.com";
        UserProfileDto.Response mockResponse = new UserProfileDto.Response("TestUser", "/profile.jpg");
        given(userProfileRepository.findUserProfileByEmail(email)).willReturn(mockResponse);

        // When
        UserProfileDto.Response result = userService.getUserProfileByEmail(email);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getName()).isEqualTo("TestUser");
        assertThat(result.getImage()).isEqualTo("/profile.jpg");
        verify(userProfileRepository, times(1)).findUserProfileByEmail(email);
    }

    @Test
    @DisplayName("프로필 조회 실패 - 사용자 프로필 없음")
    void getUserProfileByEmail_Failure_NotFound() {
        // Given
        String email = "nonexistent@example.com";
        given(userProfileRepository.findUserProfileByEmail(email)).willReturn(null);

        // When & Then
        assertThrows(NoSuchElementException.class, () -> userService.getUserProfileByEmail(email),
            "User profile not found for email: " + email);
        verify(userProfileRepository, times(1)).findUserProfileByEmail(email);
    }

    // --- getUserSettingByEmail 메서드 테스트 ---
    @Test
    @DisplayName("사용자 설정 조회 성공")
    void getUserSettingByEmail_Success() throws Exception {
        // Given
        String email = "test@example.com";
        UserSettingDto.Response mockResponse = new UserSettingDto.Response("Korean", "South Korea", "KST +09:00");
        given(userSettingRepository.findUserSettingByEmail(email)).willReturn(mockResponse);

        // When
        UserSettingDto.Response result = userService.getUserSettingByEmail(email);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("Korean");
        verify(userSettingRepository, times(1)).findUserSettingByEmail(email);
    }

    @Test
    @DisplayName("사용자 설정 조회 실패 - 설정 없음")
    void getUserSettingByEmail_Failure_NotFound() {
        // Given
        String email = "nonexistent@example.com";
        given(userSettingRepository.findUserSettingByEmail(email)).willReturn(null);

        // When & Then
        assertThrows(NoSuchElementException.class, () -> userService.getUserSettingByEmail(email),
            "User setting not found for email: " + email);
        verify(userSettingRepository, times(1)).findUserSettingByEmail(email);
    }

    // --- updateUserProfileName 메서드 테스트 ---
    @Test
    @DisplayName("사용자 프로필 이름 업데이트 성공")
    void updateUserProfileName_Success() throws Exception {
        // Given
        String email = "test@example.com";
        String newName = "UpdatedName";
        UserProfileDto.Response mockResponse = new UserProfileDto.Response(newName, "/profile.jpg");

        given(userProfileRepository.findUserProfileByEmail(email)).willReturn(mockResponse);

        // When
        UserProfileDto.Response result = userService.updateUserProfileName(email, newName);

        // Then
        verify(userProfileRepository, times(1)).updateUserName(email, newName);
        verify(userProfileRepository, times(1)).findUserProfileByEmail(email);
        assertThat(result.getName()).isEqualTo(newName);
    }

    // --- removeUser 메서드 테스트 ---
    @Test
    @DisplayName("사용자 삭제 성공")
    void removeUser_Success() throws Exception {
        // Given
        String email = "userToDelete@example.com";

        // When
        userService.removeUser(email);

        // Then
        verify(userRepository, times(1)).deleteUserByEmail(email); // deleteUserByEmail이 호출되었는지 확인
    }

    // --- findUserByEmail 메서드 테스트 ---
    @Test
    @DisplayName("이메일로 사용자 찾기 성공")
    void findUserByEmail_Success() throws Exception {
        // Given
        String email = "found@example.com";
        User mockUser = new User(email, "pwd", ProviderType.LOCAL, UserRole.ROLE_USER);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));

        // When
        User foundUser = userService.findUserByEmail(email);

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo(email);
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("이메일로 사용자 찾기 실패 - 사용자 없음")
    void findUserByEmail_Failure_NotFound() {
        // Given
        String email = "notfound@example.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class, () -> userService.findUserByEmail(email),
            "User Does Not Exist");
        verify(userRepository, times(1)).findByEmail(email);
    }

}