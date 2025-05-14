package com.leun.user.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leun.auth.config.SecurityConfiguration;
import com.leun.auth.service.CustomUserDetailsService;
import com.leun.auth.util.JwtUtil;
import com.leun.user.dto.UserProfileDto;
import com.leun.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfiguration.class)
public class AdminControllerTest {

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

    private final String TEST_USER_EMAIL = "user@example.com";
    private final String TEST_ADMIN_EMAIL = "admin@example.com";

    @Test
    @DisplayName("GET /v1/admin/profile - ROLE_ADMIN로 접근 시 성공 (@WithMockUser 사용)")
    @WithMockUser(username = TEST_ADMIN_EMAIL, roles = {"ADMIN"})
    void getAdminProfile_Success_WithAdminRole_UsingWithMockUser() throws Exception {
        // Given
        UserProfileDto.Response mockProfileResponse = new UserProfileDto.Response(
            TEST_ADMIN_EMAIL, "Admin", "/admin-image.jpg");

        given(userService.getUserProfileByEmail(TEST_ADMIN_EMAIL)).willReturn(
            mockProfileResponse);

        // When & Then
        mockMvc.perform(get("/v1/admin/profile")
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(TEST_ADMIN_EMAIL))
            .andExpect(jsonPath("$.name").value("Admin"))
            .andExpect(jsonPath("$.image").value("/admin-image.jpg"));

        verify(userService, times(1)).getUserProfileByEmail(TEST_ADMIN_EMAIL);
    }

    @Test
    @DisplayName("GET /v1/admin/profile - ROLE_ADMIN로 접근 시 성공 (user().roles() 사용)")
    void getAdminProfile_Success_WithAdminRole_UsingUserDotRoles() throws Exception {
        // Given
        UserProfileDto.Response mockProfileResponse = new UserProfileDto.Response(
            TEST_ADMIN_EMAIL, "Admin", "/admin-image.jpg");

        given(userService.getUserProfileByEmail(TEST_ADMIN_EMAIL)).willReturn(
            mockProfileResponse);

        // When & Then
        mockMvc.perform(get("/v1/admin/profile")
                .with(user(TEST_ADMIN_EMAIL).roles("ADMIN"))
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(TEST_ADMIN_EMAIL))
            .andExpect(jsonPath("$.name").value("Admin"))
            .andExpect(jsonPath("$.image").value("/admin-image.jpg"));

        verify(userService, times(1)).getUserProfileByEmail(TEST_ADMIN_EMAIL);
    }


    @Test
    @DisplayName("GET /v1/admin/profile - ROLE_USER로 접근 시 403 Forbidden")
    void getAdminProfile_Failure_WithUserRole() throws Exception {

        // When & Then
        mockMvc.perform(get("/v1/admin/profile")
                .with(user(TEST_USER_EMAIL).roles("USER"))
                .contentType(APPLICATION_JSON))
            .andExpect(status().isForbidden());

        verify(userService, never()).getUserProfileByEmail(anyString());
    }


    @Test
    @DisplayName("GET /v1/admin/profile - 미인증 시 401 Unauthorized")
    void getAdminProfile_Failure_Unauthenticated() throws Exception {

        // When & Then
        mockMvc.perform(get("/v1/admin/profile")
                .contentType(APPLICATION_JSON))
            .andExpect(status().isUnauthorized());

        verify(userService, never()).getUserProfileByEmail(anyString());
    }
}