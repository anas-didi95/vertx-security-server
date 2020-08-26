package com.anasdidi.security.api.user;

import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.mongo.MongoClient;

class UserService {

  private final Logger logger = LogManager.getLogger(UserService.class);
  private final MongoClient mongoClient;

  UserService(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(String requestId, UserVO vo) {
    String tag = "create";
    vo.id = CommonUtils.generateId();
    vo.version = Long.valueOf(0);
    JsonObject document = UserUtils.toMongoDocument(vo);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] document\n{}", tag, requestId, document.encodePrettily());
    }

    return mongoClient.rxSave(UserConstants.COLLECTION_NAME, document)//
        .doOnError(e -> {
          logger.error("[{}:{}] {}\n{}", tag, requestId, e.getMessage());
          logger.error("[{}:{}] document\n{}", tag, requestId, document.encodePrettily());
          e.addSuppressed(new ApplicationException("User creation failed!", requestId, e));
        })//
        .defaultIfEmpty(vo.id)//
        .toSingle();
  }

  Single<String> update(String requestId, UserVO vo) {
    String tag = "update";
    JsonObject query = new JsonObject()//
        .put("_id", vo.id)//
        .put("version", vo.version);
    JsonObject update = new JsonObject()//
        .put("$set", new JsonObject()//
            .put("fullName", vo.fullName)//
            .put("email", vo.email)//
            .put("version", vo.version + 1));

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] query\n{}", tag, requestId, query.encodePrettily());
      logger.debug("[{}:{}] update\n{}", tag, requestId, update.encodePrettily());
    }

    return mongoClient.rxFindOneAndUpdate(UserConstants.COLLECTION_NAME, query, update)//
        .map(doc -> doc.getString("_id"))//
        .toSingle();
  }
}
