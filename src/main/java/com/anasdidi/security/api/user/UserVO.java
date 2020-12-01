package com.anasdidi.security.api.user;

import java.time.Instant;
import com.anasdidi.security.common.CommonUtils;
import io.vertx.core.json.JsonObject;

class UserVO {

  final String id;
  final String username;
  final String password;
  final String fullName;
  final String email;
  final String lastModifiedBy;
  final Instant lastModifiedDate;
  final Long version;

  private UserVO(String id, String username, String password, String fullName, String email,
      String lastModifiedBy, Instant lastModifiedDate, Long version) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.fullName = fullName;
    this.email = email;
    this.lastModifiedBy = lastModifiedBy;
    this.lastModifiedDate = lastModifiedDate;
    this.version = version;
  }

  static UserVO fromJson(JsonObject json) {
    String id = json.getString("id", json.getString("_id"));
    String username = json.getString("username");
    String password = json.getString("password");
    String fullName = json.getString("fullName");
    String email = json.getString("email");
    String lastModifiedBy = json.getString("lastModifiedBy");
    Instant lastModifiedDate = CommonUtils.getInstantMongoDate(json, "lastModifiedDate");
    Long version = json.getLong("version");

    return new UserVO(id, username, password, fullName, email, lastModifiedBy, lastModifiedDate,
        version);
  }

  static JsonObject toJson(UserVO vo) {
    return new JsonObject()//
        .put("id", vo.id)//
        .put("username", vo.username)//
        .put("password", vo.password)//
        .put("fullName", vo.fullName)//
        .put("email", vo.email)//
        .put("lastModifiedBy", vo.lastModifiedBy)//
        .put("lastModifiedDate", vo.lastModifiedDate)//
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
        .put("lastModifiedBy", lastModifiedBy)//
        .put("lastModifiedDate", lastModifiedDate)//
        .put("version", version)//
        .encodePrettily();
  }
}
