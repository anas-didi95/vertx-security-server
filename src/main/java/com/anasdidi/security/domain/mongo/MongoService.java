package com.anasdidi.security.domain.mongo;

import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.ext.mongo.MongoClient;

class MongoService {

  private final MongoClient mongoClient;

  MongoService(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(MongoVO vo) {
    return mongoClient.rxSave(vo.collection, vo.document).toSingle();
  }
}
