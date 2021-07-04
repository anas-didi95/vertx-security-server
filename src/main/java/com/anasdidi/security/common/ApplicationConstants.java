package com.anasdidi.security.common;

import java.util.HashMap;
import java.util.Map;

public class ApplicationConstants {

  public static final String CONTEXT_PATH = "/security";
  public static final Map<String, String> HEADERS;

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

  public enum EventMongo {
    MONGO_CREATE, MONGO_UPDATE, MONGO_DELETE;
  }

  public enum ErrorValue {
    REQUEST_BODY_EMPTY("E001", "Request body is empty!"), //
    VALIDATION("E002", "Validation error!"), //
    USER_CREATE("E101", "Create user failed!"), //
    USER_UPDATE("E102", "Update user failed!"), USER_DELETE("E103", "Delete user failed!");

    public final String code;
    public final String message;

    private ErrorValue(String code, String message) {
      this.code = code;
      this.message = message;
    }
  }

  public enum CollectionRecord {
    USER("users");

    public final String name;

    private CollectionRecord(String name) {
      this.name = name;
    }
  }

  public enum HttpStatus {
    OK(200), CREATED(201), BAD_REQUEST(400);

    public final int code;

    private HttpStatus(int code) {
      this.code = code;
    }
  }
}
