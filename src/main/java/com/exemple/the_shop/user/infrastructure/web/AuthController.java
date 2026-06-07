package com.exemple.the_shop.user.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exemple.the_shop.user.application.AuthResponse;
import com.exemple.the_shop.user.application.AuthService;
import com.exemple.the_shop.user.application.RefreshTokenService;
import com.exemple.the_shop.user.domain.model.RefreshToken;
import com.exemple.the_shop.user.infrastructure.security.JwtUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("auth")
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;
  private final JwtUtil jwtUtil;

  public AuthController(AuthService authService, RefreshTokenService refreshTokenService, JwtUtil jwtUtil) {
    this.authService = authService;
    this.refreshTokenService = refreshTokenService;
    this.jwtUtil = jwtUtil;
  }

  @PostMapping("/signin")
  public ResponseEntity<AuthResponse> signin(@Valid @RequestBody SigninRequest signinRequest) {
    return ResponseEntity.ok()
        .body(authService.signin(signinRequest.email(), signinRequest.password()));
  }

  @PostMapping("/signup")
  public ResponseEntity<AuthResponse> signup(
      @Valid @RequestBody SignupRequest signupRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(signupRequest.email(),
        signupRequest.password(), signupRequest.firstName(), signupRequest.lastName()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody String refreshToken) {
    authService.signout(refreshToken);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshToken(@RequestBody String refreshToken) {
    RefreshToken newRefreshToken = refreshTokenService.rotate(refreshToken);
    String accessToken = jwtUtil.generateAccessToken(jwtUtil.extractUserId(refreshToken),
        jwtUtil.extractRole(refreshToken));
    return ResponseEntity.ok().body(new AuthResponse(accessToken, newRefreshToken.getToken()));
  }
}
