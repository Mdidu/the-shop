package com.exemple.the_shop.user.application;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exemple.the_shop.user.domain.EmailAlreadyUsedException;
import com.exemple.the_shop.user.domain.model.RefreshToken;
import com.exemple.the_shop.user.domain.model.User;
import com.exemple.the_shop.user.domain.port.out.UserRepository;
import com.exemple.the_shop.user.infrastructure.security.JwtUtil;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;
  private final JwtUtil jwtUtil;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      RefreshTokenService refreshTokenService, JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.refreshTokenService = refreshTokenService;
    this.jwtUtil = jwtUtil;
  }

  @Transactional
  public AuthResponse signup(String email, String password, String firstName, String lastName) {
    if (userRepository.findByEmail(email).isPresent()) {
      throw new EmailAlreadyUsedException("Email already in use: " + email);
    }
    User user = User.create(email, passwordEncoder.encode(password), firstName, lastName);
    userRepository.save(user);
    RefreshToken refreshToken = refreshTokenService.issue(user.getId());
    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
    return new AuthResponse(accessToken, refreshToken.getToken());
  }

  @Transactional
  public AuthResponse signin(String email, String password) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    RefreshToken refreshToken = refreshTokenService.issue(user.getId());
    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
    return new AuthResponse(accessToken, refreshToken.getToken());
  }

  @Transactional
  public void signout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }
}
