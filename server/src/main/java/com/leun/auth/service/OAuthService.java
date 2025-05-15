package com.leun.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.leun.auth.config.OAuthConfig;
import com.leun.auth.dto.AuthDto;
import com.leun.auth.util.JwtUtil;
import com.leun.user.entity.User;
import com.leun.user.entity.User.ProviderType;
import com.leun.user.entity.User.UserRole;
import com.leun.user.entity.UserProfile;
import com.leun.user.entity.UserSetting;
import com.leun.user.repository.UserProfileRepository;
import com.leun.user.repository.UserRepository;
import com.leun.user.repository.UserSettingRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthConfig oauthConfig;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Transactional
    public AuthDto.Response googleLoginWithAuthCode(String authCode)
        throws Exception {

        if (authCode == null || authCode.isEmpty()) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty.");
        }

        GoogleTokenResponse tokenResponse;
        try {
            tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                new GsonFactory(),
                oauthConfig.getGoogleClientId(),
                oauthConfig.getGoogleClientSecret(),
                authCode,
                oauthConfig.getGoogleRedirectUri()
            ).execute();

        } catch (IOException e) {
            throw new Exception("Failed to exchange auth code for tokens with Google.", e);
        }

        String idTokenString = tokenResponse.getIdToken();
        if (idTokenString == null) {
            throw new Exception("ID token not received from Google token endpoint.");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
            new GsonFactory())
            .setAudience(Collections.singletonList(oauthConfig.getGoogleClientId()))
            .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            throw new Exception("Failed to verify Google ID token.", e);
        }

        if (idToken == null) {
            throw new IllegalArgumentException("Invalid or expired Google ID token.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String imageUrl = (String) payload.get("picture");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User(email, passwordEncoder.encode("password"), ProviderType.GOOGLE,
                UserRole.ROLE_USER);
            userRepository.save(user);

            UserProfile userProfile = new UserProfile(user, name, imageUrl);
            userProfileRepository.save(userProfile);

            UserSetting userSetting = new UserSetting(user, "Korean", "South Korea", "KST +09:00");
            userSettingRepository.save(userSetting);
        } else if (user.getProvider() != ProviderType.GOOGLE) {

            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String token = jwtUtil.generateToken(email);

        return new AuthDto.Response(name, imageUrl, token);
    }

    @Transactional
    public AuthDto.Response naverLoginWithAuthCode(String authCode) throws Exception {

        if (authCode == null || authCode.isEmpty()) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty.");
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", oauthConfig.getNaverClientId());
        params.add("client_secret", oauthConfig.getNaverClientSecret());
        params.add("code", authCode);
        params.add("redirect_uri", oauthConfig.getNaverRedirectUri());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        ResponseEntity<Map> tokenResponse;
        try {
            tokenResponse = restTemplate.exchange(
                oauthConfig.getNaverTokenUri(),
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
        } catch (Exception e) {
            throw new Exception("Failed to exchange auth code for tokens with Naver.", e);
        }

        Map<String, Object> tokenResponseBody = tokenResponse.getBody();
        if (tokenResponseBody == null || tokenResponseBody.get("access_token") == null) {
            throw new Exception("Failed to get access token from Naver token endpoint.");
        }
        String accessToken = (String) tokenResponseBody.get("access_token");

        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> userInfoRequestEntity = new HttpEntity<>(userInfoHeaders);

        ResponseEntity<Map> userInfoResponse;
        try {
            userInfoResponse = restTemplate.exchange(
                oauthConfig.getNaverUserInfoUri(),
                HttpMethod.GET,
                userInfoRequestEntity,
                Map.class
            );
        } catch (Exception e) {
            throw new Exception("Failed to get user info from Naver API.", e);
        }

        Map<String, Object> userInfoResponseBody = userInfoResponse.getBody();
        if (userInfoResponseBody == null || !userInfoResponseBody.containsKey("response")) {
            throw new Exception("Invalid user info response from Naver API.");
        }

        Map<String, Object> responseMap = (Map<String, Object>) userInfoResponseBody.get("response");
        if (responseMap == null) {
            throw new Exception("Naver user info 'response' field is null.");
        }

        String naverUserId = (String) responseMap.get("id");
        String email = (String) responseMap.get("email");
        String name = (String) responseMap.get("name");
        String imageUrl = (String) responseMap.get("profile_image");
        // String nickname = (String) responseMap.get("nickname");

        if (naverUserId == null || email == null) {
            throw new Exception(
                "Required user info (id or email) missing from Naver API response.");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User(email, passwordEncoder.encode("password_placeholder"),
                ProviderType.NAVER, UserRole.ROLE_USER);

            userRepository.save(user);

            UserProfile userProfile = new UserProfile(user, name, imageUrl);
            userProfileRepository.save(userProfile);

            UserSetting userSetting = new UserSetting(user, "Korean", "South Korea", "KST +09:00");
            userSettingRepository.save(userSetting);

        } else if (user.getProvider() != ProviderType.NAVER) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String token = jwtUtil.generateToken(email);

        return new AuthDto.Response(name, imageUrl, token);
    }
}
