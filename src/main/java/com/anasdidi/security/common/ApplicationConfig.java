package com.anasdidi.security.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ApplicationConfig {

  private static ApplicationConfig config;
  private static String KEY_APP_HOST = "APP_HOST";
  private static String KEY_APP_PORT = "APP_PORT";
  private static String KEY_MONGO_CONNECTION_STRING = "MONGO_CONNECTION_STRING";
  private static String KEY_JWT_SECRET = "JWT_SECRET";
  private static String KEY_JWT_ISSUER = "JWT_ISSUER";
  private static String KEY_JWT_PERMISSIONS_KEY = "JWT_PERMISSIONS_KEY";
  private static String KEY_JWT_ACCESS_TOKEN_EXPIRE_IN_MINUTES =
      "JWT_ACCESS_TOKEN_EXPIRE_IN_MINUTES";
  private static String KEY_JWT_REFRESH_TOKEN_EXPIRE_IN_MINUTES =
      "JWT_REFRESH_TOKEN_EXPIRE_IN_MINUTES";
  private static String KEY_GRAPHIQL_ENABLE = "GRAPHIQL_ENABLE";
  private static String KEY_CORS_ORIGINS = "CORS_ORIGINS";

  private final JsonObject json;

  private ApplicationConfig(JsonObject json) {
    this.json = json;
  }

  public static ApplicationConfig create(JsonObject json) {
    config = new ApplicationConfig(json);
    return config;
  }

  public static ApplicationConfig instance() {
    if (config == null) {
      System.err.println("ERROR! ApplicationConfig is null!");
    }
    return config;
  }

  public static JsonArray getKeyList() {
    return new JsonArray().add(KEY_APP_HOST).add(KEY_APP_PORT).add(KEY_MONGO_CONNECTION_STRING)
        .add(KEY_JWT_SECRET).add(KEY_JWT_ISSUER).add(KEY_JWT_PERMISSIONS_KEY)
        .add(KEY_JWT_ACCESS_TOKEN_EXPIRE_IN_MINUTES).add(KEY_JWT_REFRESH_TOKEN_EXPIRE_IN_MINUTES)
        .add(KEY_GRAPHIQL_ENABLE).add(KEY_CORS_ORIGINS);
  }

  @Override
  public String toString() {
    JsonObject copy = json.copy();
    copy.put(KEY_MONGO_CONNECTION_STRING, ApplicationUtils.hideValue(getMongoConnectionString()));
    copy.put(KEY_JWT_SECRET, ApplicationUtils.hideValue(getJwtSecret()));
    copy.put(KEY_JWT_ISSUER, ApplicationUtils.hideValue(getJwtIssuer()));
    return this.getClass().getSimpleName() + copy.encode();
  }

  public String getAppHost() {
    return json.getString(KEY_APP_HOST);
  }

  public int getAppPort() {
    return json.getInteger(KEY_APP_PORT);
  }

  public String getMongoConnectionString() {
    return json.getString(KEY_MONGO_CONNECTION_STRING);
  }

  public String getJwtSecret() {
    return json.getString(KEY_JWT_SECRET);
  }

  public String getJwtIssuer() {
    return json.getString(KEY_JWT_ISSUER);
  }

  public String getJwtPermissionsKey() {
    return json.getString(KEY_JWT_PERMISSIONS_KEY);
  }

  public Integer getJwtAccessTokenExpireInMinutes() {
    return json.getInteger(KEY_JWT_ACCESS_TOKEN_EXPIRE_IN_MINUTES);
  }

  public Integer getJwtRefreshTokenExpireInMinutes() {
    return json.getInteger(KEY_JWT_REFRESH_TOKEN_EXPIRE_IN_MINUTES);
  }

  public Boolean getGraphiqlEnable() {
    return json.getBoolean(KEY_GRAPHIQL_ENABLE);
  }

  public String getCorsOrigins() {
    return json.getString(KEY_CORS_ORIGINS);
  }
}
