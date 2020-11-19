package com.anasdidi.security.common;

import io.vertx.core.json.JsonObject;

public class AppConfig {

  private static AppConfig appConfig;
  private final JsonObject cfg;

  private AppConfig(JsonObject cfg) {
    this.cfg = cfg;
  }

  public static AppConfig create(JsonObject cfg) {
    appConfig = new AppConfig(cfg);
    return appConfig;
  }

  public static AppConfig instance() throws Exception {
    if (appConfig == null) {
      throw new Exception("AppConfig is null!");
    }
    return appConfig;
  }

  @Override
  public String toString() {
    return new JsonObject()//
        .put("APP_PORT", getAppPort())//
        .put("APP_HOST", getAppHost())//
        .put("JWT_SECRET", getJwtSecret())//
        .put("JWT_ISSUER", getJwtIssuer())//
        .put("JWT_EXPIRE_IN_MINUTES", getJwtExpireInMinutes())//
        .put("MONGO_CONFIG", getMongoConfig())//
        .put("GRAPHIQL_IS_ENABLE", getGraphiqlIsEnable())//
        .encodePrettily();
  }

  public int getAppPort() {
    return cfg.getInteger("APP_PORT");
  }

  public String getAppHost() {
    return cfg.getString("APP_HOST", "localhost");
  }

  public String getJwtSecret() {
    return cfg.getString("JWT_SECRET");
  }

  public String getJwtIssuer() {
    return cfg.getString("JWT_ISSUER");
  }

  public int getJwtExpireInMinutes() {
    return cfg.getInteger("JWT_EXPIRE_IN_MINUTES");
  }

  public JsonObject getMongoConfig() {
    String connectionString = cfg.getString("MONGO_CONNECTION_STRING");

    if (connectionString == null) {
      return null;
    }

    return new JsonObject().put("connection_string", connectionString);
  }

  public boolean getGraphiqlIsEnable() {
    return cfg.getBoolean("GRAPHIQL_IS_ENABLE", false);
  }
}
