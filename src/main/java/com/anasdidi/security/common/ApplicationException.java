package com.anasdidi.security.common;

import java.time.Instant;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ApplicationException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = -2056237507702938045L;
  private String message;
  private String requestId;
  private JsonArray errorList;
  private Instant instant;

  public ApplicationException(String message, String requestId, JsonArray errorList) {
    super();
    this.message = message;
    this.requestId = requestId;
    this.errorList = errorList;
    this.instant = Instant.now();
  }

  public ApplicationException(String message, String requestId, Throwable e) {
    super(e);
    this.message = message;
    this.requestId = requestId;
    this.errorList = new JsonArray().add(e.getMessage());
    this.instant = Instant.now();
  }

  public ApplicationException(String message, String requestId, String error) {
    super();
    this.message = message;
    this.requestId = requestId;
    this.errorList = new JsonArray().add(error);
    this.instant = Instant.now();
  }

  public String getMessage() {
    return new JsonObject()//
        .put("status", new JsonObject()//
            .put("isSuccess", false)//
            .put("message", message))//
        .put("data", new JsonObject()//
            .put("requestId", requestId)//
            .put("errorList", errorList)//
            .put("instant", instant))
        .encode();
  }
}
