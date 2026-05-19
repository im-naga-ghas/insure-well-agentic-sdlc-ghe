package com.insurewell.security;

import java.util.Map;
import java.util.Optional;

public final class UserProfiles {

  private static final Map<String, String> USER_TO_HOLDER_NAME = Map.of(
    "alex", "Alex Johnson",
    "maria", "Maria Garcia",
    "david", "David Chen"
  );

  private UserProfiles() {
  }

  public static Optional<String> holderNameFor(String username) {
    return Optional.ofNullable(USER_TO_HOLDER_NAME.get(username));
  }
}
