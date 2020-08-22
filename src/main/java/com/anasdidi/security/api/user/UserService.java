package com.anasdidi.security.api.user;

import com.anasdidi.security.common.CommonUtils;

import io.reactivex.Single;
import io.vertx.reactivex.ext.mongo.MongoClient;

public class UserService {

  private final MongoClient mongoClient;

  UserService(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(UserVO vo) {
    vo.id = CommonUtils.generateId();
    vo.version = Long.valueOf(0);

    return mongoClient.rxSave(UserConstants.COLLECTION_NAME, UserUtils.toMongoDocument(vo))//
        .defaultIfEmpty(vo.id)//
        .toSingle();
  }
}
