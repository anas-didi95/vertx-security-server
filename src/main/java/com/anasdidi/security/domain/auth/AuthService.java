package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import com.anasdidi.security.common.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.rxjava3.ext.auth.jwt.JWTAuth;

class AuthService extends BaseService {

  private static final Logger logger = LogManager.getLogger(AuthService.class);
  private JWTAuth jwtAuth;

  void setJwtAuth(JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  Single<String> login(AuthVO vo) {
    JsonObject query = new JsonObject().put("username", vo.username);

    if (logger.isDebugEnabled()) {
      logger.debug("[login:{}] query{}", vo.traceId, query.encode());
    }


    return sendRequest(EventMongo.MONGO_READ, CollectionRecord.USER, query, null, null)
        .doOnError(error -> {
          logger.debug("[login:{}] query{}", vo.traceId, query.encode());
          logger.debug("[login:{}] {}", vo.traceId, error.getMessage());
        }).flatMap(response -> {
          JsonObject user = (JsonObject) response.body();

          if (BCrypt.checkpw(vo.password, user.getString("password"))) {
            String accessToken =
                jwtAuth.generateToken(new JsonObject(), new JWTOptions().setSubject("username"));
            return Single.just(accessToken);
          } else {
            return Single.error(new Exception("Login failed!"));
          }
        });
  }
}
