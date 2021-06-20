package com.anasdidi.security.domain.user;

import io.vertx.core.Promise;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.Router;

public class UserVerticle extends AbstractVerticle {

  private final Router mainRouter;
  private final UserService userService;
  private final UserHandler userHandler;

  public UserVerticle(EventBus eventBus, Router mainRouter, MongoClient mongoClient) {
    this.mainRouter = mainRouter;
    this.userService = new UserService(eventBus);
    this.userHandler = new UserHandler(userService);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);

    router.post("/").handler(userHandler::create);

    mainRouter.mountSubRouter("/user", router);
    startFuture.complete();
  }
}
