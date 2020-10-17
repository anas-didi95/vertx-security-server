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

  public int getAppPort() {
    return config.getInteger("APP_PORT");
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
    return config.getInteger("TEST_MONGO_PORT");
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
}
