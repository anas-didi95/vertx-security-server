package com.anasdidi.security.domain.user;

import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;

class UserService extends BaseService {

  private final static Logger logger = LogManager.getLogger(UserService.class);

  Single<String> create(UserVO vo) {
    JsonObject document = vo.toJson().put("password", BCrypt.hashpw(vo.password, BCrypt.gensalt()));

    if (logger.isDebugEnabled()) {
      logger.debug("[create:{}] document{}", vo.traceId, document.encode());
    }

    return sendRequest(EventMongo.MONGO_CREATE, CollectionRecord.USER, null, document, null)
        .doOnError(error -> {
          logger.error("[create:{}] document{}", vo.traceId, document.encode());
          logger.error("[create:{}] {}", vo.traceId, error.getMessage());
          error.addSuppressed(new ApplicationException(ErrorValue.USER_CREATE, vo.traceId,
              "Unable to create user with username: " + vo.username));
        }).map(response -> {
          JsonObject responseBody = (JsonObject) response.body();
          return responseBody.getString("id");
        });
  }

  Single<String> update(UserVO vo) {
    JsonObject query = new JsonObject().put("_id", vo.id);
    JsonObject document = new JsonObject().put("fullName", vo.fullName).put("email", vo.email)
        .put("telegramId", vo.telegramId).put("permissions", vo.permissions);

    if (logger.isDebugEnabled()) {
      logger.debug("[update:{}] query{}", vo.traceId, query.encode());
      logger.debug("[update:{}] document{}", vo.traceId, document.encode());
    }

    return sendRequest(EventMongo.MONGO_UPDATE, CollectionRecord.USER, query, document, vo.version)
        .doOnError(error -> {
          logger.error("[update:{}] query{}", vo.traceId, query.encode());
          logger.error("[update:{}] document{}", vo.traceId, document.encode());
          logger.error("[update:{}] {}", vo.traceId, error.getMessage());
          error.addSuppressed(
              new ApplicationException(ErrorValue.USER_UPDATE, vo.traceId, error.getMessage()));
        }).map(response -> {
          JsonObject responseBody = (JsonObject) response.body();
          return responseBody.getString("id");
        });
  }

  Single<String> delete(UserVO vo) {
    JsonObject query = new JsonObject().put("_id", vo.id);

    if (logger.isDebugEnabled()) {
      logger.debug("[delete:{}] query{}", vo.traceId, query.encode());
    }

    return sendRequest(EventMongo.MONGO_DELETE, CollectionRecord.USER, query, null, vo.version)
        .doOnError(error -> {
          logger.error("[delete:{}] query{}", vo.traceId, query.encode());
          logger.error("[delete:{}] {}", vo.traceId, error.getMessage());
          error.addSuppressed(
              new ApplicationException(ErrorValue.USER_DELETE, vo.traceId, error.getMessage()));
        }).map(response -> {
          JsonObject responseBody = (JsonObject) response.body();
          return responseBody.getString("id");
        });
  }
}
