package com.insurewell.dto;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
  private String token;
  private String username;
  private String role;
}
