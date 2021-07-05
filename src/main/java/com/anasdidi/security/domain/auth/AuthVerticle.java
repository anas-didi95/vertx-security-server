package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;

public class AuthVerticle extends BaseVerticle {

  private static final Logger logger = LogManager.getLogger(AuthVerticle.class);
  private final AuthService authService;
  private final AuthHandler authHandler;
  private Router router;

  public AuthVerticle() {
    this.authService = new AuthService();
    this.authHandler = new AuthHandler(authService);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    Future<Void> future = startFuture.future();
    future.compose(v -> setHandler(getRouter(), null))
        .onComplete(v -> logger.info("[start] Set handler completed"));

    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return AuthConstants.CONTEXT_PATH;
  }

  @Override
  public Router getRouter() {
    if (router == null) {
      router = Router.router(vertx);
    }
    return router;
  }

  @Override
  protected Future<Void> setHandler(Router router, EventBus eventBus) {
    return Future.future(promise -> {
      router.post("/login").handler(authHandler::login);
      promise.complete();
    });
  }
}
