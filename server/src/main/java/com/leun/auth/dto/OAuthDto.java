package com.leun.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class OAuthDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleRequest {
        @NotNull(message = "AuthCode cannot be null")
        @NotEmpty(message = "AuthCode cannot be empty")
        private String code;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NaverRequest {
        @NotNull(message = "AuthCode cannot be null")
        @NotEmpty(message = "AuthCode cannot be empty")
        private String code;
    }
}
