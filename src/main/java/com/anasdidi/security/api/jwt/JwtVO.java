package com.anasdidi.security.api.jwt;

import java.time.Instant;
import com.anasdidi.security.common.CommonUtils;
import io.vertx.core.json.JsonObject;

class JwtVO {

  final String accessToken;
  final String username;
  final String password;
  final String userId;
  final Instant issuedDate;
  final String refreshToken;

  private JwtVO(String accessToken, String username, String password, String userId,
      Instant issuedDate, String refreshToken) {
    this.accessToken = accessToken;
    this.username = username;
    this.password = password;
    this.userId = userId;
    this.issuedDate = issuedDate;
    this.refreshToken = refreshToken;
  }

  static JwtVO fromJson(JsonObject json) {
    String accessToken = json.getString("accessToken");
    String username = json.getString("username");
    String password = json.getString("password");
    String userId = json.getString("userId");
    Instant issuedDate = CommonUtils.getInstantMongoDate(json, "issuedDate");
    String refreshToken = json.getString("refreshToken");

    return new JwtVO(accessToken, username, password, userId, issuedDate, refreshToken);
  }

  @Override
  public String toString() {
    return new JsonObject()//
        .put("accessToken", (accessToken != null ? "*****" : ""))//
        .put("username", username)//
        .put("password", (password != null ? "*****" : ""))//
        .put("userId", userId)//
        .put("issuedDate", issuedDate)//
        .put("refreshToken", refreshToken)//
        .encodePrettily();
  }
}
