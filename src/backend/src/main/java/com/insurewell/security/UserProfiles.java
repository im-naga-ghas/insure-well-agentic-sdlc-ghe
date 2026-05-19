package com.insurewell.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class UserProfiles {

  private final Map<String, String> userToHolderName;

  public UserProfiles(
      @Value("${insurewell.security.users.alex.username}") String alexUsername,
      @Value("${insurewell.security.users.alex.holder-name}") String alexHolderName,
      @Value("${insurewell.security.users.maria.username}") String mariaUsername,
      @Value("${insurewell.security.users.maria.holder-name}") String mariaHolderName,
      @Value("${insurewell.security.users.david.username}") String davidUsername,
      @Value("${insurewell.security.users.david.holder-name}") String davidHolderName) {
    Map<String, String> map = new HashMap<>();
    map.put(alexUsername, alexHolderName);
    map.put(mariaUsername, mariaHolderName);
    map.put(davidUsername, davidHolderName);
    this.userToHolderName = Map.copyOf(map);
  }

  public Optional<String> holderNameFor(String username) {
    return Optional.ofNullable(userToHolderName.get(username));
  }
}
