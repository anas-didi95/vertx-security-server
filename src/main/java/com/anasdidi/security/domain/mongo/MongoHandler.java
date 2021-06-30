package com.anasdidi.security.domain.mongo;

import com.anasdidi.security.common.BaseHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.eventbus.Message;

class MongoHandler extends BaseHandler {

  private final MongoService mongoService;

  MongoHandler(MongoService mongoService) {
    this.mongoService = mongoService;
  }

  void create(Message<Object> request) {
    getRequestBody(request).map(json -> MongoVO.fromJson(json))
        .flatMap(vo -> mongoService.create(vo))
        .subscribe(id -> request.reply(new JsonObject().put("id", id)),
            error -> request.fail(1, error.getMessage()));
  }

  void update(Message<Object> request) {
    getRequestBody(request).map(json -> MongoVO.fromJson(json))
        .flatMap(vo -> mongoService.update(vo))
        .subscribe(id -> request.reply(new JsonObject().put("id", id)),
            error -> request.fail(2, error.getMessage()));
  }

  void delete(Message<Object> request) {
    getRequestBody(request).map(json -> MongoVO.fromJson(json))
        .flatMap(vo -> mongoService.delete(vo))
        .subscribe(id -> request.reply(new JsonObject().put("id", id)),
            error -> request.fail(3, error.getMessage()));
  }
}
