package com.anasdidi.security.domain.user;

import java.util.HashMap;
import java.util.Map;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

class UserHandler {

  void create(RoutingContext routingContext) {
    Single<JsonObject> subscriber =
        Single.just(new JsonObject().put("id", System.currentTimeMillis()));

    subscriber.subscribe(responseBody -> {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      routingContext.response().setStatusCode(201).headers().addAll(headers);
      routingContext.response().end(responseBody.encode());
    });
  }
}
