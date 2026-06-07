package com.exemple.the_shop.user.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SigninRequest(@NotBlank @Email String email, @NotBlank String password) {
};
