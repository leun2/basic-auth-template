package com.leun.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leun.auth.config.SecurityConfiguration;
import com.leun.auth.service.CustomUserDetailsService;
import com.leun.auth.util.JwtUtil;
import com.leun.user.dto.UserDto;
import com.leun.user.dto.UserDto.Request;
import com.leun.user.dto.UserProfileDto;
import com.leun.user.dto.UserSettingDto;
import com.leun.user.dto.UserSettingDto.Request.Country;
import com.leun.user.dto.UserSettingDto.Request.Language;
import com.leun.user.dto.UserSettingDto.Request.Timezone;
import com.leun.user.dto.UserSettingDto.Response;
import com.leun.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfiguration.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserService userService;

    private final String AUTHENTICATED_USER_EMAIL = "authenticated@example.com";
    
    @Test
    @DisplayName("POST /v1/user - 사용자 등록 성공")
    void registerUser_Success_WithValidRequestBody() throws Exception {
        // Given
        UserDto.Request request = new Request("test@example.com", "password1234!", "mock_user");

        doNothing().when(userService).register(any(UserDto.Request.class));

        // When & Then
        mockMvc.perform(post("/v1/user")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        ArgumentCaptor<UserDto.Request> argumentCaptor = ArgumentCaptor.forClass(UserDto.Request.class);

        verify(userService, times(1)).register(argumentCaptor.capture());

        UserDto.Request capturedRequest = argumentCaptor.getValue();

        assertThat(capturedRequest.getEmail()).isEqualTo("test@example.com");
        assertThat(capturedRequest.getPassword()).isEqualTo("password1234!");
        assertThat(capturedRequest.getName()).isEqualTo("mock_user");
    }

    @Test
    @DisplayName("GET /v1/user/profile - 인증된 사용자 프로필 조회 성공")
    void getUserProfile_Success_Authenticated() throws Exception {
        // Given
        UserProfileDto.Response mockProfileResponse = new UserProfileDto.Response("Authenticated User", "/authenticated-image.jpg");

        given(userService.getUserProfileByEmail(AUTHENTICATED_USER_EMAIL)).willReturn(mockProfileResponse);

        // When & Then
        mockMvc.perform(get("/v1/user/profile")
                .with(user(AUTHENTICATED_USER_EMAIL))
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Authenticated User"))
            .andExpect(jsonPath("$.image").value("/authenticated-image.jpg"));

        verify(userService, times(1)).getUserProfileByEmail(AUTHENTICATED_USER_EMAIL);
    }

    @Test
    @DisplayName("GET /v1/user/profile - 미인증 사용자 접근 시 401 Unauthorized")
    void getUserProfile_Failure_Unauthenticated() throws Exception {

        // When & Then
        mockMvc.perform(get("/v1/user/profile")
                .contentType(APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        
        verify(userService, never()).getUserProfileByEmail(anyString());
    }

     @Test
     @DisplayName("GET /v1/user/setting - 설정 조회 성공")
     void getUserSetting_Success_WithMockUser() throws Exception {
         UserSettingDto.Response mockSettingResponse = new UserSettingDto.Response("English", "USA", "PST");

         given(userService.getUserSettingByEmail(AUTHENTICATED_USER_EMAIL)).willReturn(mockSettingResponse);

         mockMvc.perform(get("/v1/user/setting")
                 .with(user(AUTHENTICATED_USER_EMAIL))
                 .contentType(APPLICATION_JSON))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.language").value("English"));

         verify(userService, times(1)).getUserSettingByEmail(AUTHENTICATED_USER_EMAIL);
     }

    @Test
    @DisplayName("PATCH /v1/user/profile/name - 인증된 사용자 프로필 이름 업데이트 성공")
    void updateUserProfileName_Success_Authenticated() throws Exception {
        // Given
        UserProfileDto.Request.Name updateRequest = new UserProfileDto.Request.Name("New Name");
        UserProfileDto.Response updatedProfileResponse = new UserProfileDto.Response("New Name", "/image.jpg");

        given(userService.updateUserProfileName(AUTHENTICATED_USER_EMAIL, "New Name")).willReturn(updatedProfileResponse);

        // When & Then
        mockMvc.perform(patch("/v1/user/profile/name")
                .with(user(AUTHENTICATED_USER_EMAIL))
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"));

        verify(userService, times(1)).updateUserProfileName(AUTHENTICATED_USER_EMAIL, "New Name");
    }

    @Test
    @DisplayName("POST /v1/user/profile/image - 인증된 사용자 이미지 업로드 성공 (파일 포함)")
    void updateUserProfileImage_Success_Authenticated_WithFile() throws Exception {
        // Given
        MockMultipartFile mockImageFile = new MockMultipartFile(
            "image", // @ModelAttribute DTO 필드 이름과 일치해야 함 ("image")
            "profile.jpg", // 파일 이름
            "image/jpeg", // 파일 타입 (contentType)
            "dummy image content".getBytes() // 파일 내용 (byte 배열)
        );

        UserProfileDto.Response mockServiceResponse = new UserProfileDto.Response(
            "Authenticated User", // 이름
            "/uploaded_profile_image.jpg" // 업로드 후 예상되는 이미지 URL
        );

        given(userService.updateUserProfileImage(AUTHENTICATED_USER_EMAIL)).willReturn(mockServiceResponse);

        // When & Then
        mockMvc.perform(multipart("/v1/user/profile/image") // multipart 빌더 사용
                .file(mockImageFile) // MockMultipartFile 추가
                .with(user(AUTHENTICATED_USER_EMAIL)) // 인증된 사용자 상태 시뮬레이션
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Authenticated User"))
            .andExpect(jsonPath("$.image").value("/uploaded_profile_image.jpg"));

        verify(userService, times(1)).updateUserProfileImage(AUTHENTICATED_USER_EMAIL);
    }

    @Test
    @DisplayName("POST /v1/user/profile/image - 미인증 사용자 접근 시 401 Unauthorized")
    void updateUserProfileImage_Failure_Unauthenticated() throws Exception {
        // Given
        MockMultipartFile mockImageFile = new MockMultipartFile(
            "image",
            "profile.jpg",
            "image/jpeg",
            "dummy".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/v1/user/profile/image")
                    .file(mockImageFile)
            )
            .andExpect(status().isUnauthorized());

        verify(userService, never()).updateUserProfileImage(anyString());
    }

    @Test
    @DisplayName("POST /v1/user/profile/image - 파일 누락 시 400 Bad Request 반환 (Validation 추가 후)")
    void updateUserProfileImage_Failure_WithoutFile_AfterValidationFix() throws Exception {

        // When & Then
        mockMvc.perform(multipart("/v1/user/profile/image")
                    .with(user(AUTHENTICATED_USER_EMAIL))
                // .file() is intentionally missing
            )
            .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserProfileImage(anyString());
    }

    @Test
    @DisplayName("PATCH /v1/user/setting/language - 인증된 사용자 언어 설정 업데이트 성공")
    void updateUserSettingLanguage_Success_Authenticated() throws Exception {
        // Given
        UserSettingDto.Request.Language request = new Language("Korean");
        UserSettingDto.Response response = new Response("Korean", "South Korea", "KST +09:00");

        given(userService.updateUserSettingLanguage(AUTHENTICATED_USER_EMAIL, "Korean")).willReturn(response);

        // When & Then
        mockMvc.perform(patch("/v1/user/setting/language")
                .with(user(AUTHENTICATED_USER_EMAIL))
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("Korean"))
            .andExpect(jsonPath("$.country").value("South Korea"))
            .andExpect(jsonPath("$.timezone").value("KST +09:00"));

        verify(userService, times(1)).updateUserSettingLanguage(AUTHENTICATED_USER_EMAIL, "Korean");
    }

    @Test
    @DisplayName("PATCH /v1/user/setting/country - 인증된 사용자 국가 설정 업데이트 성공")
    void updateUserSettingCountry_Success_Authenticated() throws Exception {
        // Given
        UserSettingDto.Request.Country request = new Country("North Korea");
        UserSettingDto.Response response = new Response("Korean", "North Korea", "KST +09:00");

        given(userService.updateUserSettingCountry(AUTHENTICATED_USER_EMAIL, "North Korea")).willReturn(response);

        // When & Then
        mockMvc.perform(patch("/v1/user/setting/country")
                .with(user(AUTHENTICATED_USER_EMAIL))
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("Korean"))
            .andExpect(jsonPath("$.country").value("North Korea"))
            .andExpect(jsonPath("$.timezone").value("KST +09:00"));

        verify(userService, times(1)).updateUserSettingCountry(AUTHENTICATED_USER_EMAIL, "North Korea");
    }

    @Test
    @DisplayName("PATCH /v1/user/setting/timezone - 인증된 사용자 시간대 설정 업데이트 성공")
    void updateUserSettingTimezone_Success_Authenticated() throws Exception {
        // Given
        UserSettingDto.Request.Timezone request = new Timezone("JST +09:00");
        UserSettingDto.Response response = new Response("Korean", "North Korea", "JST +09:00");

        given(userService.updateUserSettingTimezone(AUTHENTICATED_USER_EMAIL, "JST +09:00")).willReturn(response);

        // When & Then
        mockMvc.perform(patch("/v1/user/setting/timezone")
                .with(user(AUTHENTICATED_USER_EMAIL))
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("Korean"))
            .andExpect(jsonPath("$.country").value("North Korea"))
            .andExpect(jsonPath("$.timezone").value("JST +09:00"));

        verify(userService, times(1)).updateUserSettingTimezone(AUTHENTICATED_USER_EMAIL, "JST +09:00");
    }

    @Test
    @DisplayName("DELETE /v1/user - 인증된 사용자 삭제 성공")
    void deleteUser_Success_Authenticated() throws Exception {

        doNothing().when(userService).removeUser(AUTHENTICATED_USER_EMAIL);

        // When & Then
        mockMvc.perform(delete("/v1/user")
                .with(user(AUTHENTICATED_USER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(userService, times(1)).removeUser(AUTHENTICATED_USER_EMAIL);
    }
}

