package com.anasdidi.security.common;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import graphql.execution.ExecutionId;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.User;

public class CommonUtils {

  public static String generateUUID() {
    return generateUUID(null);
  }

  public static String generateUUID(ExecutionId executionId) {
    String uuid = (executionId != null ? executionId.toString() : UUID.randomUUID().toString());
    return uuid.replace("-", "").toUpperCase();
  }

  public static String getFormattedDateString(Instant instant, String format) {
    Date date = Date.from(instant);
    SimpleDateFormat sdf = new SimpleDateFormat(format);

    return sdf.format(date);
  }

  public static Instant getInstantMongoDate(JsonObject json, String key) {
    JsonObject dateJson = json.getJsonObject(key);

    if (dateJson == null || dateJson.isEmpty()) {
      return null;
    } else {
      return dateJson.getInstant("$date");
    }
  }

  public static String getUserIdFromToken(User user) {
    return user.principal().getString("sub");
  }
}
