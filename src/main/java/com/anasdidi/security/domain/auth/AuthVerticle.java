package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;

public class AuthVerticle extends BaseVerticle {

  private static final Logger logger = LogManager.getLogger(AuthVerticle.class);
  private final AuthService authService;
  private final AuthHandler authHandler;

  public AuthVerticle() {
    this.authService = new AuthService();
    this.authHandler = new AuthHandler(authService);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    super.start(startFuture);
    authService.setJwtAuth(getAuthProvider());

    logger.info("[start] Verticle started");
    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return AuthConstants.CONTEXT_PATH;
  }

  @Override
  protected void setHandler(Router router, EventBus eventBus, JWTAuthHandler jwtAuthHandler) {
    router.post("/login").handler(authHandler::login);

    router.route().handler(jwtAuthHandler);
    router.get("/check").handler(authHandler::check);
  }
}
