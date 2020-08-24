package com.anasdidi.security.api.user;

import io.vertx.core.json.JsonObject;

class UserUtils {

  static UserVO toVO(JsonObject json) {
    UserVO vo = new UserVO();
    vo.id = json.getString("id");
    vo.username = json.getString("username");
    vo.password = json.getString("password");
    vo.fullName = json.getString("fullName");
    vo.email = json.getString("email");
    vo.version = json.getLong("version");
    return vo;
  }

  static JsonObject toMongoDocument(UserVO vo) {
    return new JsonObject()//
        .put("_id", vo.id)//
        .put("username", vo.username)//
        .put("password", vo.password)//
        .put("fullName", vo.fullName)//
        .put("email", vo.email)//
        .put("version", vo.version);
  }
}
