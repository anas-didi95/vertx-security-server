package com.anasdidi.security.common;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class CommonUtils {

  public static String generateId() {
    return UUID.randomUUID().toString().replace("-", "").toUpperCase();
  }

  public static String getFormattedDateString(Instant instant, String format) {
    Date date = Date.from(instant);
    SimpleDateFormat sdf = new SimpleDateFormat(format);

    return sdf.format(date);
  }
}
