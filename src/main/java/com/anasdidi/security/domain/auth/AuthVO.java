package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationUtils;
import com.anasdidi.security.common.BaseVO;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.auth.User;

class AuthVO extends BaseVO {

  final String username;
  final String password;
  final String subject;
  final Boolean hasPermissionsKey;
  final String accessToken;
  final String refreshToken;
  final String tokenType;

  private AuthVO(String traceId, String username, String password, String subject,
      boolean hasPermissionsKey, String accessToken, String refreshToken, String tokenType) {
    super(traceId);
    this.username = username;
    this.password = password;
    this.subject = subject;
    this.hasPermissionsKey = hasPermissionsKey;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.tokenType = tokenType;
  }

  static final AuthVO fromJson(JsonObject json) {
    return fromJson(json, User.create(new JsonObject()));
  }

  static final AuthVO fromJson(JsonObject json, User user) {
    ApplicationConfig config = ApplicationConfig.instance();
    String traceId = json.getString("traceId");
    String username = json.getString("username");
    String password = json.getString("password");
    String subject = user.principal().getString("sub");
    Boolean hasPermissionsKey = user.principal().containsKey(config.getJwtPermissionsKey());
    String accessToken = json.getString("accessToken");
    String refreshToken = json.getString("refreshToken");
    String tokenType = user.principal().getString("typ");

    return new AuthVO(traceId, username, password, subject, hasPermissionsKey, accessToken,
        refreshToken, tokenType);
  }

  final JsonObject toJson() {
    return new JsonObject().put("username", username).put("password", password)
        .put("subject", subject).put("hasPermissionsKey", hasPermissionsKey)
        .put("accessToken", ApplicationUtils.hideValue(accessToken))
        .put("refreshToken", ApplicationUtils.hideValue("refreshToken"))
        .put("tokenType", tokenType);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + toJson().encode();
  }
}
