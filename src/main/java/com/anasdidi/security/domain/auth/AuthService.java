package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import com.anasdidi.security.common.ApplicationConstants.TokenType;
import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.ApplicationUtils;
import com.anasdidi.security.common.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.rxjava3.ext.auth.jwt.JWTAuth;

class AuthService extends BaseService {

  private static final Logger logger = LogManager.getLogger(AuthService.class);
  private JWTAuth jwtAuth;

  void setJwtAuth(JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  Single<AuthVO> login(AuthVO vo) {
    JsonObject query = new JsonObject().put("username", vo.username);

    if (logger.isDebugEnabled()) {
      logger.debug("[login:{}] query{}", vo.traceId, query.encode());
    }

    return sendRequest(EventMongo.MONGO_READ_ONE, CollectionRecord.USER, query, null, null)
        .flatMap(response -> {
          JsonObject user = getResponseBody(response);

          if (user.isEmpty()) {
            logger.error("[login:{}] query{}", vo.traceId, query.encode());
            return Single.error(new ApplicationException(ErrorValue.AUTH_LOGIN, vo.traceId,
                "Record not found with username: " + vo.username));
          } else if (!BCrypt.checkpw(vo.password, user.getString("password"))) {
            logger.error("[login:{}] query{}", vo.traceId, query.encode());
            return Single.error(new ApplicationException(ErrorValue.AUTH_LOGIN, vo.traceId,
                "Wrong password for username: " + vo.username));
          }

          String userId = user.getString("_id");
          return Single.zip(getAccessToken(user), getRefreshToken(userId),
              (accessToken, refreshToken) -> AuthVO.fromJson(new JsonObject()
                  .put("accessToken", accessToken).put("refreshToken", refreshToken)));
        });
  }

  Single<JsonObject> check(AuthVO vo) {
    JsonObject query = new JsonObject().put("_id", vo.subject);

    if (logger.isDebugEnabled()) {
      logger.debug("[check:{}] query{}", vo.traceId, query.encode());
    }

    return sendRequest(EventMongo.MONGO_READ_ONE, CollectionRecord.USER, query, null, null)
        .flatMap(response -> {
          JsonObject responseBody = getResponseBody(response);

          if (responseBody.isEmpty()) {
            logger.error("[check:{}] query{}", vo.traceId, query.encode());
            return Single.error(new ApplicationException(ErrorValue.AUTH_CHECK, vo.traceId,
                "Record not found with id: " + vo.subject));
          }

          return Single.just(new JsonObject().put("userId", responseBody.getString("_id"))
              .put("username", responseBody.getString("username"))
              .put("fullName", responseBody.getString("fullName"))
              .put("permissions", responseBody.getJsonArray("permissions")));
        });
  }

  Single<AuthVO> refresh(AuthVO vo) {
    JsonObject query = new JsonObject().put("_id", vo.subject);

    if (logger.isDebugEnabled()) {
      logger.debug("[refresh:{}] query{}", vo.traceId, query.encode());
    }

    Single<JsonObject> token =
        sendRequest(EventMongo.MONGO_READ_ONE, CollectionRecord.TOKEN, query, null, null)
            .flatMap(response -> {
              JsonObject responseBody = getResponseBody(response);

              if (responseBody.isEmpty()) {
                logger.error("[refresh:{}] query{}", vo.traceId, query.encode());
                return Single.error(new ApplicationException(ErrorValue.AUTH_REFRESH, vo.traceId,
                    "Record not found with id: " + vo.subject));
              }

              return Single.just(responseBody);
            });
    Single<String> revokeToken =
        token.flatMap(json -> revokeRefreshToken(json.getString("_id"), json.getLong("version")));
    Single<String> getAccessToken = token.flatMap(json -> getAccessToken(json.getString("userId")));
    Single<String> getRefreshToken =
        token.flatMap(json -> getRefreshToken(json.getString("userId")));

    return Single.zip(revokeToken, getAccessToken, getRefreshToken,
        (revokeTokenId, accessToken, refreshToken) -> AuthVO.fromJson(
            new JsonObject().put("accessToken", accessToken).put("refreshToken", refreshToken)));
  }

  Single<String> logout(AuthVO vo) {
    JsonObject query = new JsonObject().put("userId", vo.subject);

    if (logger.isDebugEnabled()) {
      logger.debug("[logout:{}] query{}", vo.traceId, query.encode());
    }

    return sendRequest(EventMongo.MONGO_DELETE_MANY, CollectionRecord.TOKEN, query, null, null)
        .flatMap(response -> {
          JsonObject responseBody = getResponseBody(response);
          long removedCount = responseBody.getLong(MongoClientDeleteResult.REMOVED_COUNT);

          if (removedCount <= 0) {
            logger.error("[logout:{}] query{}", vo.traceId, query.encode());
            return Single.error(new ApplicationException(ErrorValue.AUTH_LOGOUT, vo.traceId,
                "Record not found with userId: " + vo.subject));
          }

          return Single.just(vo.subject);
        });
  }

  private Single<String> getAccessToken(String userId) {
    JsonObject query = new JsonObject().put("_id", userId);
    return sendRequest(EventMongo.MONGO_READ_ONE, CollectionRecord.USER, query, null, null)
        .flatMap(response -> {
          JsonObject responseBody = getResponseBody(response);
          return getAccessToken(responseBody);
        });
  }

  private Single<String> getAccessToken(JsonObject user) {
    return Single.fromCallable(() -> {
      ApplicationConfig config = ApplicationConfig.instance();
      return jwtAuth.generateToken(
          new JsonObject().put("typ", TokenType.TOKEN_ACCESS).put(config.getJwtPermissionsKey(),
              user.getJsonArray("permissions")),
          new JWTOptions().setSubject(user.getString("_id")).setIssuer(config.getJwtIssuer())
              .setExpiresInMinutes(config.getJwtAccessTokenExpireInMinutes()));
    });
  }

  private Single<String> getRefreshToken(String userId) {
    JsonObject document =
        new JsonObject().put("userId", userId).put("issuedDate", ApplicationUtils.setRecordDate());
    return sendRequest(EventMongo.MONGO_CREATE, CollectionRecord.TOKEN, null, document, null)
        .map(response -> {
          JsonObject responseBody = getResponseBody(response);
          ApplicationConfig config = ApplicationConfig.instance();
          return jwtAuth.generateToken(new JsonObject().put("typ", TokenType.TOKEN_REFRESH),
              new JWTOptions().setSubject(responseBody.getString("id"))
                  .setIssuer(config.getJwtIssuer())
                  .setExpiresInMinutes(config.getJwtRefreshTokenExpireInMinutes()));
        });
  }

  private Single<String> revokeRefreshToken(String id, Long version) {
    JsonObject query = new JsonObject().put("_id", id);
    return sendRequest(EventMongo.MONGO_DELETE_ONE, CollectionRecord.TOKEN, query, null, version)
        .map(response -> {
          JsonObject responseBody = getResponseBody(response);
          return responseBody.getString("id");
        });
  }
}
