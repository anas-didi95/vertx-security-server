package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
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

    return sendRequest(EventMongo.MONGO_READ, CollectionRecord.USER, query, null, null)
        .doOnError(error -> {
          logger.error("[login:{}] query{}", vo.traceId, query.encode());
          logger.error("[login:{}] {}", vo.traceId, error.getMessage());
          error.addSuppressed(
              new ApplicationException(ErrorValue.AUTH_LOGIN, vo.traceId, error.getMessage()));
        }).flatMap(response -> {
          JsonObject user = (JsonObject) response.body();

          if (user.isEmpty()) {
            return Single.error(new ApplicationException(ErrorValue.AUTH_LOGIN, vo.traceId,
                "Record not found with username: " + vo.username));
          } else if (!BCrypt.checkpw(vo.password, user.getString("password"))) {
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

    return sendRequest(EventMongo.MONGO_READ, CollectionRecord.USER, query, null, null)
        .doOnError(error -> {
          logger.error("[check:{}] query{}", vo.traceId, query.encode());
          logger.error("[check:{}] {}]", vo.traceId, error.getMessage());
          error.addSuppressed(
              new ApplicationException(ErrorValue.AUTH_CHECK, vo.traceId, error.getMessage()));
        }).flatMap(response -> {
          JsonObject responseBody = (JsonObject) response.body();

          if (responseBody.isEmpty()) {
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

    Single<String> userId =
        sendRequest(EventMongo.MONGO_READ, CollectionRecord.TOKEN, query, null, null)
            .flatMap(response -> {
              JsonObject responseBody = (JsonObject) response.body();

              if (responseBody.isEmpty()) {
                return Single.error(new ApplicationException(ErrorValue.AUTH_REFRESH, vo.traceId,
                    "Record not found with id: " + vo.subject));
              }

              return Single.just(responseBody.getString("userId"));
            });
    Single<String> getAccessToken = userId.flatMap(this::getAccessToken);
    Single<String> getRefreshToken = userId.flatMap(this::getRefreshToken);

    return Single.zip(getAccessToken, getRefreshToken,
        (accessToken, refreshToken) -> AuthVO.fromJson(
            new JsonObject().put("accessToken", accessToken).put("refreshToken", refreshToken)));
  }

  private Single<String> getAccessToken(String userId) {
    JsonObject query = new JsonObject().put("_id", userId);
    return sendRequest(EventMongo.MONGO_READ, CollectionRecord.USER, query, null, null)
        .flatMap(response -> {
          JsonObject responseBody = (JsonObject) response.body();
          return getAccessToken(responseBody);
        });
  }

  private Single<String> getAccessToken(JsonObject user) {
    return Single.fromCallable(() -> {
      ApplicationConfig config = ApplicationConfig.instance();
      return jwtAuth.generateToken(
          new JsonObject().put("typ", "accessToken").put(config.getJwtPermissionsKey(),
              user.getJsonArray("permissions")),
          new JWTOptions().setSubject(user.getString("_id")).setIssuer(config.getJwtIssuer())
              .setExpiresInMinutes(config.getJwtExpireInMinutes()));
    });
  }

  private Single<String> getRefreshToken(String userId) {
    JsonObject document = new JsonObject().put("userId", userId);
    return sendRequest(EventMongo.MONGO_CREATE, CollectionRecord.TOKEN, null, document, null)
        .map(response -> {
          JsonObject responseBody = (JsonObject) response.body();
          ApplicationConfig config = ApplicationConfig.instance();
          return jwtAuth.generateToken(new JsonObject().put("typ", "refreshToken"),
              new JWTOptions().setSubject(responseBody.getString("id"))
                  .setIssuer(config.getJwtIssuer())
                  .setExpiresInMinutes(config.getJwtExpireInMinutes()));
        });
  }
}
