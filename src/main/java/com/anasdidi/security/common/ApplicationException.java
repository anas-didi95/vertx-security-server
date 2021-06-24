package com.anasdidi.security.common;

import java.util.Arrays;
import java.util.List;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import io.vertx.core.json.JsonObject;

public class ApplicationException extends Exception {

  private final String code;
  private final String message;
  private final String traceId;
  private List<String> errorList;

  public ApplicationException(ErrorValue error, String traceId, List<String> detailList) {
    this.code = error.code;
    this.message = error.message;
    this.traceId = traceId;
    this.errorList = detailList;
  }

  public ApplicationException(ErrorValue error, String traceId, String detail) {
    this.code = error.code;
    this.message = error.message;
    this.traceId = traceId;
    this.errorList = Arrays.asList(detail);
  }

  @Override
  public String getMessage() {
    return new JsonObject().put("code", code).put("message", message).put("traceId", traceId)
        .put("errors", errorList).encode();
  }
}
