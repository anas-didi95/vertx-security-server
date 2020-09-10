package com.anasdidi.security.api.jwt;

import io.vertx.core.json.JsonObject;

class JwtVO {

  String id;
  String accessToken;
  String username;
  String password;

  JwtVO() {
  }

  @Override
  public String toString() {
    return new JsonObject()//
        .put("id", id)//
        .put("accessToken", (accessToken != null ? "***" : ""))//
        .put("username", username)//
        .put("password", (password != null ? "***" : ""))//
        .encodePrettily();
  }
}
