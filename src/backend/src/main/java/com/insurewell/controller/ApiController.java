package com.insurewell.controller;

import com.insurewell.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

  @GetMapping
  public ResponseEntity<Map<String, Object>> apiRoot() {
    return ResponseEntity.ok(Map.of(
      "name", "InsureWell API",
      "status", "ok",
      "endpoints", Map.of(
        "health", "/api/health",
        "auth", "/api/auth/me",
        "policies", "/api/policies",
        "claims", "/api/claims"
      )
    ));
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    return ResponseEntity.ok(Map.of(
      "status", "ok",
      "timestamp", OffsetDateTime.now().toString()
    ));
  }

  @GetMapping("/auth/me")
  public ResponseEntity<Map<String, Object>> currentUser(Authentication authentication) {
    AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("username", user.getUsername());
    body.put("role", user.getRole());
    body.put("policyId", user.getPolicyId());
    return ResponseEntity.ok(body);
  }

  @GetMapping("/auth/csrf")
  public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
    return ResponseEntity.ok(Map.of(
      "headerName", csrfToken.getHeaderName(),
      "token", csrfToken.getToken()
    ));
  }
}