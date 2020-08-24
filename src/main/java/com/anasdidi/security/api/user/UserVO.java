package com.anasdidi.security.api.user;

import io.vertx.core.json.JsonObject;

class UserVO {

  String id;
  String username;
  String password;
  String fullName;
  String email;
  Long version;

  UserVO() {
  }

  @Override
  public String toString() {
    return new JsonObject()//
        .put("id", id)//
        .put("username", username)//
        .put("password", (password != null ? "***" : ""))//
        .put("fullName", fullName)//
        .put("email", email)//
        .put("version", version)//
        .encodePrettily();
  }
}
