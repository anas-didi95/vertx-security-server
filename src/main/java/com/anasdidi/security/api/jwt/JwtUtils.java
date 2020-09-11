package com.anasdidi.security.api.jwt;

import io.vertx.core.json.JsonObject;

class JwtUtils {

  static JwtVO toVO(JsonObject json) {
    JwtVO vo = new JwtVO();
    vo.id = json.getString("id");
    vo.accessToken = json.getString("accessToken");
    vo.username = json.getString("username");
    vo.password = json.getString("password");
    return vo;
  }
}
