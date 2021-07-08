package com.anasdidi.security.domain.mongo;

import com.anasdidi.security.common.ApplicationUtils;
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
    vo.document.put("version", 0).put("lastModifiedDate", ApplicationUtils.setRecordDate());
    return mongoClient.rxSave(vo.collection, vo.document).toSingle();
  }

  Single<String> update(MongoVO vo) {
    JsonObject update = new JsonObject().put("$set", vo.document.put("version", vo.version + 1)
        .put("lastModifiedDate", ApplicationUtils.setRecordDate()));

    return checkRecordExist(vo).flatMap(record -> checkRecordVersion(record, vo.version))
        .flatMap(json -> mongoClient.rxFindOneAndUpdate(vo.collection, vo.query, update))
        .map(result -> result.getString("_id")).toSingle();
  }

  Single<String> delete(MongoVO vo) {
    return checkRecordExist(vo).flatMap(record -> checkRecordVersion(record, vo.version))
        .flatMap(json -> mongoClient.rxFindOneAndDelete(vo.collection, vo.query))
        .map(result -> result.getString("_id")).toSingle();
  }

  Single<JsonObject> read(MongoVO vo) {
    return mongoClient.rxFindOne(vo.collection, vo.query, new JsonObject())
        .defaultIfEmpty(new JsonObject());
  }

  private Maybe<JsonObject> checkRecordExist(MongoVO vo) {
    return mongoClient.findOne(vo.collection, vo.query, new JsonObject().put("version", 1))
        .switchIfEmpty(
            Maybe.error(new Exception("Record not found with query: " + vo.query.encode())));
  }

  private Maybe<JsonObject> checkRecordVersion(JsonObject record, long version) {
    return Maybe.defer(() -> {
      if (record.getLong("version") != version) {
        return Maybe.error(
            new Exception("Current record has version mismatch with requested value: " + version));
      }
      return Maybe.just(record);
    });
  }
}
