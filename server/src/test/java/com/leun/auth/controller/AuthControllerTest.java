package com.leun.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leun.auth.config.SecurityConfiguration;
import com.leun.auth.dto.AuthDto;
import com.leun.auth.service.AuthService;
import com.leun.auth.service.CustomUserDetailsService;
import com.leun.auth.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /v1/auth/login - 로그인 성공 시 200 OK 및 토큰 반환")
    void login_Success() throws Exception {
        // Given
        AuthDto.Request loginRequest = new AuthDto.Request("test@example.com", "password123");
        AuthDto.Response loginResponse = new AuthDto.Response("test", "/example", "mock_jwt_token_123");

        given(authService.login(any(AuthDto.Request.class))).willReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("test"))
            .andExpect(jsonPath("$.image").value("/example"))
            .andExpect(jsonPath("$.token").value("mock_jwt_token_123"));
    }

    @Test
    @DisplayName("POST /v1/auth/login - 로그인 실패 (잘못된 자격 증명) 시 401 Unauthorized 반환")
    void login_Failure_InvalidCredentials() throws Exception {
        // Given
        AuthDto.Request loginRequest = new AuthDto.Request("wrong@example.com", "wrongpassword");

        doThrow(new BadCredentialsException("Invalid email or password"))
            .when(authenticationManager)
            .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /v1/auth/login - 요청 본문 유효성 검증 실패 (예: 필드 누락 또는 형식 오류)")
    void login_Failure_InvalidRequestBody() throws Exception {
        // Given
        AuthDto.Request invalidRequest = new AuthDto.Request(null, "password123");

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
}