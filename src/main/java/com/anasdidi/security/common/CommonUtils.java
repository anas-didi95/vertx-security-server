package com.anasdidi.security.common;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import graphql.execution.ExecutionId;

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
}
