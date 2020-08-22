package com.anasdidi.security.common;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

public abstract class CommonController {

  protected void sendResponse(Single<JsonObject> subscriber, RoutingContext routingContext, int statusCode,
      String message) {
    subscriber.subscribe(data -> {
      routingContext.response()//
          .putHeader("Accept", "application/json")//
          .putHeader("Content-Type", "application/json")//
          .setStatusCode(statusCode)//
          .end(new JsonObject()//
              .put("status", new JsonObject()//
                  .put("isSuccess", true)//
                  .put("message", message))
              .put("data", data)//
              .encode());
    }, e -> {
      routingContext.response()//
          .putHeader("Accept", "application/json")//
          .putHeader("Content-Type", "application/json")//
          .setStatusCode(200)//
          .end(new JsonObject()//
              .put("status", new JsonObject()//
                  .put("isSuccess", false)//
                  .put("message", "Request failed!"))
              .put("error", e.getMessage())//
              .encode());
    });
  }
}
