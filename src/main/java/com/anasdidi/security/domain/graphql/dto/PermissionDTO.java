package com.anasdidi.security.domain.graphql.dto;

import io.vertx.core.json.JsonObject;

public class PermissionDTO {

  private final String id;

  private PermissionDTO(String id) {
    this.id = id;
  }

  public static PermissionDTO fromJson(JsonObject json) {
    String id = json.getString("id", json.getString("_id"));
    return new PermissionDTO(id);
  }

  @Override
  public String toString() {
    return "PermissionDTO [id=" + id + "]";
  }

  public String getId() {
    return id;
  }
}
