package com.leun.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AuthDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull(message = "Email cannot be null")
        @NotEmpty(message = "Email cannot be empty")
        @Email(message = "Invalid email format")
        private String email;
        @NotNull(message = "Password cannot be null")
        @NotEmpty(message = "Password cannot be empty")
        private String password;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String name;
        private String image;
        private String accessToken;
        private String refreshToken;
    }
}
