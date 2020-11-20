package com.anasdidi.security.api.user;

import java.time.Instant;
import io.vertx.core.json.JsonObject;

class UserVO {

  final String id;
  final String username;
  final String password;
  final String fullName;
  final String email;
  final Instant createDate;
  final Instant updateDate;
  final Long version;

  UserVO(String id, String username, String password, String fullName, String email,
      Instant createDate, Instant updateDate, Long version) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.fullName = fullName;
    this.email = email;
    this.createDate = createDate;
    this.updateDate = updateDate;
    this.version = version;
  }

  static UserVO fromJson(JsonObject json) {
    String id = json.getString("id", json.getString("_id"));
    String username = json.getString("username");
    String password = json.getString("password");
    String fullName = json.getString("fullName");
    String email = json.getString("email");
    Instant createDate = json.getInstant("createDate");
    Instant updateDate = json.getInstant("updateDate");
    Long version = json.getLong("version");

    return new UserVO(id, username, password, fullName, email, createDate, updateDate, version);
  }

  static JsonObject toJson(UserVO vo) {
    return new JsonObject()//
        .put("id", vo.id)//
        .put("username", vo.username)//
        .put("password", vo.password)//
        .put("fullName", vo.fullName)//
        .put("email", vo.email)//
        .put("createDate", vo.createDate)//
        .put("updateDate", vo.updateDate)//
        .put("version", vo.version);
  }

  @Override
  public String toString() {
    return new JsonObject()//
        .put("id", id)//
        .put("username", username)//
        .put("password", (password != null ? "*****" : ""))//
        .put("fullName", fullName)//
        .put("email", email)//
        .put("createDate", createDate)//
        .put("updateDate", updateDate)//
        .put("version", version)//
        .encodePrettily();
  }
}
