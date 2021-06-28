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

    System.out.println(update);
    return mongoClient.rxFindOne(vo.collection, vo.query, new JsonObject())
        .switchIfEmpty(
            Maybe.error(new Exception("Record not found with id: " + vo.query.getString("_id"))))
        .flatMap(json -> mongoClient.rxFindOneAndUpdate(vo.collection, vo.query, update))
        .map(result -> result.getString("_id")).toSingle();
  }
}
