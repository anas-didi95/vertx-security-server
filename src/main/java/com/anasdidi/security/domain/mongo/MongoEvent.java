package com.anasdidi.security.domain.mongo;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.eventbus.Message;

class MongoEvent {

  private final MongoService mongoService;

  MongoEvent(MongoService mongoService) {
    this.mongoService = mongoService;
  }

  void create(Message<Object> request) {
    Single.fromCallable(() -> {
      JsonObject requestBody = (JsonObject) request.body();
      return requestBody;
    }).map(json -> MongoVO.fromJson(json)).flatMap(vo -> mongoService.create(vo)).subscribe(
        id -> request.reply(new JsonObject().put("id", id)),
        error -> request.fail(1, error.getMessage()));
  }
}
