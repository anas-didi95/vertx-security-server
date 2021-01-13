package com.anasdidi.security.api.graphql.dto;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import com.anasdidi.security.common.CommonUtils;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class UserDTO {

  private final String id;
  private final String username;
  private final String fullName;
  private final String email;
  private final String lastModifiedBy;
  private final Instant lastModifiedDate;
  private final Long version;
  private final String telegramId;
  private final List<String> permissions;

  private UserDTO(String id, String username, String fullName, String email, String lastModifiedBy,
      Instant lastModifiedDate, Long version, String telegramId, List<String> permissions) {
    this.id = id;
    this.username = username;
    this.fullName = fullName;
    this.email = email;
    this.lastModifiedBy = lastModifiedBy;
    this.lastModifiedDate = lastModifiedDate;
    this.version = version;
    this.telegramId = telegramId;
    this.permissions = permissions;
  }

  public static UserDTO fromJson(JsonObject json) {
    String id = json.getString("id");
    String username = json.getString("username");
    String fullName = json.getString("fullName");
    String email = json.getString("email");
    String lastModifiedBy = json.getString("lastModifiedBy");
    Instant lastModifiedDate = json.getInstant("lastModifiedDate");
    Long version = json.getLong("version");
    String telegramId = json.getString("telegramId");
    List<String> permissions = json.getJsonArray("permissions", new JsonArray()).stream()
        .map(o -> (String) o).collect(Collectors.toList());

    return new UserDTO(id, username, fullName, email, lastModifiedBy, lastModifiedDate, version,
        telegramId, permissions);
  }

  public String getId(DataFetchingEnvironment env) {
    return id;
  }

  public String getUsername(DataFetchingEnvironment env) {
    return username;
  }

  public String getFullName(DataFetchingEnvironment env) {
    return fullName;
  }

  public String getEmail(DataFetchingEnvironment env) {
    return email;
  }

  public String getLastModifiedDate(DataFetchingEnvironment env) {
    String format = env.getArgument("format");

    if (format == null) {
      return lastModifiedDate.toString();
    } else {
      return CommonUtils.getFormattedDateString(lastModifiedDate, format);
    }
  }

  public Long getVersion(DataFetchingEnvironment env) {
    return version;
  }

  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public String getTelegramId(DataFetchingEnvironment env) {
    return telegramId;
  }

  public List<String> getPermissions(DataFetchingEnvironment env) {
    return permissions;
  }

  @Override
  public String toString() {
    return new JsonObject()//
        .put("id", id)//
        .put("username", username)//
        .put("fullName", fullName)//
        .put("email", email)//
        .put("lastModifiedBy", lastModifiedBy)//
        .put("lastModifiedDate", lastModifiedDate)//
        .put("version", version)//
        .put("telegramId", telegramId)//
        .put("permissions", permissions)//
        .encodePrettily();
  }
}
