package com.insurewell.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes the identity of the caller as resolved from the Microsoft Entra ID access token.
 */
@RestController
@RequestMapping("/api/me")
public class UserController {

  @GetMapping
  public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
    Map<String, Object> user = new LinkedHashMap<>();
    if (jwt == null) {
      user.put("authenticated", false);
      return ResponseEntity.ok(user);
    }
    user.put("authenticated", true);
    user.put("id", jwt.getClaimAsString("oid"));
    user.put("name", jwt.getClaimAsString("name"));
    user.put("username", jwt.getClaimAsString("preferred_username"));
    user.put("tenantId", jwt.getClaimAsString("tid"));
    List<String> roles = jwt.getClaimAsStringList("roles");
    user.put("roles", roles != null ? roles : List.of());
    String scopes = jwt.getClaimAsString("scp");
    user.put("scopes", scopes != null ? List.of(scopes.split(" ")) : List.of());
    return ResponseEntity.ok(user);
  }
}
