package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.BaseVO;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.auth.User;

class AuthVO extends BaseVO {

  final String username;
  final String password;
  final String userId;
  final Boolean hasPermissionsKey;

  private AuthVO(String traceId, String username, String password, String userId,
      boolean hasPermissionsKey) {
    super(traceId);
    this.username = username;
    this.password = password;
    this.userId = userId;
    this.hasPermissionsKey = hasPermissionsKey;
  }

  static final AuthVO fromJson(JsonObject json) {
    return fromJson(json, User.create(new JsonObject()));
  }

  static final AuthVO fromJson(JsonObject json, User user) {
    ApplicationConfig config = ApplicationConfig.instance();
    String traceId = json.getString("traceId");
    String username = json.getString("username");
    String password = json.getString("password");
    String userId = user.principal().getString("sub");
    Boolean hasPermissionsKey = user.principal().containsKey(config.getJwtPermissionKey());

    return new AuthVO(traceId, username, password, userId, hasPermissionsKey);
  }

  final JsonObject toJson() {
    return new JsonObject().put("username", username).put("password", password)
        .put("userId", userId).put("hasPermissionsKey", hasPermissionsKey);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + toJson().encode();
  }
}
