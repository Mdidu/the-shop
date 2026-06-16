package com.exemple.the_shop.user.infrastructure.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 16, max = 72) String password,
        @NotBlank String firstName,
        @NotBlank String lastName) {

}
