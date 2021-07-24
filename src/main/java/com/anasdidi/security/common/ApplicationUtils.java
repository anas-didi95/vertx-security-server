package com.anasdidi.security.common;

import java.time.Instant;
import java.util.UUID;
import io.vertx.core.json.JsonObject;

public class ApplicationUtils {

  public static final String getFormattedUUID() {
    return getFormattedUUID(UUID.randomUUID().toString());
  }

  public static final String getFormattedUUID(String uuid) {
    return uuid.replaceAll("-", "").toUpperCase();
  }

  public static final JsonObject setRecordDate() {
    return new JsonObject().put("$date", Instant.now());
  }

  public static final Instant getRecordDate(JsonObject json, String field) {
    return json.getJsonObject(field, new JsonObject()).getInstant("$date");
  }

  public static final String hideValue(String value) {
    return String.format("[len: %d]", value != null ? value.length() : -1);
  }
}
