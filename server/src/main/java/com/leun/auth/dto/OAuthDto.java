package com.leun.auth.dto;

import lombok.Getter;
import lombok.Setter;

public class OAuthDto {

    @Getter
    @Setter
    public static class GoogleRequest {
        private String code;
    }

    @Getter
    @Setter
    public static class NaverRequest {
        private String code;
        private String status;
    }
}
