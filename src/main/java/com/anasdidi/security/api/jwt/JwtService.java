package com.anasdidi.security.api.jwt;

import java.time.Instant;

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
  private final JsonObject cfg;

  JwtService(JWTAuth jwtAuth, MongoClient mongoClient, JsonObject cfg) {
    this.jwtAuth = jwtAuth;
    this.mongoClient = mongoClient;
    this.cfg = cfg;
  }

  private JwtVO getAndSaveToken(String requestId, String username, String userId) {
    String tag = "getAndSaveToken";

    JsonObject claims = new JsonObject()//
        .put("username", username);
    String accessToken = jwtAuth.generateToken(claims, new JWTOptions()//
        .setSubject(userId)//
        .setIssuer(cfg.getString("JWT_ISSUER"))//
        .setExpiresInMinutes(cfg.getInteger("JWT_EXPIRE_IN_MINUTES")));

    String id = CommonUtils.generateId();
    JsonObject document = new JsonObject()//
        .put("_id", id)//
        .put("accessToken", accessToken)//
        .put("hasRefresh", false)//
        .put("createTimestamp", Instant.now())//
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

    return mongoClient.rxFindOneAndUpdate(JwtConstants.COLLECTION_NAME, query, update).map(rst -> {
      return getAndSaveToken(requestId, rst.getString("username"), rst.getString("userId"));
    }).toSingle();
  }
}
