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
import com.leun.user.dto.UserSettingDto;
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
import java.util.NoSuchElementException;
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
    public AuthDto.Response googleLoginWithAuthCode(String authCode) throws Exception { // 인증 코드를 인자로 받습니다.

        if (authCode == null || authCode.isEmpty()) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty.");
        }

        // 1. 인증 코드를 사용하여 Google로부터 토큰(ID 토큰, Access 토큰)을 교환합니다.
        //    이 과정에서 Google API 라이브러리가 필요하며, Client Secret이 사용됩니다.
        GoogleTokenResponse tokenResponse;
        try {
            tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(), // HTTP 전송 계층
                new GsonFactory(), // JSON 파싱 라이브러리 (Gson 사용 가정)
                oauthConfig.getGoogleClientId(), // application.yml 또는 GoogleConfig에서 주입받은 Client ID
                oauthConfig.getGoogleClientSecret(), // application.yml 또는 GoogleConfig에서 주입받은 Client Secret (!!서버에서만 사용!!)
                authCode, // 클라이언트로부터 받은 인증 코드
                oauthConfig.getGoogleRedirectUri() // application.yml 또는 GoogleConfig에서 주입받은 Redirect URI
            ).execute(); // Google API에 서버 간 요청 전송

        } catch (IOException e) {
            // Google API 통신 오류 발생 시 처리
            throw new Exception("Failed to exchange auth code for tokens with Google.", e);
        }

        // 2. Google로부터 받은 응답에서 ID 토큰 문자열을 추출합니다.
        String idTokenString = tokenResponse.getIdToken();
        if (idTokenString == null) {
            // ID 토큰이 응답에 포함되지 않은 경우 (scope 설정 확인 필요)
            throw new Exception("ID token not received from Google token endpoint.");
        }

        // 3. 추출한 ID 토큰 문자열을 검증합니다. (기존 GoogleIdTokenVerifier 재활용)
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(oauthConfig.getGoogleClientId())) // Client ID로 Audience 설정
            .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString); // ID 토큰 문자열 검증
        } catch (GeneralSecurityException | IOException e) {
            // 토큰 검증 라이브러리 오류 발생 시 처리
            throw new Exception("Failed to verify Google ID token.", e);
        }


        if (idToken == null) {
            // 토큰 검증 실패 (위변조되었거나, 만료되었거나, Audience가 다르거나 등)
            throw new IllegalArgumentException("Invalid or expired Google ID token.");
        }

        // 4. ID 토큰 검증 성공 후 페이로드에서 사용자 정보 추출 (기존 로직과 유사)
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String imageUrl = (String) payload.get("picture");
        // 필요한 경우 locale 등 추가 정보 추출 가능

        // 5. 백엔드 사용자 처리 (기존 사용자 찾기 또는 새로 생성) - 기존 로직 재활용 가능
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Google OAuth로 처음 로그인하는 경우, 사용자 등록
            user = new User(email, passwordEncoder.encode("password"), ProviderType.GOOGLE, UserRole.ROLE_USER);
            userRepository.save(user);

            UserProfile userProfile = new UserProfile(user, name, imageUrl); // 이미지 URL 저장
            userProfileRepository.save(userProfile);

            UserSetting userSetting = new UserSetting(user, "Korean", "South Korea", "KST +09:00"); // 기본 설정
            userSettingRepository.save(userSetting);
        } else if (user.getProvider() != ProviderType.GOOGLE) {
            // 이미 다른 방식으로 가입된 이메일인 경우
            throw new IllegalArgumentException("이미 다른 방식으로 가입된 이메일입니다.");
        }

        // 6. 백엔드 JWT 토큰 생성 (기존 로직 재활용)
        String token = jwtUtil.generateToken(user.getEmail());

        // 7. 사용자 프로필 및 설정 조회 (기존 로직 재활용)
        UserProfile profile = userProfileRepository.findByUser(user)
            .orElseThrow(() -> new NoSuchElementException("User Profile Not Found"));
        UserSetting setting = userSettingRepository.findByUser(user)
            .orElseThrow(() -> new NoSuchElementException("User Setting Not Found"));

        // 8. 응답 DTO 생성 및 반환 (기존 로직 재활용)
        UserSettingDto.Response settingDto =
            new UserSettingDto.Response(setting.getLanguage(), setting.getCountry(), setting.getTimezone());

        // 백엔드 응답 DTO는 백엔드가 생성한 JWT와 사용자 정보를 포함해야 합니다.
        // LoginDto.Response 구조 확인 필요
        return new AuthDto.Response(profile.getName(), profile.getImage(), token); // 이름, 이미지, 토큰 반환
    }

    @Transactional // 트랜잭션 관리
    public AuthDto.Response naverLoginWithAuthCode(String authCode) throws Exception {

        if (authCode == null || authCode.isEmpty()) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty.");
        }

        // 1. Naver 토큰 교환 요청 (Authorization Code -> Access Token)
        //    Google API 라이브러리는 Naver와 호환되지 않으므로 직접 HTTP 요청을 보냅니다.
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", oauthConfig.getNaverClientId());
        params.add("client_secret", oauthConfig.getNaverClientSecret());
        params.add("code", authCode);
        params.add("redirect_uri", oauthConfig.getNaverRedirectUri());
        // state는 클라이언트에서 보냈다면 여기서도 검증해야 합니다.

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // 폼 데이터 형식

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        ResponseEntity<Map> tokenResponse; // Naver 응답은 Map으로 받아서 처리
        try {
            tokenResponse = restTemplate.exchange(
                oauthConfig.getNaverTokenUri(), // application.yml에서 읽어온 Naver 토큰 엔드포인트
                HttpMethod.POST,
                requestEntity,
                Map.class // 응답 본문을 Map으로 받음
            );
        } catch (Exception e) {
            // Naver API 통신 오류 발생 시 처리
            throw new Exception("Failed to exchange auth code for tokens with Naver.", e);
        }

        // 2. 토큰 응답 파싱 및 Access Token 추출
        Map<String, Object> tokenResponseBody = tokenResponse.getBody();
        if (tokenResponseBody == null || tokenResponseBody.get("access_token") == null) {
            throw new Exception("Failed to get access token from Naver token endpoint.");
        }
        String accessToken = (String) tokenResponseBody.get("access_token");
        // String refreshToken = (String) tokenResponseBody.get("refresh_token"); // 리프레시 토큰 필요 시 저장

        // 3. Naver 사용자 정보 조회 요청 (Access Token 사용)
        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.set("Authorization", "Bearer " + accessToken); // Access Token을 Bearer 스키마로 포함

        HttpEntity<Void> userInfoRequestEntity = new HttpEntity<>(userInfoHeaders);

        ResponseEntity<Map> userInfoResponse; // Naver 사용자 정보 응답도 Map으로 받아서 처리
        try {
            userInfoResponse = restTemplate.exchange(
                oauthConfig.getNaverUserInfoUri(), // application.yml에서 읽어온 Naver 사용자 정보 엔드포인트
                HttpMethod.GET,
                userInfoRequestEntity,
                Map.class
            );
        } catch (Exception e) {
            throw new Exception("Failed to get user info from Naver API.", e);
        }

        // 4. 사용자 정보 응답 파싱 및 필수 정보 추출
        Map<String, Object> userInfoResponseBody = userInfoResponse.getBody();
        if (userInfoResponseBody == null || !userInfoResponseBody.containsKey("response")) {
            throw new Exception("Invalid user info response from Naver API.");
        }

        Map<String, Object> responseMap = (Map<String, Object>) userInfoResponseBody.get("response");
        if (responseMap == null) {
            throw new Exception("Naver user info 'response' field is null.");
        }

        // Naver 사용자 정보 필드명 확인 (Naver 개발자 센터 참고)
        String naverUserId = (String) responseMap.get("id"); // Naver 고유 사용자 ID (필수)
        String email = (String) responseMap.get("email"); // 이메일 (필수)
        String name = (String) responseMap.get("name"); // 이름
        String imageUrl = (String) responseMap.get("profile_image"); // 프로필 사진 URL
        // String nickname = (String) responseMap.get("nickname"); // 닉네임 등 추가 정보

        if (naverUserId == null || email == null) {
            throw new Exception("Required user info (id or email) missing from Naver API response.");
        }


        // 5. 백엔드 사용자 처리 (기존 사용자 찾기 또는 새로 생성) - ProviderType.NAVER 사용
        // Google과 마찬가지로 provider + providerUserId로 찾는 것을 고려
        User user = userRepository.findByEmail(email).orElse(null); // 이메일로 찾는 예시

        if (user == null) {
            // Naver OAuth로 처음 로그인하는 경우, 사용자 등록
            // providerUserId를 User 엔티티에 추가했다고 가정
            user = new User(email, passwordEncoder.encode("password_placeholder"), ProviderType.NAVER, UserRole.ROLE_USER);
            // user.setProviderUserId(naverUserId); // providerUserId 저장 (User 엔티티 수정 필요)
            userRepository.save(user);

            UserProfile userProfile = new UserProfile(user, name, imageUrl);
            userProfileRepository.save(userProfile);

            UserSetting userSetting = new UserSetting(user, "Korean", "South Korea", "KST +09:00"); // 기본 설정
            userSettingRepository.save(userSetting);

        } else if (user.getProvider() != ProviderType.NAVER) {
            // 이미 다른 방식으로 가입된 이메일인 경우 (다른 Provider 또는 일반 로그인)
            throw new IllegalArgumentException("이미 다른 방식으로 가입된 이메일입니다.");
            // TODO: 필요하다면 여기서 계정 연동 로직 구현
        }
        // TODO: Naver 재로그인 시 프로필 정보(이름, 사진 등) 업데이트 로직 추가 고려

        // 6. 백엔드 JWT 토큰 생성 (기존 로직 재활용)
        String token = jwtUtil.generateToken(user.getEmail()); // JWT subject를 email로 사용

        // 7. 사용자 프로필 조회 (기존 로직 재활용)
        UserProfile profile = userProfileRepository.findByUser(user)
            .orElseThrow(() -> new NoSuchElementException("User Profile Not Found"));
        // UserSetting은 필요에 따라 조회하여 응답 DTO에 포함

        // 8. 응답 DTO 생성 및 반환 (기존 로직 재활용)
        return new AuthDto.Response(profile.getName(), profile.getImage(), token);
    }
}
