package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationException;
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
          logger.error("[login:{}] query{}", vo.traceId, query.encode());
          logger.error("[login:{}] {}", vo.traceId, error.getMessage());
          error.addSuppressed(
              new ApplicationException(ErrorValue.AUTH_LOGIN, vo.traceId, error.getMessage()));
        }).flatMap(response -> {
          JsonObject user = (JsonObject) response.body();

          if (user.isEmpty()) {
            return Single.error(new ApplicationException(ErrorValue.AUTH_LOGIN, vo.traceId,
                "Record not found with username: " + vo.username));
          } else if (!BCrypt.checkpw(vo.password, user.getString("password"))) {
            return Single.error(new ApplicationException(ErrorValue.AUTH_LOGIN, vo.traceId,
                "Wrong password for username: " + vo.username));
          }

          String accessToken = getAccessToken(user);
          return Single.just(accessToken);
        });
  }

  private String getAccessToken(JsonObject user) {
    ApplicationConfig config = ApplicationConfig.instance();
    return jwtAuth.generateToken(new JsonObject().put("typ", "accessToken"),
        new JWTOptions().setSubject(user.getString("username")).setIssuer(config.getJwtIssuer())
            .setExpiresInMinutes(config.getJwtExpireInMinutes()));
  }
}
