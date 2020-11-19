package com.anasdidi.security.api.user;

import org.mindrot.jbcrypt.BCrypt;
import io.vertx.core.json.JsonObject;

class UserUtils {

  static String encryptPassword(String password) {
    String encryptedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
    return encryptedPassword;
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

  static JsonObject toJson(UserVO vo) {
    return new JsonObject()//
        .put("id", vo.id)//
        .put("username", vo.username)//
        .put("password", vo.password)//
        .put("fullName", vo.fullName)//
        .put("email", vo.email)//
        .put("version", vo.version);
  }
}
