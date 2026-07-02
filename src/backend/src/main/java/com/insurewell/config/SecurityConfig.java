package com.insurewell.config;

import com.insurewell.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final String allowedOrigins;

  public SecurityConfig(@Value("${app.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
      .csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .ignoringRequestMatchers(
          AntPathRequestMatcher.antMatcher(HttpMethod.OPTIONS, "/**"),
          AntPathRequestMatcher.antMatcher("/h2-console/**")
        ))
      .cors(Customizer.withDefaults())
      .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
        .requestMatchers(
          AntPathRequestMatcher.antMatcher("/api"),
          AntPathRequestMatcher.antMatcher("/api/health"),
          AntPathRequestMatcher.antMatcher("/h2-console/**")
        ).permitAll()
        .anyRequest().authenticated())
      .httpBasic(Customizer.withDefaults())
      .build();
  }

  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
    Map<String, AuthenticatedUser> users = Map.of(
      "admin", new AuthenticatedUser("admin", passwordEncoder.encode("admin123"), "ADMIN", null),
      "alex", new AuthenticatedUser("alex", passwordEncoder.encode("policy123"), "POLICYHOLDER", "POL-2024-001"),
      "maria", new AuthenticatedUser("maria", passwordEncoder.encode("policy123"), "POLICYHOLDER", "POL-2024-002"),
      "david", new AuthenticatedUser("david", passwordEncoder.encode("policy123"), "POLICYHOLDER", "POL-2023-009")
    );
    return username -> {
      AuthenticatedUser user = users.get(username);
      if (user == null) {
        throw new UsernameNotFoundException("Unknown user: " + username);
      }
      return user;
    };
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(parseAllowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
    configuration.setExposedHeaders(List.of("WWW-Authenticate"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private List<String> parseAllowedOrigins() {
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
      .map(String::trim)
      .filter(origin -> !origin.isEmpty())
      .toList();

    if (origins.isEmpty()) {
      throw new IllegalStateException("app.cors.allowed-origins must contain at least one origin");
    }

    return origins;
  }
}
