package com.anasdidi.security.domain.user;

import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;

public class UserVerticle extends BaseVerticle {

  private static final Logger logger = LogManager.getLogger(UserVerticle.class);
  private final UserService userService;
  private final UserValidator userValidator;
  private final UserHandler userHandler;
  private Router router;

  public UserVerticle() {
    this.userService = new UserService();
    this.userValidator = new UserValidator();
    this.userHandler = new UserHandler(userService, userValidator);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    userService.setEventBus(vertx.eventBus());

    Future<Void> future = startFuture.future();
    future.compose(v -> setHandler(getRouter(), null))
        .onComplete(v -> logger.info("[start] Set handler completed"));

    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return UserConstants.CONTEXT_PATH;
  }

  @Override
  public Router getRouter() {
    if (router == null) {
      this.router = Router.router(vertx);
    }
    return router;
  }

  @Override
  protected Future<Void> setHandler(Router router, EventBus eventBus) {
    return Future.future(promise -> {
      router.post("/").handler(userHandler::create);
      router.put("/:userId").handler(userHandler::update);
      router.delete("/:userId").handler(userHandler::delete);
      promise.complete();
    });
  }
}
