package com.anasdidi.security.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;

public class AppConfig {

  private final static Logger logger = LogManager.getLogger(AppConfig.class);
  private static AppConfig appConfig;

  private final JsonObject config;

  private AppConfig(JsonObject config) {
    this.config = config;
  }

  public static AppConfig create(JsonObject config) {
    appConfig = new AppConfig(config);
    return appConfig;
  }

  public static AppConfig instance() {
    if (appConfig == null) {
      logger.error("[instance] appConfig is null!");
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
        .put("MONGO_CONNECTION_STRING", getMongoConnectionString())
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
    return config.getInteger("APP_PORT");
  }

  public String getAppHost() {
    return config.getString("APP_HOST", "localhost");
  }

  public String getJwtSecret() {
    return config.getString("JWT_SECRET");
  }

  public String getJwtIssuer() {
    return config.getString("JWT_ISSUER");
  }

  public int getJwtExpireInMinutes() {
    return config.getInteger("JWT_EXPIRE_IN_MINUTES");
  }

  public String getMongoHost() {
    return config.getString("MONGO_HOST");
  }

  public int getMongoPort() {
    return config.getInteger("MONGO_PORT");
  }

  public String getMongoUsername() {
    return config.getString("MONGO_USERNAME");
  }

  public String getMongoPassword() {
    return config.getString("MONGO_PASSWORD");
  }

  public String getMongoAuthSource() {
    return config.getString("MONGO_AUTH_SOURCE");
  }

  public String getTestMongoHost() {
    return config.getString("TEST_MONGO_HOST");
  }

  public int getTestMongoPort() {
    return config.getInteger("TEST_MONGO_PORT", -1);
  }

  public String getTestMongoUsename() {
    return config.getString("TEST_MONGO_USERNAME");
  }

  public String getTestMongoPassword() {
    return config.getString("TEST_MONGO_PASSWORD");
  }

  public String getTestMongoAuthSource() {
    return config.getString("TEST_MONGO_AUTH_SOURCE");
  }

  public String getMongoConnectionString() {
    return "mongodb://mongo:mongo@mongo:27017/security?authSource=admin";
  }

  public boolean getGraphiqlIsEnable() {
    return config.getBoolean("GRAPHIQL_IS_ENABLE", false);
  }
}
