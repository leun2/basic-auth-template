package com.leun.auth.service;

import com.leun.auth.entity.RefreshToken;
import com.leun.auth.repository.RefreshTokenRepository;
import com.leun.auth.util.JwtUtil;
import com.leun.user.entity.User;
import com.leun.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public Map<String, String> refreshAccessToken(String refreshToken) { // 반환 타입 변경
        log.debug("Log:" + "service" + " " + "refreshAccessToken" + " " + refreshToken);
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid Refresh Token.");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with email: " + email));

        RefreshToken storedRefreshToken = refreshTokenRepository.findByUser(user)
            .orElseThrow(() -> new IllegalArgumentException(
                "Refresh Token not found for user: " + email));

        if (!storedRefreshToken.getToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token mismatch.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email); // 새로운 Refresh Token 생성

        storedRefreshToken.updateToken(newRefreshToken); // DB 업데이트
        refreshTokenRepository.save(storedRefreshToken); // 저장

        return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken); // 둘 다 반환
    }

    @Transactional
    public void logout(String refreshToken) {

        log.debug("Log:" + "service" + " " + "logout" + " " + refreshToken);
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid Refresh Token provided for logout.");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with email: " + email));

        refreshTokenRepository.findByUser(user)
            .ifPresent(refreshTokenRepository::delete);
    }
}


