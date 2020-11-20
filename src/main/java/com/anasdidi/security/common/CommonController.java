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
      String responseBody = e.getMessage();
      if (e.getSuppressed().length > 0) {
        for (Throwable t : e.getSuppressed()) {
          if (t instanceof ApplicationException) {
            responseBody = t.getMessage();
          }
        }
      }

      logger.error("[{}:{}] onError : timeTaken={}ms, responseBody={}", TAG, requestId, timeTaken,
          responseBody);

      routingContext.response().headers().addAll(CommonConstants.HEADERS);
      routingContext.response()//
          .setStatusCode(CommonConstants.STATUS_CODE_BAD_REQUEST)//
          .end(responseBody);
    });
  }
}
