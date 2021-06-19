package com.anasdidi.security.domain.user;

import io.vertx.core.Promise;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.web.Router;

public class UserVerticle extends AbstractVerticle {

  private final Router mainRouter;
  private final UserHandler userHandler;

  public UserVerticle(Router mainRouter) {
    this.mainRouter = mainRouter;
    this.userHandler = new UserHandler();
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);

    router.post("/").handler(userHandler::create);

    mainRouter.mountSubRouter("/user", router);
    startFuture.complete();
  }
}
