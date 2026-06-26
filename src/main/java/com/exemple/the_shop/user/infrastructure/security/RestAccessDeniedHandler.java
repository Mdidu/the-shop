package com.exemple.the_shop.user.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.exemple.the_shop.shared.web.ApiError;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Réponse aux requêtes <b>authentifiées mais sans le rôle requis</b> → 403.
 *
 * <p>
 * Même contrainte que {@link RestAuthenticationEntryPoint} : le refus survient
 * dans la chaîne de filtres, hors de portée des {@code @RestControllerAdvice}.
 * On écrit donc l'{@link ApiError} directement dans la réponse.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    /**
     * Le code pourrait être externalisé pour respecter le principe DRY
     * Pourquoi je ne le fais pas ? Au moins temporairement
     * Afin que RestAccessDenied Handler et restAuthenticationEntryPoint se lisent
     * seuls dans un but purement pédagogique
     */

    ApiError body = ApiError.of(HttpStatus.FORBIDDEN, "Accès refusé");

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getWriter(), body);
  }
}
