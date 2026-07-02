package com.insurewell.controller;

import com.insurewell.config.JwtUtil;
import com.insurewell.dto.AuthRequest;
import com.insurewell.dto.AuthResponse;
import com.insurewell.model.AppUser;
import com.insurewell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth Controller
 * Provides login endpoint that returns a JWT token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private UserRepository userRepository;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody AuthRequest request) {
    if (request.getUsername() == null || request.getUsername().trim().isEmpty()
        || request.getPassword() == null || request.getPassword().trim().isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Username and password are required"));
    }
    try {
      Authentication auth = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
      );
      AppUser user = userRepository.findByUsername(request.getUsername())
          .orElseThrow(() -> new RuntimeException("User not found"));
      String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
      return ResponseEntity.ok(AuthResponse.builder()
          .token(token)
          .username(user.getUsername())
          .role(user.getRole())
          .build());
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Invalid username or password"));
    }
  }
}
