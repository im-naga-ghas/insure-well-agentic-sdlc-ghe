package com.insurewell.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntraIdSecurityUnitTest {

  private static Jwt jwtWithAudience(String... audience) {
    return Jwt.withTokenValue("token")
      .header("alg", "RS256")
      .audience(List.of(audience))
      .claim("sub", "user")
      .issuedAt(Instant.now())
      .expiresAt(Instant.now().plusSeconds(300))
      .build();
  }

  @Test
  void audienceValidatorAcceptsMatchingAudience() {
    AudienceValidator validator = new AudienceValidator(List.of("client-id", "api://client-id"));
    assertFalse(validator.validate(jwtWithAudience("api://client-id")).hasErrors());
  }

  @Test
  void audienceValidatorRejectsForeignAudience() {
    AudienceValidator validator = new AudienceValidator(List.of("client-id"));
    assertTrue(validator.validate(jwtWithAudience("another-api")).hasErrors());
  }

  @Test
  void propertiesBuildMicrosoftIdentityPlatformEndpoints() {
    EntraIdProperties properties = new EntraIdProperties();
    properties.setTenantId("tenant");
    properties.setClientId("client");

    assertTrue(properties.isEnabled());
    assertEquals("https://login.microsoftonline.com/tenant/v2.0", properties.getIssuerUri());
    assertEquals("https://login.microsoftonline.com/tenant/discovery/v2.0/keys",
      properties.getJwkSetUri());
    assertEquals(List.of("client", "api://client"), properties.getAcceptedAudiences());
  }

  @Test
  void autoModeDisablesAuthenticationWhenTenantIsMissing() {
    assertFalse(new EntraIdProperties().isEnabled());
  }

  @Test
  void authoritiesConverterMapsRolesAndScopes() {
    Jwt jwt = Jwt.withTokenValue("token")
      .header("alg", "RS256")
      .claims(claims -> claims.putAll(Map.of(
        "roles", List.of("Claims.Approve"),
        "scp", "Policy.Read Claims.Write")))
      .build();

    List<String> authorities = new EntraIdAuthoritiesConverter().convert(jwt).stream()
      .map(authority -> authority.getAuthority())
      .toList();

    assertEquals(List.of("ROLE_Claims.Approve", "SCOPE_Policy.Read", "SCOPE_Claims.Write"),
      authorities);
  }
}
