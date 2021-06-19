package com.anasdidi.security.domain.user;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.RoutingContext;

class UserHandler {

  private final Logger logger = LogManager.getLogger(UserHandler.class);
  private final MongoClient mongoClient;

  UserHandler(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  void create(RoutingContext routingContext) {
    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();

      if (logger.isDebugEnabled()) {
        logger.debug("[create] requestBody {}", requestBody.encode());
      }

      return requestBody;
    }).flatMap(json -> mongoClient.rxSave("users", json).toSingle())
        .map(id -> new JsonObject().put("id", id));

    subscriber.subscribe(responseBody -> {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      routingContext.response().setStatusCode(201).headers().addAll(headers);
      routingContext.response().end(responseBody.encode());
    });
  }
}
