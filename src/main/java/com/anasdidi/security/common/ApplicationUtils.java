package com.anasdidi.security.common;

import java.time.Instant;
import java.util.UUID;
import io.vertx.core.json.JsonObject;

public class ApplicationUtils {

  public static final String getFormattedUUID() {
    return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
  }

  public static final JsonObject setRecordDate() {
    return new JsonObject().put("$date", Instant.now());
  }

  public static final Instant getRecordDate(JsonObject json, String field) {
    return json.getJsonObject(field, new JsonObject()).getInstant("$date");
  }
}
