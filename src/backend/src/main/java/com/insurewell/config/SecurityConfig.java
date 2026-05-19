package com.insurewell.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

  @Value("${insurewell.cors.allowed-origin:http://localhost:3000}")
  private String allowedOrigin;

  @Value("${insurewell.security.users.admin.username}")
  private String adminUsername;

  @Value("${insurewell.security.users.admin.password}")
  private String adminPassword;

  @Value("${insurewell.security.users.alex.username}")
  private String alexUsername;

  @Value("${insurewell.security.users.alex.password}")
  private String alexPassword;

  @Value("${insurewell.security.users.maria.username}")
  private String mariaUsername;

  @Value("${insurewell.security.users.maria.password}")
  private String mariaPassword;

  @Value("${insurewell.security.users.david.username}")
  private String davidUsername;

  @Value("${insurewell.security.users.david.password}")
  private String davidPassword;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.ignoringRequestMatchers(
        new AntPathRequestMatcher("/api/**"),
        new AntPathRequestMatcher("/h2-console/**")
      ))
      .cors(Customizer.withDefaults())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(new AntPathRequestMatcher("/**", "OPTIONS")).permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/health")).permitAll()
        .anyRequest().authenticated()
      )
      .httpBasic(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
    return new InMemoryUserDetailsManager(
      User.withUsername(adminUsername)
        .password(passwordEncoder.encode(adminPassword))
        .roles("ADMIN")
        .build(),
      User.withUsername(alexUsername)
        .password(passwordEncoder.encode(alexPassword))
        .roles("POLICYHOLDER")
        .build(),
      User.withUsername(mariaUsername)
        .password(passwordEncoder.encode(mariaPassword))
        .roles("POLICYHOLDER")
        .build(),
      User.withUsername(davidUsername)
        .password(passwordEncoder.encode(davidPassword))
        .roles("POLICYHOLDER")
        .build()
    );
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(allowedOrigin));
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
