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
      String uUsername = user.getString("username", "");
      String uPassword = user.getString("password", "");
      if (uUsername == null || uUsername.isBlank() || uPassword == null || uPassword.isBlank()) {
        logger.error("[{}:{}] Invalid credential! username={}", tag, requestId, username);
        throw new ApplicationException("Invalid credential!", requestId, "Incorrect username/password!");
      }

      boolean result1 = username.equals(uUsername);
      boolean result2 = BCrypt.checkpw(password, uPassword);
      if (!result1 || !result2) {
        logger.error("[{}:{}] Invalid credential! username={}", tag, requestId, username);
        throw new ApplicationException("Invalid credential!", requestId, "Incorrect username/password!");
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
