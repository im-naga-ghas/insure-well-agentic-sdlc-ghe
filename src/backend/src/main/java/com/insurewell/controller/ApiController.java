package com.insurewell.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

  @GetMapping
  public ResponseEntity<Map<String, Object>> apiRoot() {
    return ResponseEntity.ok(Map.of(
      "name", "InsureWell API",
      "status", "ok",
      "endpoints", Map.of(
        "health", "/api/health",
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
}