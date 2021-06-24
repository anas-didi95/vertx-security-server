package com.anasdidi.security.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ApplicationConfig {

  private static ApplicationConfig config;
  private static String KEY_APP_HOST = "APP_HOST";
  private static String KEY_APP_PORT = "APP_PORT";
  private static String KEY_MONGO_CONNECTION_STRING = "MONGO_CONNECTION_STRING";
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
    return new JsonArray().add(KEY_APP_HOST).add(KEY_APP_PORT).add(KEY_MONGO_CONNECTION_STRING);
  }

  @Override
  public String toString() {
    JsonObject copy = json.copy();
    copy.put(KEY_MONGO_CONNECTION_STRING, String.format("[len: %d]",
        getMongoConnectionString() != null ? getMongoConnectionString().length() : -1));
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
}
