package com.leun.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public class UserProfileDto {
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String email;
        private String name;
        private String image;

        public Response(String name, String image) {
            this.name = name;
            this.image = image;
        }
    }

    public static class Request {

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Name {
            private String name;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Image {
            @NotNull(message = "Profile image file is required")
            private MultipartFile image;
        }
    }
}
