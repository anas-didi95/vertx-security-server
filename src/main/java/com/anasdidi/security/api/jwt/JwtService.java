package com.anasdidi.security.api.jwt;

import org.mindrot.jbcrypt.BCrypt;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;

class JwtService {

  private final JWTAuth jwtAuth;

  JwtService(JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  Single<String> validate(String username, String password, JsonObject user) {
    return Single.fromCallable(() -> {
      boolean result1 = username.equals(username);
      boolean result2 = BCrypt.checkpw(password, user.getString("password"));

      if (result1 && result2) {
        JsonObject claims = new JsonObject();
        return jwtAuth.generateToken(claims, new JWTOptions()//
            .setSubject(user.getString("id"))//
            .setIssuer("anasdidi.com")//
            .setExpiresInMinutes(30));
      }

      return "";
    });
  }
}
