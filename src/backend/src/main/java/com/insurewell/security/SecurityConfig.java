package com.insurewell.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Web security for the InsureWell API.
 *
 * <p>The API is an OAuth 2.0 resource server: callers present a Microsoft Entra ID
 * (Microsoft identity platform v2.0) access token as a {@code Bearer} token. Tokens are validated
 * against the tenant JWKS endpoint, and the issuer and audience claims are checked.</p>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(EntraIdProperties.class)
public class SecurityConfig {

  /** Endpoints that stay anonymous so that clients can discover and monitor the API. */
  static final String[] PUBLIC_ENDPOINTS = { "/api", "/api/health" };

  private final EntraIdProperties properties;

  public SecurityConfig(EntraIdProperties properties) {
    this.properties = properties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(Customizer.withDefaults())
      // Stateless API: callers authenticate with a token in the Authorization header rather than
      // with cookies or a session, so there is no CSRF attack surface to protect.
      .csrf(csrf -> csrf.disable())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(requests -> {
        requests.requestMatchers(new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name())).permitAll();
        for (String endpoint : PUBLIC_ENDPOINTS) {
          requests.requestMatchers(new AntPathRequestMatcher(endpoint)).permitAll();
        }
        if (properties.isEnabled()) {
          requests.anyRequest().authenticated();
        } else {
          requests.anyRequest().permitAll();
        }
      });

    if (properties.isEnabled()) {
      http.oauth2ResourceServer(oauth2 ->
        oauth2.jwt(jwt -> jwt
          .decoder(jwtDecoder())
          .jwtAuthenticationConverter(jwtAuthenticationConverter())));
    }

    return http.build();
  }

  /**
   * Decoder for Microsoft identity platform v2.0 access tokens. Only created when authentication is
   * enabled, so the application still starts without an Entra ID registration.
   */
  private JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
    decoder.setJwtValidator(tokenValidator(properties));
    return decoder;
  }

  static OAuth2TokenValidator<Jwt> tokenValidator(EntraIdProperties properties) {
    return new DelegatingOAuth2TokenValidator<>(
      JwtValidators.createDefaultWithIssuer(properties.getIssuerUri()),
      new AudienceValidator(properties.getAcceptedAudiences()));
  }

  /** Maps Entra ID app roles ({@code roles}) and delegated scopes ({@code scp}) to authorities. */
  static JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new EntraIdAuthoritiesConverter());
    converter.setPrincipalClaimName("preferred_username");
    return converter;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(properties.getAllowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
