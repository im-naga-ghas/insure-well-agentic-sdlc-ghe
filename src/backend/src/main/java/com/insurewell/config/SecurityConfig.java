package com.insurewell.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Autowired
  private JwtAuthFilter jwtAuthFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    // Restrict origins to localhost for development; override jwt.cors.allowed-origins in production
    config.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "http://localhost:8080"
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // CSRF disabled intentionally: this is a stateless REST API using JWT Bearer tokens.
        // No session cookies are used, so CSRF attacks do not apply.
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/health", "/api/claims/health").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            // Read access: both roles
            .requestMatchers(HttpMethod.GET, "/api/policies/**", "/api/claims/**").authenticated()
            // Policyholder: submit claims
            .requestMatchers(HttpMethod.POST, "/api/claims").hasAnyRole("POLICYHOLDER", "ADMIN")
            // Admin-only: create/modify/delete policies, update claim status, delete claims
            .requestMatchers(HttpMethod.POST, "/api/policies").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/policies/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/policies/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/claims/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/claims/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .headers(headers -> headers.frameOptions(fo -> fo.disable()))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
