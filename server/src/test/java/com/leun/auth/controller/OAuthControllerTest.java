package com.leun.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leun.auth.config.SecurityConfiguration;
import com.leun.auth.dto.AuthDto;
import com.leun.auth.dto.OAuthDto;
import com.leun.auth.dto.OAuthDto.GoogleRequest;
import com.leun.auth.dto.OAuthDto.NaverRequest;
import com.leun.auth.service.CustomUserDetailsService;
import com.leun.auth.service.OAuthService;
import com.leun.auth.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OAuthController.class)
@Import(SecurityConfiguration.class)
public class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private OAuthService oAuthService;

    @Test
    @DisplayName("POST /v1/auth/google/login - 유효한 authCode로 성공 시 200 OK 및 응답 반환")
    void googleLogin_Success_WithValidCode() throws Exception {
        // Given
        OAuthDto.GoogleRequest request = new OAuthDto.GoogleRequest("valid_google_auth_code");
        AuthDto.Response mockResponse = new AuthDto.Response("google user", "/google-image.jpg", "mock_google_access_token", "mock_google_refresh_token");

        given(oAuthService.googleLoginWithAuthCode(anyString())).willReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/v1/auth/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("google user"))
            .andExpect(jsonPath("$.image").value("/google-image.jpg"))
            .andExpect(jsonPath("$.accessToken").value("mock_google_access_token"))
            .andExpect(jsonPath("$.refreshToken").value("mock_google_refresh_token"));

        verify(oAuthService, times(1)).googleLoginWithAuthCode("valid_google_auth_code");
    }

    @Test
    @DisplayName("POST /v1/auth/naver/login - 로그인 성공 시 200 OK 및 토큰 반환")
    void naverLogin_Success_WithValidCode() throws Exception {
        // Given
        OAuthDto.NaverRequest request = new OAuthDto.NaverRequest("valid_naver_auth_code");
        AuthDto.Response mockResponse = new AuthDto.Response("naver user", "/naver-image.jpg", "mock_naver_access_token", "mock_naver_refresh_token");

        given(oAuthService.naverLoginWithAuthCode(anyString())).willReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/v1/auth/naver/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("naver user"))
            .andExpect(jsonPath("$.image").value("/naver-image.jpg"))
            .andExpect(jsonPath("$.accessToken").value("mock_naver_access_token"))
            .andExpect(jsonPath("$.refreshToken").value("mock_naver_refresh_token"));

        verify(oAuthService, times(1)).naverLoginWithAuthCode("valid_naver_auth_code");
    }

    @Test
    @DisplayName("POST /v1/auth/google/login - 요청 본문 유효성 검증 실패 (예: 필드 누락 또는 형식 오류)")
    void googleLogin_Failure_WithInvalidCode() throws Exception {
        // Given
        OAuthDto.GoogleRequest invalidRequest = new GoogleRequest(null);

        // When & Then
        mockMvc.perform(post("/v1/auth/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/auth/naver/login - 요청 본문 유효성 검증 실패 (예: 필드 누락 또는 형식 오류)")
    void naverLogin_Failure_WithInvalidCode() throws Exception {
        // Given
        OAuthDto.NaverRequest invalidRequest = new NaverRequest(null);

        // When & Then
        mockMvc.perform(post("/v1/auth/naver/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
}
