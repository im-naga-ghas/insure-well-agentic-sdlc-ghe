package com.insurewell.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser implements UserDetails {

  private final String username;
  private final String password;
  private final String role;
  private final String policyId;

  public AuthenticatedUser(String username, String password, String role, String policyId) {
    this.username = username;
    this.password = password;
    this.role = role;
    this.policyId = policyId;
  }

  public boolean isAdmin() {
    return "ADMIN".equals(role);
  }

  public String getRole() {
    return role.toLowerCase();
  }

  public String getPolicyId() {
    return policyId;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
