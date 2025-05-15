package com.leun.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class UserDto {
    @Getter
    @Setter
    public static class Request {

        @Email
        @NotNull(message = "Email cannot be null")
        @NotEmpty(message = "Email cannot be empty")
        private String email;
        @NotNull(message = "Password cannot be null")
        @NotEmpty(message = "Password cannot be empty")
        private String password;
        @NotNull(message = "Name cannot be null")
        @NotEmpty(message = "Name cannot be empty")
        private String name;

        public Request(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }
}
