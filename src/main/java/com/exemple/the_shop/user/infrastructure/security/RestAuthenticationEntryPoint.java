package com.exemple.the_shop.user.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.exemple.the_shop.shared.web.ApiError;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Réponse aux requêtes <b>non authentifiées</b> sur une ressource protégée →
 * 401.
 *
 * <p>
 * Ce point d'entrée est invoqué par la chaîne de filtres Spring Security, donc
 * <b>en amont du {@code DispatcherServlet}</b> : aucun
 * {@code @RestControllerAdvice}
 * ne peut intercepter ce cas. On sérialise donc l'{@link ApiError} à la main
 * dans
 * la réponse, en réutilisant l'{@link ObjectMapper} du contexte (configuré pour
 * les
 * types {@code java.time} comme {@code Instant}).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    /**
     * Le code pourrait être externalisé pour respecter le principe DRY
     * Pourquoi je ne le fais pas ? Au moins temporairement
     * Afin que RestAccessDenied Handler et restAuthenticationEntryPoint se lisent
     * seuls dans un but purement pédagogique
     */
    ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED, "Authentification requise");

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getWriter(), body);
  }
}
