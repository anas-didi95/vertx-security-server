package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseVO;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.auth.User;

class AuthVO extends BaseVO {

  final String userId;
  final String username;
  final String password;

  private AuthVO(String traceId, String userId, String username, String password) {
    super(traceId);
    this.userId = userId;
    this.username = username;
    this.password = password;
  }

  static final AuthVO fromJson(JsonObject json) {
    return fromJson(json, User.create(new JsonObject()));
  }

  static final AuthVO fromJson(JsonObject json, User user) {
    String traceId = json.getString("traceId");
    String userId = user.principal().getString("sub");
    String username = json.getString("username");
    String password = json.getString("password");

    return new AuthVO(traceId, userId, username, password);
  }

  final JsonObject toJson() {
    return new JsonObject().put("username", username).put("password", password);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + toJson().encode();
  }
}
