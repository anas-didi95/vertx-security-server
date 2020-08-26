package com.anasdidi.security.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

public abstract class CommonController {

  private final Logger logger = LogManager.getLogger(CommonController.class);

  protected void sendResponse(String requestId, Single<JsonObject> subscriber, RoutingContext routingContext,
      int statusCode, String message) {
    String tag = "sendResponse";
    subscriber.subscribe(data -> {
      String responseBody = new JsonObject()//
          .put("status", new JsonObject()//
              .put("isSuccess", true)//
              .put("message", message))//
          .put("data", data)//
          .encode();

      logger.info("[{}:{}] onSuccess : responseBody={}", tag, requestId, responseBody);

      routingContext.response()//
          .putHeader(CommonConstants.Header.ACCEPT.value, CommonConstants.MediaType.APP_JSON.value)//
          .putHeader(CommonConstants.Header.CONTENT_TYPE.value, CommonConstants.MediaType.APP_JSON.value)//
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

      logger.error("[{}:{}] onError : responseBody={}", tag, requestId, responseBody);

      routingContext.response()//
          .putHeader(CommonConstants.Header.ACCEPT.value, CommonConstants.MediaType.APP_JSON.value)//
          .putHeader(CommonConstants.Header.CONTENT_TYPE.value, CommonConstants.MediaType.APP_JSON.value)//
          .setStatusCode(200)//
          .end(responseBody);
    });
  }
}
