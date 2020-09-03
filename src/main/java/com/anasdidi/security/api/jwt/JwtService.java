package com.anasdidi.security.api.jwt;

import com.anasdidi.security.common.ApplicationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;

class JwtService {

  private final Logger logger = LogManager.getLogger(JwtService.class);
  private final JWTAuth jwtAuth;

  JwtService(JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  Single<String> login(String requestId, String username, String password, JsonObject user) {
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
      return jwtAuth.generateToken(claims, new JWTOptions()//
          .setSubject(user.getString("id"))//
          .setIssuer("anasdidi.com")//
          .setExpiresInMinutes(30));
    });
  }
}
