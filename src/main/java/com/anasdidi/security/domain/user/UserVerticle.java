package com.anasdidi.security.domain.user;

import io.vertx.core.Promise;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.web.Router;

public class UserVerticle extends AbstractVerticle {

  private final Router mainRouter;

  public UserVerticle(Router mainRouter) {
    this.mainRouter = mainRouter;
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);

    router.post("/").handler(routingContext -> {
      routingContext.response().putHeader("Content-Type", "application/json").setStatusCode(201)
          .end();
    });

    mainRouter.mountSubRouter("/user", router);
    startFuture.complete();
  }
}
