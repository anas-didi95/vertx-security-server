package com.anasdidi.security.common;

import io.vertx.core.json.JsonObject;

public class ApplicationConfig {

  private static ApplicationConfig config;
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

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " :: " + json.encode();
  }

  public String getAppHost() {
    return json.getString("APP_HOST");
  }

  public int getAppPort() {
    return json.getInteger("APP_PORT");
  }

  public String getMongoConnectionString() {
    return json.getString("MONGO_CONNECTION_STRING");
  }
}
