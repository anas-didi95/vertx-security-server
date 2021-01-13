package com.anasdidi.security.api.graphql.dto;

import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.json.JsonObject;

public class PermissionDTO {

  private final String id;

  private PermissionDTO(String id) {
    this.id = id;
  }

  public static PermissionDTO fromJson(JsonObject json) {
    String id = json.getString("id");

    return new PermissionDTO(id);
  }

  public String getId(DataFetchingEnvironment env) {
    return id;
  }

  @Override
  public String toString() {
    return new JsonObject()//
        .put("id", id)//
        .encodePrettily();
  }
}
