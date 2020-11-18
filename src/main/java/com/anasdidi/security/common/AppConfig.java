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
        .put("MONGO_HOST", getMongoHost())//
        .put("MONGO_PORT", getMongoPort())//
        .put("MONGO_USERNAME", getMongoUsername())//
        .put("MONGO_PASSWORD", getMongoPassword())//
        .put("MONGO_AUTH_SOURCE", getMongoAuthSource())//
        .put("TEST_MONGO_HOST", getTestMongoHost())//
        .put("TEST_MONGO_PORT", getTestMongoPort())//
        .put("TEST_MONGO_USERNAME", getTestMongoUsename())//
        .put("TEST_MONGO_PASSWORD", getTestMongoPassword())//
        .put("TEST_MONGO_AUTH_SOURCE", getTestMongoAuthSource())//
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

  public String getMongoHost() {
    return cfg.getString("MONGO_HOST");
  }

  public int getMongoPort() {
    return cfg.getInteger("MONGO_PORT");
  }

  public String getMongoUsername() {
    return cfg.getString("MONGO_USERNAME");
  }

  public String getMongoPassword() {
    return cfg.getString("MONGO_PASSWORD");
  }

  public String getMongoAuthSource() {
    return cfg.getString("MONGO_AUTH_SOURCE");
  }

  public String getTestMongoHost() {
    return cfg.getString("TEST_MONGO_HOST");
  }

  public int getTestMongoPort() {
    return cfg.getInteger("TEST_MONGO_PORT", -1);
  }

  public String getTestMongoUsename() {
    return cfg.getString("TEST_MONGO_USERNAME");
  }

  public String getTestMongoPassword() {
    return cfg.getString("TEST_MONGO_PASSWORD");
  }

  public String getTestMongoAuthSource() {
    return cfg.getString("TEST_MONGO_AUTH_SOURCE");
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
