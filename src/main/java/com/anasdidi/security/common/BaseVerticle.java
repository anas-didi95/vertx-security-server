package com.anasdidi.security.common;

import com.anasdidi.security.common.ApplicationConstants.TokenType;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.rxjava3.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava3.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;

public abstract class BaseVerticle extends AbstractVerticle {

  private Router router;

  public abstract String getContextPath();

  protected abstract String getPermission();

  protected abstract void setHandler(Router router, EventBus eventBus,
      JWTAuthHandler jwtAuthHandler, Handler<RoutingContext> jwtAuthzHandler);

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

  protected final JWTAuth getJwtAuthProvider() {
    ApplicationConfig config = ApplicationConfig.instance();
    return JWTAuth.create(vertx,
        new JWTAuthOptions().setJWTOptions(new JWTOptions().setIssuer(config.getJwtIssuer()))
            .addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer(config.getJwtSecret())));
  }

  private JWTAuthHandler getJwtAuthHandler() {
    return JWTAuthHandler.create(getJwtAuthProvider());
  }

  private JWTAuthorization getJwtAuthzProvider() {
    ApplicationConfig config = ApplicationConfig.instance();
    return JWTAuthorization.create(config.getJwtPermissionsKey());
  }

  private Handler<RoutingContext> getJwtAuthzHandler() {
    return routingContext -> {
      String tokenType = routingContext.user().principal().getString("typ");

      if (!TokenType.TOKEN_ACCESS.toString().equals(tokenType)) {
        routingContext.fail(401);
        return;
      } else if (getPermission() == null || getPermission().isBlank()) {
        routingContext.next();
        return;
      }

      getJwtAuthzProvider().rxGetAuthorizations(routingContext.user()).subscribe(() -> {
        if (PermissionBasedAuthorization.create(getPermission()).match(routingContext.user())) {
          routingContext.next();
          return;
        } else {
          routingContext.fail(403);
          return;
        }
      });
    };
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    setHandler(getRouter(), vertx.eventBus(), getJwtAuthHandler(), getJwtAuthzHandler());
  }
}
