package com.anasdidi.security.api.jwt;

import com.anasdidi.security.common.AppConfig;
import io.vertx.core.http.CookieSameSite;
import io.vertx.reactivex.core.http.Cookie;

final class JwtUtils {

  static Cookie generateRefreshTokenCookie(String refreshToken, String salt) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    String value = refreshToken + JwtConstants.REFRESH_TOKEN_DELIMITER + salt;

    return Cookie.cookie("refreshToken", value).setSameSite(CookieSameSite.STRICT).setHttpOnly(true)
        .setSecure(appConfig.getCookieSecure());
  }
}
