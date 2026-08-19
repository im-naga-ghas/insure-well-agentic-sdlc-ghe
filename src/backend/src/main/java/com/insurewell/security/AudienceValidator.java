package com.insurewell.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validates that an access token was issued for this API, as recommended by the Microsoft identity
 * platform token validation guidance.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

  private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
    "invalid_token", "The required audience is missing", null);

  private final List<String> acceptedAudiences;

  public AudienceValidator(List<String> acceptedAudiences) {
    this.acceptedAudiences = List.copyOf(acceptedAudiences);
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    if (acceptedAudiences.isEmpty()) {
      return OAuth2TokenValidatorResult.success();
    }
    boolean matches = token.getAudience() != null
      && token.getAudience().stream().anyMatch(acceptedAudiences::contains);
    return matches ? OAuth2TokenValidatorResult.success()
      : OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
  }
}
