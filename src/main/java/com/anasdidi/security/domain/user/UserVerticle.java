package com.anasdidi.security.domain.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;

public class UserVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(UserVerticle.class);
  private final Router mainRouter;
  private final UserService userService;
  private final UserValidator userValidator;
  private final UserHandler userHandler;

  public UserVerticle(EventBus eventBus, Router mainRouter) {
    this.mainRouter = mainRouter;
    this.userService = new UserService(eventBus);
    this.userValidator = new UserValidator();
    this.userHandler = new UserHandler(userService, userValidator);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    Future<Void> future = startFuture.future();
    future.compose(v -> setupRouter())
        .onComplete(v -> logger.info("[start] Setup router completed"));

    startFuture.complete();
  }

  private Future<Void> setupRouter() {
    return Future.future(promise -> {
      Router router = Router.router(vertx);
      router.post("/").handler(userHandler::create);
      mainRouter.mountSubRouter("/user", router);

      promise.complete();
    });
  }
}
