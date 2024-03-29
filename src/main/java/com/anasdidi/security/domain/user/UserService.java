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
        .onErrorResumeNext(error -> {
          logger.error("[create:{}] document{}", vo.traceId, document.encode());
          logger.error("[create:{}] {}", vo.traceId, error.getMessage());
          return Single.error(new ApplicationException(ErrorValue.USER_CREATE, vo.traceId,
              "Unable to create user with username: " + vo.username));
        }).map(response -> {
          JsonObject responseBody = getResponseBody(response);
          return responseBody.getString("id");
        });
  }

  Single<String> update(UserVO vo) {
    JsonObject query = new JsonObject().put("_id", vo.id);
    JsonObject document = new JsonObject().put("fullName", vo.fullName).put("email", vo.email)
        .put("telegramId", vo.telegramId).put("permissions", vo.permissions)
        .put("lastModifiedBy", vo.lastModifiedBy);

    if (logger.isDebugEnabled()) {
      logger.debug("[update:{}] query{}", vo.traceId, query.encode());
      logger.debug("[update:{}] document{}", vo.traceId, document.encode());
    }

    return sendRequest(EventMongo.MONGO_UPDATE, CollectionRecord.USER, query, document, vo.version)
        .onErrorResumeNext(error -> {
          logger.error("[update:{}] query{}", vo.traceId, query.encode());
          logger.error("[update:{}] document{}", vo.traceId, document.encode());
          logger.error("[update:{}] {}", vo.traceId, error.getMessage());
          return Single.error(
              new ApplicationException(ErrorValue.USER_UPDATE, vo.traceId, error.getMessage()));
        }).map(response -> {
          JsonObject responseBody = getResponseBody(response);
          return responseBody.getString("id");
        });
  }

  Single<String> delete(UserVO vo) {
    JsonObject query = new JsonObject().put("_id", vo.id);

    if (logger.isDebugEnabled()) {
      logger.debug("[delete:{}] query{}", vo.traceId, query.encode());
    }

    return sendRequest(EventMongo.MONGO_DELETE_ONE, CollectionRecord.USER, query, null, vo.version)
        .onErrorResumeNext(error -> {
          logger.error("[delete:{}] query{}", vo.traceId, query.encode());
          logger.error("[delete:{}] {}", vo.traceId, error.getMessage());
          return Single.error(
              new ApplicationException(ErrorValue.USER_DELETE, vo.traceId, error.getMessage()));
        }).map(response -> {
          JsonObject responseBody = getResponseBody(response);
          return responseBody.getString("id");
        });
  }

  Single<String> changePassword(UserVO vo) {
    JsonObject query = new JsonObject().put("_id", vo.id);
    JsonObject document =
        new JsonObject().put("password", BCrypt.hashpw(vo.newPassword, BCrypt.gensalt()))
            .put("lastModifiedBy", vo.lastModifiedBy);

    if (logger.isDebugEnabled()) {
      logger.debug("[changePassword:{}] query{}", vo.traceId, query.encode());
      logger.debug("[changePassword:{}] document{}", vo.traceId, document.encode());
    }

    Single<String> checkOldPassword =
        sendRequest(EventMongo.MONGO_READ_ONE, CollectionRecord.USER, query, null, null)
            .onErrorResumeNext(error -> {
              logger.error("[changePassword:{}] query{}", vo.traceId, query.encode());
              return Single.error(new ApplicationException(ErrorValue.USER_CHANGE_PASSWORD,
                  vo.traceId, error.getMessage()));
            }).flatMap(response -> {
              JsonObject responseBody = getResponseBody(response);
              String hashed = responseBody.getString("password", "");
              if (hashed.isBlank()) {
                return Single.error(new ApplicationException(ErrorValue.USER_CHANGE_PASSWORD,
                    vo.traceId, "Record not found with query: " + query.encode()));
              } else if (!BCrypt.checkpw(vo.oldPassword, hashed)) {
                return Single.error(new ApplicationException(ErrorValue.USER_CHANGE_PASSWORD,
                    vo.traceId, "Current record has old password mismatch with requested value"));
              } else {
                return Single.just(hashed);
              }
            });

    Single<String> updatePassword =
        sendRequest(EventMongo.MONGO_UPDATE, CollectionRecord.USER, query, document, vo.version)
            .onErrorResumeNext(error -> {
              logger.error("[changePassword:{}] query{}", vo.traceId, query.encode());
              logger.error("[changePassword:{}] document{}", vo.traceId, document.encode());
              return Single.error(new ApplicationException(ErrorValue.USER_CHANGE_PASSWORD,
                  vo.traceId, error.getMessage()));
            }).map(response -> {
              JsonObject responseBody = getResponseBody(response);
              return responseBody.getString("id");
            });

    return Single.concat(checkOldPassword, updatePassword).lastOrError();
  }
}
