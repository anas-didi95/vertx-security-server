package com.anasdidi.security.common;

import io.vertx.core.Promise;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;

public abstract class BaseVerticle extends AbstractVerticle {

  private Router router;

  public abstract String getContextPath();

  protected abstract void setHandler(Router router, EventBus eventBus,
      JWTAuthHandler jwtAuthHandler);

  public final boolean hasRouter() {
    return getContextPath() != null && !getContextPath().isBlank();
  }

  public final Router getRouter() {
    if (hasRouter()) {
      if (router == null) {
        router = Router.router(vertx);
      }
      return router;
    }
    return null;
  };

  protected final JWTAuth getAuthProvider() {
    ApplicationConfig config = ApplicationConfig.instance();
    return JWTAuth.create(vertx,
        new JWTAuthOptions().setJWTOptions(new JWTOptions().setIssuer(config.getJwtIssuer()))
            .addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer(config.getJwtSecret())));
  }

  protected final JWTAuthHandler getJwtAuthHandler() {
    return JWTAuthHandler.create(getAuthProvider());
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    setHandler(getRouter(), vertx.eventBus(), getJwtAuthHandler());
  }
}
