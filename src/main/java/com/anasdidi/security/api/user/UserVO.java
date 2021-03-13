package com.anasdidi.security.api.user;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import com.anasdidi.security.common.CommonUtils;
import io.vertx.core.json.JsonArray;
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
  final String telegramId;
  final List<String> permissions;

  final String oldPassword;
  final String newPassword;

  private UserVO(String id, String username, String password, String fullName, String email,
      String lastModifiedBy, Instant lastModifiedDate, Long version, String telegramId,
      List<String> permissions, String oldPassword, String newPassword) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.fullName = fullName;
    this.email = email;
    this.lastModifiedBy = lastModifiedBy;
    this.lastModifiedDate = lastModifiedDate;
    this.version = version;
    this.telegramId = telegramId;
    this.permissions = permissions;

    this.oldPassword = oldPassword;
    this.newPassword = newPassword;
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
    String telegramId = json.getString("telegramId");
    List<String> permissions = json.getJsonArray("permissions", new JsonArray()).stream()
        .map(o -> (String) o).collect(Collectors.toList());

    String oldPassword = json.getString("oldPassword");
    String newPassword = json.getString("newPassword");

    return new UserVO(id, username, password, fullName, email, lastModifiedBy, lastModifiedDate,
        version, telegramId, permissions, oldPassword, newPassword);
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
        .put("version", vo.version)//
        .put("telegramId", vo.telegramId)//
        .put("permissions", vo.permissions)//
        .put("oldPassword", vo.oldPassword)//
        .put("newPassword", vo.newPassword);
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
        .put("teleramId", telegramId)//
        .put("permissions", permissions)//
        .put("oldPassword", oldPassword)//
        .put("newPassword", newPassword)//
        .encodePrettily();
  }
}
