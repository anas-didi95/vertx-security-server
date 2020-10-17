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

  private JwtVO getAndSaveToken(String requestId, String username, String userId) {
    String tag = "getAndSaveToken";
    AppConfig appConfig = AppConfig.instance();

    JsonObject claims = new JsonObject()//
        .put("username", username);
    String accessToken = jwtAuth.generateToken(claims, new JWTOptions()//
        .setSubject(userId)//
        .setIssuer(appConfig.getJwtIssuer())//
        .setExpiresInMinutes(appConfig.getJwtExpireInMinutes()));

    String id = CommonUtils.generateId();
    JsonObject document = new JsonObject()//
        .put("_id", id)//
        .put("hasRefresh", false)//
        .put("createDate", Instant.now())//
        .put("username", username)//
        .put("userId", userId);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] document\n{}", tag, requestId, document.copy().put("accessToken", "***"));
    }

    mongoClient.rxSave(JwtConstants.COLLECTION_NAME, document).subscribe();

    JsonObject json = new JsonObject()//
        .put("id", id)//
        .put("accessToken", accessToken);
    return JwtUtils.toVO(json);
  }

  Single<JwtVO> login(String requestId, String username, String password, JsonObject user) {
    String tag = "login";
    return Single.fromCallable(() -> {
      String uUsername = user.getString("username");
      String uPassword = user.getString("password");
      if (uUsername == null || uUsername.isBlank() || uPassword == null || uPassword.isBlank()) {
        logger.error("[{}:{}] {} username={}", tag, requestId, JwtConstants.MSG_ERR_INVALID_CREDENTIAL, username);
        throw new ApplicationException(JwtConstants.MSG_ERR_INVALID_CREDENTIAL, requestId,
            JwtConstants.MSG_ERR_INVALID_USERNAME_PASSWORD);
      }

      boolean result1 = username.equals(uUsername);
      boolean result2 = BCrypt.checkpw(password, uPassword);
      if (!result1 || !result2) {
        logger.error("[{}:{}] {} username={}", tag, requestId, JwtConstants.MSG_ERR_INVALID_CREDENTIAL, username);
        throw new ApplicationException(JwtConstants.MSG_ERR_INVALID_CREDENTIAL, requestId,
            JwtConstants.MSG_ERR_INVALID_USERNAME_PASSWORD);
      }

      return getAndSaveToken(requestId, username, user.getString("id"));
    });
  }

  Single<JwtVO> refresh(String requestId, JwtVO vo) {
    String tag = "refresh";
    JsonObject query = new JsonObject()//
        .put("_id", vo.id)//
        .put("hasRefresh", false);
    JsonObject update = new JsonObject()//
        .put("$set", new JsonObject()//
            .put("hasRefresh", true));

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] query\n{}", tag, requestId, query.encodePrettily());
      logger.debug("[{}:{}] update\n{}", tag, requestId, update.encodePrettily());
    }

    return mongoClient.rxFindOneAndUpdate(JwtConstants.COLLECTION_NAME, query, update)//
        .doOnComplete(() -> {
          logger.error("[{}:{}] {}", tag, requestId, JwtConstants.MSG_ERR_JWT_RECORD_NOT_FOUND);
          logger.debug("[{}:{}] query\n{}", tag, requestId, query.encodePrettily());
          logger.debug("[{}:{}] update\n{}", tag, requestId, update.encodePrettily());
          throw new ApplicationException(JwtConstants.MSG_ERR_REFRESH_TOKEN_FAILED, requestId,
              JwtConstants.MSG_ERR_JWT_RECORD_NOT_FOUND);
        })//
        .map(rst -> {
          String username = rst.getString("username");
          String userId = rst.getString("userId");

          if (!vo.username.equals(username) || !vo.userId.equals(userId)) {
            throw new ApplicationException(JwtConstants.MSG_ERR_REFRESH_TOKEN_INVALID, requestId,
                JwtConstants.MSG_ERR_REFRESH_TOKEN_CREDENTIAL_MISMATCH);
          }

          return getAndSaveToken(requestId, username, userId);
        })//
        .toSingle();
  }
}
