package com.anasdidi.security.api.jwt;

import io.vertx.core.http.CookieSameSite;
import io.vertx.reactivex.core.http.Cookie;

final class JwtUtils {

  static Cookie generateRefreshTokenCookie(String refreshToken) {
    return Cookie.cookie("refreshToken", refreshToken).setSameSite(CookieSameSite.STRICT)
        .setHttpOnly(true).setSecure(false);
  }
}
