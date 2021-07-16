package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;

public class AuthVerticle extends BaseVerticle {

  private static final Logger logger = LogManager.getLogger(AuthVerticle.class);
  private final AuthService authService;
  private final AuthValidator authValidator;
  private final AuthHandler authHandler;

  public AuthVerticle() {
    this.authService = new AuthService();
    this.authValidator = new AuthValidator();
    this.authHandler = new AuthHandler(authService, authValidator);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    super.start(startFuture);
    authService.setJwtAuth(getJwtAuthProvider());
    authService.setEventBus(vertx.eventBus());

    logger.info("[start] Verticle started");
    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return AuthConstants.CONTEXT_PATH;
  }

  @Override
  protected String getPermission() {
    return null;
  }

  @Override
  protected void setHandler(Router router, EventBus eventBus, JWTAuthHandler jwtAuthHandler,
      Handler<RoutingContext> jwtAuthzHandler) {
    router.post("/login").handler(authHandler::login);

    router.route().handler(jwtAuthHandler).failureHandler(authHandler::sendResponseFailure);
    router.get("/check").handler(authHandler::check);
    router.get("/refresh").handler(authHandler::refresh);
  }
}
