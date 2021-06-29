package com.anasdidi.security.domain.user;

import com.anasdidi.security.common.BaseVO;
import io.vertx.core.json.JsonObject;

class UserVO extends BaseVO {

  final String id;
  final String username;
  final String password;
  final String fullName;
  final String email;
  final String telegramId;
  final Long version;

  private UserVO(String traceId, String id, String username, String password, String fullName,
      String email, String telegramId, Long version) {
    super(traceId);
    this.id = id;
    this.username = username;
    this.password = password;
    this.fullName = fullName;
    this.email = email;
    this.telegramId = telegramId;
    this.version = version;
  }

  static UserVO fromJson(JsonObject json) {
    return fromJson(json, json.getString("id"));
  }

  static UserVO fromJson(JsonObject json, String userId) {
    String traceId = json.getString("traceId");
    String id = userId;
    String username = json.getString("username");
    String password = json.getString("password");
    String fullName = json.getString("fullName");
    String email = json.getString("email");
    String telegramId = json.getString("telegramId");
    Long version = json.getLong("version");

    return new UserVO(traceId, id, username, password, fullName, email, telegramId, version);
  }

  JsonObject toJson() {
    return new JsonObject().put("username", username).put("password", password)
        .put("fullName", fullName).put("email", email).put("telegramId", telegramId)
        .put("version", version);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + toJson().encode();
  }
}
