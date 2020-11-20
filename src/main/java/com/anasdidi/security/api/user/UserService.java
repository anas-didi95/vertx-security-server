package com.anasdidi.security.api.user;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import com.anasdidi.security.common.ApplicationException;
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

  Single<String> create(UserVO vo, String requestId) {
    final String TAG = "create";
    JsonObject document = new JsonObject()//
        .put("username", vo.username)//
        .put("password", UserUtils.encryptPassword(vo.password))//
        .put("fullName", vo.fullName)//
        .put("email", vo.email)//
        .put("createDate", new JsonObject().put("$date", Instant.now()))//
        .put("version", 0);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] document\n{}", TAG, requestId, document.encodePrettily());
    }

    return mongoClient.rxSave(UserConstants.COLLECTION_NAME, document)//
        .doOnError(e -> {
          logger.error("[{}:{}] {}", TAG, requestId, e.getMessage());
          logger.error("[{}:{}] document\n{}", TAG, requestId, document.encodePrettily());
          e.addSuppressed(
              new ApplicationException(UserConstants.MSG_ERR_USER_CREATE_FAILED, requestId, e));
        })//
        .toSingle();
  }

  Single<String> update(UserVO vo, String requestId) {
    final String TAG = "update";
    JsonObject query = new JsonObject()//
        .put("_id", vo.id)//
        .put("version", vo.version);
    JsonObject update = new JsonObject()//
        .put("$set", new JsonObject()//
            .put("fullName", vo.fullName)//
            .put("email", vo.email)//
            .put("updateDate", new JsonObject().put("$date", Instant.now()))//
            .put("version", vo.version + 1));

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] query\n{}", TAG, requestId, query.encodePrettily());
      logger.debug("[{}:{}] update\n{}", TAG, requestId, update.encodePrettily());
    }

    return mongoClient.rxFindOneAndUpdate(UserConstants.COLLECTION_NAME, query, update)//
        .doOnComplete(() -> {
          logger.error("[{}:{}] {}", TAG, requestId, UserConstants.MSG_ERR_USER_RECORD_NOT_FOUND);
          logger.error("[{}:{}] query\n{}", TAG, requestId, query.encodePrettily());
          logger.error("[{}:{}] update\n{}", TAG, requestId, update.encodePrettily());
          throw new ApplicationException(UserConstants.MSG_ERR_USER_UPDATE_FAILED, requestId,
              UserConstants.MSG_ERR_USER_RECORD_NOT_FOUND);
        })//
        .map(doc -> doc.getString("_id"))//
        .toSingle();
  }

  Single<String> delete(String requestId, UserVO vo) {
    String tag = "delete";
    JsonObject query = new JsonObject()//
        .put("_id", vo.id)//
        .put("version", vo.version);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] query\n", tag, requestId, query.encodePrettily());
    }

    return mongoClient.rxFindOneAndDelete(UserConstants.COLLECTION_NAME, query)//
        .doOnComplete(() -> {
          logger.error("[{}:{}] {}", tag, requestId, UserConstants.MSG_ERR_USER_RECORD_NOT_FOUND);
          logger.error("[{}:{}] query\n", tag, requestId, query.encodePrettily());
          throw new ApplicationException(UserConstants.MSG_ERR_USER_DELETE_FAILED, requestId,
              UserConstants.MSG_ERR_USER_RECORD_NOT_FOUND);
        })//
        .map(doc -> doc.getString("_id"))//
        .toSingle();
  }

  Single<UserVO> getUserByUsername(UserVO vo) {
    JsonObject query = new JsonObject()//
        .put("username", vo.username);
    JsonObject fields = new JsonObject();

    return mongoClient.rxFindOne(UserConstants.COLLECTION_NAME, query, fields)//
        .map(json -> UserVO.fromJson(json))//
        .defaultIfEmpty(new UserVO(null, null, null, null, null, null, null, null))//
        .toSingle();
  }

  Single<List<UserVO>> getUserList(UserVO vo) {
    JsonObject query = new JsonObject();

    return mongoClient.rxFind(UserConstants.COLLECTION_NAME, query)//
        .map(resultList -> resultList.stream().map(json -> UserVO.fromJson(json))
            .collect(Collectors.toList()));
  }

  Single<UserVO> getUserById(UserVO vo) {
    JsonObject query = new JsonObject()//
        .put("_id", vo.id);
    JsonObject fields = new JsonObject();

    return mongoClient.rxFindOne(UserConstants.COLLECTION_NAME, query, fields)//
        .map(json -> UserVO.fromJson(json))//
        .defaultIfEmpty(new UserVO(null, null, null, null, null, null, null, null))//
        .toSingle();
  }
}
