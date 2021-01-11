package com.anasdidi.security.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

public abstract class CommonController {

  private final Logger logger = LogManager.getLogger(CommonController.class);

  protected void sendResponse(String requestId, Single<JsonObject> subscriber,
      RoutingContext routingContext, int statusCode, String message) {
    final String TAG = "sendResponse";
    long timeTaken = System.currentTimeMillis() - (long) routingContext.get("startTime");

    subscriber.subscribe(data -> {
      boolean isDataSensitive = data.containsKey("accessToken");
      String responseBody = new JsonObject()//
          .put("status", new JsonObject()//
              .put("isSuccess", true)//
              .put("message", message))//
          .put("data", data)//
          .encode();

      logger.info("[{}:{}] onSuccess : timeTaken={}ms, statusCode={}, responseBody={}", TAG,
          requestId, timeTaken, statusCode,
          (!isDataSensitive ? responseBody : "{{content hidden}}"));

      routingContext.response().headers().addAll(CommonConstants.HEADERS);
      routingContext.response()//
          .setStatusCode(statusCode)//
          .end(responseBody);
    }, e -> {
      String responseBody = "";
      int errorStatusCode = CommonConstants.STATUS_CODE_BAD_REQUEST;

      if (e.getSuppressed().length > 0) {
        for (Throwable t : e.getSuppressed()) {
          if (t instanceof ApplicationException) {
            ApplicationException ex = (ApplicationException) t;

            responseBody = ex.getMessage();
            errorStatusCode = ex.getErrorStatusCode();
          }
        }
      } else if (e instanceof ApplicationException) {
        ApplicationException ex = (ApplicationException) e;

        responseBody = ex.getMessage();
        errorStatusCode = ex.getErrorStatusCode();
      }

      logger.error("[{}:{}] onError : timeTaken={}ms, errorStatusCode={}, responseBody={}", TAG,
          requestId, timeTaken, errorStatusCode, responseBody);

      routingContext.response().headers().addAll(CommonConstants.HEADERS);
      routingContext.response()//
          .setStatusCode(errorStatusCode)//
          .end(responseBody);
    });
  }
}
