package com.anasdidi.security.domain.mongo;

import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.ext.mongo.MongoClient;

class MongoService {

  private MongoClient mongoClient;

  void setMongoClient(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(MongoVO vo) {
    return mongoClient.rxSave(vo.collection, vo.document).toSingle();
  }

  Single<String> update(MongoVO vo) {
    return mongoClient.rxFindOneAndUpdate(vo.collection, vo.query, vo.document)
        .map(result -> result.getString("_id")).toSingle();
  }
}
