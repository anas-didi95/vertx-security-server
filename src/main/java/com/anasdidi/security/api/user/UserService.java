package com.anasdidi.security.api.user;

import com.anasdidi.security.common.CommonUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Single;
import io.vertx.reactivex.ext.mongo.MongoClient;

public class UserService {

  private final Logger logger = LogManager.getLogger(UserService.class);
  private final MongoClient mongoClient;

  UserService(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(String requestId, UserVO vo) {
    String tag = "create";
    vo.id = CommonUtils.generateId();
    vo.version = Long.valueOf(0);

    return mongoClient.rxSave(UserConstants.COLLECTION_NAME, UserUtils.toMongoDocument(vo))//
        .doOnError(e -> logger.error("[{}:{}] {}\n{}", tag, requestId, e.getMessage(), vo.toString()))//
        .defaultIfEmpty(vo.id)//
        .toSingle();
  }
}
