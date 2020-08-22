package com.anasdidi.security.common;

import java.util.UUID;

public class CommonUtils {

  public static String generateId() {
    return UUID.randomUUID().toString().replace("-", "").toUpperCase();
  }
}
