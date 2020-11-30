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
  final String lastUpdatedBy;
  final Instant lastModifiedDate;
  final Long version;

  private UserVO(String id, String username, String password, String fullName, String email,
      String lastUpdatedBy, Instant lastModifiedDate, Long version) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.fullName = fullName;
    this.email = email;
    this.lastUpdatedBy = lastUpdatedBy;
    this.lastModifiedDate = lastModifiedDate;
    this.version = version;
  }

  static UserVO fromJson(JsonObject json) {
    String id = json.getString("id", json.getString("_id"));
    String username = json.getString("username");
    String password = json.getString("password");
    String fullName = json.getString("fullName");
    String email = json.getString("email");
    String lastUpdatedBy = json.getString("lastUpdatedBy");
    Instant lastModifiedDate = CommonUtils.getInstantMongoDate(json, "lastModifiedDate");
    Long version = json.getLong("version");

    return new UserVO(id, username, password, fullName, email, lastUpdatedBy, lastModifiedDate,
        version);
  }

  static JsonObject toJson(UserVO vo) {
    return new JsonObject()//
        .put("id", vo.id)//
        .put("username", vo.username)//
        .put("password", vo.password)//
        .put("fullName", vo.fullName)//
        .put("email", vo.email)//
        .put("lastUpdatedBy", vo.lastUpdatedBy)//
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
        .put("lastUpdatedBy", lastUpdatedBy)//
        .put("lastModifiedDate", lastModifiedDate)//
        .put("version", version)//
        .encodePrettily();
  }
}
