package com.anasdidi.security.common;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

public abstract class CommonController {

  protected void sendResponse(Single<JsonObject> subscriber, RoutingContext routingContext, int statusCode,
      String message) {
    subscriber.subscribe(data -> {
      routingContext.response()//
          .putHeader(CommonConstants.Header.ACCEPT.value, CommonConstants.MediaType.APP_JSON.value)//
          .putHeader(CommonConstants.Header.CONTENT_TYPE.value, CommonConstants.MediaType.APP_JSON.value)//
          .setStatusCode(statusCode)//
          .end(new JsonObject()//
              .put("status", new JsonObject()//
                  .put("isSuccess", true)//
                  .put("message", message))
              .put("data", data)//
              .encode());
    }, e -> {
      String responseBody = e.getMessage();
      if (e.getSuppressed().length > 0) {
        for (Throwable t : e.getSuppressed()) {
          if (t instanceof ApplicationException) {
            responseBody = t.getMessage();
          }
        }
      }
      routingContext.response()//
          .putHeader(CommonConstants.Header.ACCEPT.value, CommonConstants.MediaType.APP_JSON.value)//
          .putHeader(CommonConstants.Header.CONTENT_TYPE.value, CommonConstants.MediaType.APP_JSON.value)//
          .setStatusCode(200)//
          .end(responseBody);
    });
  }
}
