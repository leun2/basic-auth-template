package com.leun.auth.controller;


import com.leun.auth.dto.AuthDto.Response;
import com.leun.auth.dto.OAuthDto;
import com.leun.auth.service.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    private final OAuthService OAuthService;

    @PostMapping("/google/login")
    public ResponseEntity<Response> googleLogin(@RequestBody OAuthDto.GoogleRequest request) throws Exception {
        String authCode = request.getCode();
        if (authCode == null || authCode.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Response response = OAuthService.googleLoginWithAuthCode(authCode);
        log.debug("Log: /google/login login user with google");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/naver/login")
    public ResponseEntity<Response> naverLogin(@RequestBody OAuthDto.NaverRequest request) throws Exception {
        String authCode = request.getCode();

        if (authCode == null || authCode.isEmpty()) {
            return ResponseEntity.badRequest().body(new Response(null, null, null, null));
        }

        Response response = OAuthService.naverLoginWithAuthCode(authCode);
        log.debug("Log: /google/login login user with naver");
        return ResponseEntity.ok(response);
    }
}
