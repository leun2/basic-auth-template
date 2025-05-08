package com.leun.auth.dto;

import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    @Getter
    @Setter
    public static class Request {
        private String email;
        private String password;
    }

    @Getter
    @Setter
    public static class Response {
        private String name;
        private String image;
        private String token;

        public Response(String name, String image, String token) {
            this.name = name;
            this.image = image;
            this.token = token;
        }
    }
}
