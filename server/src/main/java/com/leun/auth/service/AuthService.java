package com.leun.auth.service;

import com.leun.auth.dto.AuthDto;
import com.leun.auth.entity.RefreshToken;
import com.leun.auth.repository.RefreshTokenRepository;
import com.leun.auth.util.JwtUtil;
import com.leun.user.entity.User;
import com.leun.user.entity.UserProfile;
import com.leun.user.repository.UserProfileRepository;
import com.leun.user.service.UserService;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthDto.Response login(AuthDto.Request request) throws Exception {
        User user = userService.findUserByEmail(request.getEmail());

        UserProfile profile = userProfileRepository.findByUser(user)
            .orElseThrow(() -> new NoSuchElementException("User Does Not Exist"));

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        refreshTokenRepository.findByUser(user)
            .ifPresentOrElse(
                rt -> rt.updateToken(refreshToken),
                () -> refreshTokenRepository.save(new RefreshToken(user, refreshToken))
            );

        return new AuthDto.Response(profile.getName(), profile.getImage(), accessToken, refreshToken);
    }
}
