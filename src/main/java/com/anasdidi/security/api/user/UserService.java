package com.anasdidi.security.api.user;

import java.util.UUID;

import io.reactivex.Single;
import io.vertx.reactivex.ext.mongo.MongoClient;

public class UserService {

  private final MongoClient mongoClient;

  UserService(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(UserVO vo) {
    vo.id = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    vo.version = Long.valueOf(0);

    return mongoClient.rxSave("users", UserUtils.toMongoDocument(vo))//
        .defaultIfEmpty(vo.id)//
        .toSingle();
  }
}
