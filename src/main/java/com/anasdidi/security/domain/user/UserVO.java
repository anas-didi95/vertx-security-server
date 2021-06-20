package com.anasdidi.security.domain.user;

import io.vertx.core.json.JsonObject;

class UserVO {

  final String id;
  final String username;
  final String password;
  final String fullName;
  final String email;
  final String telegramId;

  private UserVO(String id, String username, String password, String fullName, String email,
      String telegramId) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.fullName = fullName;
    this.email = email;
    this.telegramId = telegramId;
  }

  static UserVO fromJson(JsonObject json) {
    String id = json.getString("id");
    String username = json.getString("username");
    String password = json.getString("password");
    String fullName = json.getString("fullName");
    String email = json.getString("email");
    String telegramId = json.getString("telegramId");

    return new UserVO(id, username, password, fullName, email, telegramId);
  }

  JsonObject toJson() {
    return new JsonObject().put("username", username).put("password", password)
        .put("fullName", fullName).put("email", email).put("telegramId", telegramId);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + toJson().encode();
  }
}
