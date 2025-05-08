package com.leun.auth.service;

import com.leun.auth.dto.AuthDto;
import com.leun.auth.dto.AuthDto.Response;
import com.leun.auth.util.JwtUtil;
import com.leun.user.entity.User;
import com.leun.user.entity.UserProfile;
import com.leun.user.repository.UserProfileRepository;
import com.leun.user.service.UserService;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final JwtUtil jwtUtil;

    public AuthDto.Response login(AuthDto.Request request) throws Exception {
        User user = userService.findUserByEmail(request.getEmail());

        String token = jwtUtil.generateToken(user.getEmail());
        UserProfile profile = userProfileRepository.findByUser(user)
            .orElseThrow(() -> new NoSuchElementException("User Does Not Exist"));

        return new Response(profile.getName(), profile.getImage(), token);
    }
}
