package com.insurewell.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Converts Entra ID token claims to Spring Security authorities: app roles from the {@code roles}
 * claim become {@code ROLE_*} authorities, delegated permissions from the {@code scp} claim become
 * {@code SCOPE_*} authorities.
 */
public class EntraIdAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    List<String> roles = jwt.getClaimAsStringList("roles");
    if (roles != null) {
      roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    String scopes = jwt.getClaimAsString("scp");
    if (scopes != null) {
      for (String scope : scopes.split(" ")) {
        if (!scope.isBlank()) {
          authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
      }
    }
    return authorities;
  }
}
