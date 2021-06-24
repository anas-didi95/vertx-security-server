package com.anasdidi.security.common;

import java.util.UUID;

public class ApplicationUtils {

  public static String getFormattedUUID() {
    return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
  }
}
