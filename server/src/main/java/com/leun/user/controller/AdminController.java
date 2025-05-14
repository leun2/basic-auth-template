package com.leun.user.controller;

import com.leun.user.dto.UserProfileDto;
import com.leun.user.dto.UserProfileDto.Response;
import com.leun.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/profile")
    public ResponseEntity<Response> getUserProfile(
        @AuthenticationPrincipal UserDetails userDetails) throws Exception {

        UserProfileDto.Response response =
            userService.getUserProfileByEmail(userDetails.getUsername());

        return ResponseEntity.ok(response);
    }
}
