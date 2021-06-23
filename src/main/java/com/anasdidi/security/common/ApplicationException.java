package com.anasdidi.security.common;

import java.util.Arrays;
import java.util.List;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import io.vertx.core.json.JsonObject;

public class ApplicationException extends Exception {

  private final String code;
  private final String message;
  private List<String> errorList;

  public ApplicationException(ErrorValue error, List<String> detailList) {
    this.code = error.code;
    this.message = error.message;
    this.errorList = detailList;
  }

  public ApplicationException(ErrorValue error, String detail) {
    this.code = error.code;
    this.message = error.message;
    this.errorList = Arrays.asList(detail);
  }

  @Override
  public String getMessage() {
    return new JsonObject().put("code", code).put("message", message).put("errors", errorList)
        .encode();
  }
}
