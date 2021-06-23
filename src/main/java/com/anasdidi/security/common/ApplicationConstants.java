package com.anasdidi.security.common;

import java.util.HashMap;
import java.util.Map;

public class ApplicationConstants {

  public static Map<String, String> HEADERS;

  static {
    HEADERS = new HashMap<>();
    HEADERS.put("Content-Type", "application/json");
    HEADERS.put("Cache-Control", "no-store, no-cache");
    HEADERS.put("X-Content-Type-Options", "nosniff");
    HEADERS.put("Strict-Transport-Security", "max-age=" + 15768000);
    HEADERS.put("X-Download-Options", "noopen");
    HEADERS.put("X-XSS-Protection", "1; mode=block");
    HEADERS.put("X-FRAME-OPTIONS", "DENY");
  }

  public enum Event {
    MONGO_CREATE("mongo-create");

    public final String address;

    Event(String address) {
      this.address = address;
    }
  }

  public enum ErrorValue {
    REQUEST_BODY_EMPTY("E001", "Request body is empty!"), VALIDATION("E002", "Validation error!");

    public final String code;
    public final String message;

    ErrorValue(String code, String message) {
      this.code = code;
      this.message = message;
    }
  }
}
