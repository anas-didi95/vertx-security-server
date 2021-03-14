package com.anasdidi.security.common;

import java.time.Instant;
import io.vertx.core.json.JsonObject;

public final class MongoUtils {

  public static JsonObject setDate(Instant instant) {
    return new JsonObject().put("$date", instant);
  }

  public static JsonObject setUpdateDocument(JsonObject json) {
    return new JsonObject().put("$set", json);
  }

  public static Instant getDate(JsonObject json, String key) {
    JsonObject dateJson = json.getJsonObject(key);

    if (dateJson == null || dateJson.isEmpty()) {
      return null;
    } else {
      return dateJson.getInstant("$date");
    }
  }
}
