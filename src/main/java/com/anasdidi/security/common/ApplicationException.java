package com.anasdidi.security.common;

import java.util.Arrays;
import java.util.List;
import io.vertx.core.json.JsonObject;

public class ApplicationException extends Exception {

  private final String code;
  private final String message;
  private List<String> errorList;

  public ApplicationException(String code, String message, String error) {
    this.code = code;
    this.message = message;
    this.errorList = Arrays.asList(error);
  }

  @Override
  public String getMessage() {
    return new JsonObject().put("code", code).put("message", message).put("errors", errorList)
        .encode();
  }
}
