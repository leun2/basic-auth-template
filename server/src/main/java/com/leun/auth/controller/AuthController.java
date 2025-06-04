package com.leun.auth.controller;

import com.leun.auth.dto.AuthDto;
import com.leun.auth.service.AuthService;
import com.leun.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDto.Request request) throws Exception {
        log.debug("Log: /login login user");
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid email or password"));
        }

        AuthDto.Response response = authService.login(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        log.debug("Log:" + "/refresh-token" + " " + request.get("refreshToken") + " " + request);
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Refresh Token is required."));
        }

        try {
            Map<String, String> tokens = refreshTokenService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Refresh Token is required."));
        }

        try {
            refreshTokenService.logout(refreshToken);
            return ResponseEntity.ok(Map.of("message", "Logout successful."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Logout failed: " + e.getMessage()));
        }
    }
}
