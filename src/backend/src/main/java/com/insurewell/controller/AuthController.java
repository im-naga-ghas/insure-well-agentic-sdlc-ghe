package com.insurewell.controller;

import com.insurewell.security.UserProfiles;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @GetMapping("/me")
  public ResponseEntity<Map<String, String>> currentUser(Authentication authentication) {
    String role = authentication.getAuthorities().stream()
      .findFirst()
      .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
      .orElse("POLICYHOLDER");

    return ResponseEntity.ok(Map.of(
      "username", authentication.getName(),
      "role", role,
      "holderName", UserProfiles.holderNameFor(authentication.getName()).orElse(authentication.getName())
    ));
  }
}
