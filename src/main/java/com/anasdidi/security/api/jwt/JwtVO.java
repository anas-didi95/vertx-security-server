package com.anasdidi.security.api.jwt;

import io.vertx.core.json.JsonObject;

class JwtVO {

  final String id;
  final String accessToken;
  final String username;
  final String password;
  final String userId;
  final String salt;

  private JwtVO(String id, String accessToken, String username, String password, String userId,
      String salt) {
    this.id = id;
    this.accessToken = accessToken;
    this.username = username;
    this.password = password;
    this.userId = userId;
    this.salt = salt;
  }

  static JwtVO fromJson(JsonObject json) {
    String id = json.getString("_id", json.getString("id"));
    String accessToken = json.getString("accessToken");
    String username = json.getString("username");
    String password = json.getString("password");
    String userId = json.getString("userId");
    String salt = json.getString("salt");

    return new JwtVO(id, accessToken, username, password, userId, salt);
  }

  @Override
  public String toString() {
    return new JsonObject()//
        .put("id", id)//
        .put("accessToken", (accessToken != null ? "*****" : ""))//
        .put("username", username)//
        .put("password", (password != null ? "*****" : ""))//
        .put("userId", userId)//
        .put("salt", salt)//
        .encodePrettily();
  }
}
