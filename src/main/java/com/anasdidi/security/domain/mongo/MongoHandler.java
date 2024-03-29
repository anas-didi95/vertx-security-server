package com.anasdidi.security.domain.mongo;

import com.anasdidi.security.common.BaseHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClientDeleteResult;
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

  void deleteOne(Message<Object> request) {
    getRequestBody(request).map(json -> MongoVO.fromJson(json))
        .flatMap(vo -> mongoService.deleteOne(vo))
        .subscribe(id -> request.reply(new JsonObject().put("id", id)),
            error -> request.fail(3, error.getMessage()));
  }

  void deleteMany(Message<Object> request) {
    getRequestBody(request).map(json -> MongoVO.fromJson(json))
        .flatMap(vo -> mongoService.deleteMany(vo)).subscribe(
            removedCount -> request
                .reply(new JsonObject().put(MongoClientDeleteResult.REMOVED_COUNT, removedCount)),
            error -> request.fail(5, error.getMessage()));
  }

  void readOne(Message<Object> request) {
    getRequestBody(request).map(json -> MongoVO.fromJson(json))
        .flatMap(vo -> mongoService.readOne(vo))
        .subscribe(json -> request.reply(json), error -> request.fail(4, error.getMessage()));
  }

  void readMany(Message<Object> request) {
    getRequestBody(request).map(json -> MongoVO.fromJson(json))
        .flatMap(vo -> mongoService.readMany(vo))
        .subscribe(resultList -> request.reply(new JsonObject().put("resultList", resultList)),
            error -> request.fail(4, error.getMessage()));
  }
}
