package com.anasdidi.security.api.jwt;

import com.anasdidi.security.common.AppConfig;
import io.vertx.core.http.CookieSameSite;
import io.vertx.reactivex.core.http.Cookie;

final class JwtUtils {

  static Cookie generateRefreshTokenCookie(String refreshToken) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    return Cookie.cookie("refreshToken", refreshToken).setSameSite(CookieSameSite.STRICT)
        .setHttpOnly(true).setSecure(appConfig.getCookieSecure());
  }
}
