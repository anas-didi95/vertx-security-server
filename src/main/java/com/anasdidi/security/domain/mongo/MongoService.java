package com.anasdidi.security.domain.mongo;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.mongo.MongoClient;

class MongoService {

  private MongoClient mongoClient;

  void setMongoClient(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(MongoVO vo) {
    return mongoClient.rxSave(vo.collection, vo.document.put("version", 0)).toSingle();
  }

  Single<String> update(MongoVO vo) {
    JsonObject update = new JsonObject().put("$set", vo.document.put("version", vo.version + 1));

    return mongoClient.rxFindOne(vo.collection, vo.query, new JsonObject())
        .switchIfEmpty(
            Maybe.error(new Exception("Record not found with query: " + vo.query.encode())))
        .flatMap(json -> {
          if (json.getLong("version") != vo.version) {
            return Maybe.error(new Exception(
                "Current record has version mismatch with requested value: " + vo.version));
          } else {
            return Maybe.just(json);
          }
        }).flatMap(json -> mongoClient.rxFindOneAndUpdate(vo.collection, vo.query, update))
        .map(result -> result.getString("_id")).toSingle();
  }

  Single<String> delete(MongoVO vo) {
    return mongoClient.findOne(vo.collection, vo.query, new JsonObject())
        .switchIfEmpty(
            Maybe.error(new Exception("Record not found with query: " + vo.query.encode())))
        .flatMap(json -> mongoClient.rxFindOneAndDelete(vo.collection, vo.query))
        .map(result -> result.getString("_id")).toSingle();
  }
}
