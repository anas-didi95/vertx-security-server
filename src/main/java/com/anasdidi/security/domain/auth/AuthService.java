package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseService;
import org.mindrot.jbcrypt.BCrypt;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.rxjava3.ext.auth.jwt.JWTAuth;

class AuthService extends BaseService {

  private JWTAuth jwtAuth;

  public void setJwtAuth(JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  Single<String> login(AuthVO vo) {
    if (vo.username.equals("admin")
        && BCrypt.checkpw(vo.password, BCrypt.hashpw("password", BCrypt.gensalt()))) {

      String accessToken =
          jwtAuth.generateToken(new JsonObject(), new JWTOptions().setSubject("username"));
      return Single.just(accessToken);
    }
    return Single.error(new Exception("Login failed"));
  }
}
