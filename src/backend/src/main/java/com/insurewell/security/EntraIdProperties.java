package com.insurewell.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Microsoft Entra ID (formerly Azure AD) configuration.
 *
 * <p>Bound from {@code insurewell.security.*} properties. When {@link #getMode()} is
 * {@code auto} (default), authentication is enabled as soon as a tenant id and a client id are
 * configured, so local development without an Entra ID registration keeps working.</p>
 */
@ConfigurationProperties(prefix = "insurewell.security")
public class EntraIdProperties {

  public enum Mode {
    /** Enable authentication only when tenant id and client id are configured. */
    AUTO,
    /** Always enable authentication. */
    ON,
    /** Never enable authentication (local development only). */
    OFF
  }

  /** Whether authentication is enabled. */
  private Mode mode = Mode.AUTO;

  /** Entra ID directory (tenant) id. */
  private String tenantId = "";

  /** Application (client) id of the API registration. Used as the expected token audience. */
  private String clientId = "";

  /** Additional accepted audiences, e.g. {@code api://<client-id>}. */
  private List<String> audiences = new ArrayList<>();

  /** Cloud instance authority, e.g. {@code https://login.microsoftonline.com}. */
  private String instance = "https://login.microsoftonline.com";

  /** Browser origins allowed to call the API. */
  private List<String> allowedOrigins = new ArrayList<>(List.of("*"));

  public boolean isEnabled() {
    return switch (mode) {
      case ON -> true;
      case OFF -> false;
      case AUTO -> hasText(tenantId) && hasText(clientId);
    };
  }

  /** Issuer of Microsoft identity platform v2.0 access tokens for this tenant. */
  public String getIssuerUri() {
    return trimTrailingSlash(instance) + "/" + tenantId + "/v2.0";
  }

  /** JSON Web Key Set endpoint used to validate access token signatures. */
  public String getJwkSetUri() {
    return trimTrailingSlash(instance) + "/" + tenantId + "/discovery/v2.0/keys";
  }

  /** All audiences accepted in the {@code aud} claim of an access token. */
  public List<String> getAcceptedAudiences() {
    List<String> accepted = new ArrayList<>();
    if (hasText(clientId)) {
      accepted.add(clientId);
      accepted.add("api://" + clientId);
    }
    audiences.stream().filter(EntraIdProperties::hasText).forEach(accepted::add);
    return accepted;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String trimTrailingSlash(String value) {
    return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public List<String> getAudiences() {
    return audiences;
  }

  public void setAudiences(List<String> audiences) {
    this.audiences = audiences;
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  public List<String> getAllowedOrigins() {
    return allowedOrigins;
  }

  public void setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }
}
