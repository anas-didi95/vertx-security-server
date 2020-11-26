package com.anasdidi.security.api.jwt;

import java.time.Instant;
import com.anasdidi.security.common.AppConfig;
import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.mongo.MongoClient;

class JwtService {

  private final Logger logger = LogManager.getLogger(JwtService.class);
  private final JWTAuth jwtAuth;
  private final MongoClient mongoClient;

  JwtService(JWTAuth jwtAuth, MongoClient mongoClient) {
    this.jwtAuth = jwtAuth;
    this.mongoClient = mongoClient;
  }

  private JwtVO getAndSaveToken(String username, String userId, String requestId) throws Exception {
    final String TAG = "getAndSaveToken";
    AppConfig appConfig = AppConfig.instance();

    JsonObject claims = new JsonObject()//
        .put("username", username);
    String accessToken = jwtAuth.generateToken(claims, new JWTOptions()//
        .setSubject(userId)//
        .setIssuer(appConfig.getJwtIssuer())//
        .setExpiresInMinutes(appConfig.getJwtExpireInMinutes()));

    String id = CommonUtils.generateUUID();
    String salt = BCrypt.gensalt().replace(JwtConstants.REFRESH_TOKEN_DELIMITER, "#");
    JsonObject document = new JsonObject()//
        .put("_id", id)//
        .put("userId", userId)//
        .put("username", username)//
        .put("salt", salt)//
        .put("issuedDate", new JsonObject().put("$date", Instant.now()));

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] document\n{}", TAG, requestId, document.encodePrettily());
    }

    mongoClient.rxSave(JwtConstants.COLLECTION_NAME, document).subscribe();

    JsonObject json = new JsonObject()//
        .put("id", id)//
        .put("accessToken", accessToken)//
        .put("salt", salt);
    return JwtVO.fromJson(json);
  }

  Single<JwtVO> login(String username, String password, JsonObject user, String requestId) {
    final String TAG = "login";
    return Single.fromCallable(() -> {
      String uUsername = user.getString("username");
      String uPassword = user.getString("password");
      if (uUsername == null || uUsername.isBlank() || uPassword == null || uPassword.isBlank()) {
        logger.error("[{}:{}] {} username={}", TAG, requestId,
            JwtConstants.MSG_ERR_INVALID_CREDENTIAL, username);
        throw new ApplicationException(JwtConstants.MSG_ERR_INVALID_CREDENTIAL, requestId,
            JwtConstants.MSG_ERR_INVALID_USERNAME_PASSWORD);
      }

      boolean result1 = username.equals(uUsername);
      boolean result2 = BCrypt.checkpw(password, uPassword);
      if (!result1 || !result2) {
        logger.error("[{}:{}] {} username={}", TAG, requestId,
            JwtConstants.MSG_ERR_INVALID_CREDENTIAL, username);
        throw new ApplicationException(JwtConstants.MSG_ERR_INVALID_CREDENTIAL, requestId,
            JwtConstants.MSG_ERR_INVALID_USERNAME_PASSWORD);
      }

      return getAndSaveToken(username, user.getString("id"), requestId);
    });
  }

  Single<JwtVO> refresh(JwtVO vo, String requestId) {
    final String TAG = "refresh";
    JsonObject query = new JsonObject()//
        .put("_id", vo.id)//
        .put("salt", vo.salt);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] query\n{}", TAG, requestId, query.encodePrettily());
    }

    return mongoClient.rxFindOneAndDelete(JwtConstants.COLLECTION_NAME, query)//
        .doOnComplete(() -> {
          logger.error("[{}:{}] {}", TAG, requestId, JwtConstants.MSG_ERR_JWT_RECORD_NOT_FOUND);
          logger.debug("[{}:{}] query\n{}", TAG, requestId, query.encodePrettily());
          throw new ApplicationException(JwtConstants.MSG_ERR_REFRESH_TOKEN_FAILED, requestId,
              JwtConstants.MSG_ERR_JWT_RECORD_NOT_FOUND);
        })//
        .map(rst -> {
          String username = rst.getString("username");
          String userId = rst.getString("userId");

          return getAndSaveToken(username, userId, requestId);
        })//
        .toSingle();
  }

  Single<JwtVO> logout(JwtVO vo, String requestId) {
    final String TAG = "logout";
    JsonObject query = new JsonObject()//
        .put("_id", vo.id)//
        .put("salt", vo.salt);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] query\n{}", TAG, requestId, query.encodePrettily());
    }

    return mongoClient.rxFindOneAndDelete(JwtConstants.COLLECTION_NAME, query).doOnComplete(() -> {
      logger.error("[{}:{}] Refresh token not found!", TAG, requestId);
      logger.error("[{}:{}] query\n{}", TAG, requestId, query.encodePrettily());
    }).defaultIfEmpty(new JsonObject()).map(json -> JwtVO.fromJson(json)).toSingle();
  }
}
