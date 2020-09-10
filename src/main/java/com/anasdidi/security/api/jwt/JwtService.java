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

  Single<JsonObject> login(String requestId, String username, String password, JsonObject user) {
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

      JsonObject claims = new JsonObject()//
          .put("username", username);
      String accessToken = jwtAuth.generateToken(claims, new JWTOptions()//
          .setSubject(user.getString("id"))//
          .setIssuer(cfg.getString("JWT_ISSUER"))//
          .setExpiresInMinutes(cfg.getInteger("JWT_EXPIRE_IN_MINUTES")));

      String id = CommonUtils.generateId();
      JsonObject document = new JsonObject()//
          .put("_id", id)//
          .put("accessToken", accessToken)//
          .put("timestampCreated", Instant.now());
      mongoClient.rxSave("jwts", document).subscribe();

      return new JsonObject()//
          .put("id", id)//
          .put("accessToken", accessToken);
    });
  }
}
