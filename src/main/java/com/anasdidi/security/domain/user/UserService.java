package com.anasdidi.security.domain.user;

import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.ext.mongo.MongoClient;

class UserService {

  private final MongoClient mongoClient;

  UserService(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(UserDTO dto) {
    return mongoClient.rxSave("users", dto.toJson()).toSingle();
  }
}
