package com.anasdidi.security.api.jwt;

import io.vertx.core.json.JsonObject;

class JwtUtils {

  static JwtVO toVO(JsonObject json) {
    JwtVO vo = new JwtVO();
    vo.username = json.getString("username");
    vo.password = json.getString("password");
    return vo;
  }
}
