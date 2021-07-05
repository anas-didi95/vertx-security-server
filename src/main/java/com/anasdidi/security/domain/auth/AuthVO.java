package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseVO;
import io.vertx.core.json.JsonObject;

class AuthVO extends BaseVO {

  final String username;
  final String password;

  private AuthVO(String traceId, String username, String password) {
    super(traceId);
    this.username = username;
    this.password = password;
  }

  static final AuthVO fromJson(JsonObject json) {
    String traceId = json.getString("traceId");
    String username = json.getString("username");
    String password = json.getString("password");

    return new AuthVO(traceId, username, password);
  }

  final JsonObject toJson() {
    return new JsonObject().put("username", username).put("password", password);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + toJson().encode();
  }
}
