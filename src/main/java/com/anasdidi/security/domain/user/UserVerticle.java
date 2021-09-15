package com.anasdidi.security.domain.user;

import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;

public class UserVerticle extends BaseVerticle {

  private static final Logger logger = LogManager.getLogger(UserVerticle.class);
  private final UserService userService;
  private final UserValidator userValidator;
  private final UserHandler userHandler;

  public UserVerticle() {
    this.userService = new UserService();
    this.userValidator = new UserValidator();
    this.userHandler = new UserHandler(userService, userValidator);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    super.start(startFuture);
    userService.setEventBus(vertx.eventBus());

    logger.info("[start] Verticle started");
    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return UserConstants.CONTEXT_PATH;
  }

  @Override
  protected String getPermission() {
    return "security:user";
  }

  @Override
  protected void setHandler(Router router, EventBus eventBus, JWTAuthHandler jwtAuthHandler,
      Handler<RoutingContext> jwtAuthzHandler) {
    router.route().handler(jwtAuthHandler).failureHandler(userHandler::sendResponseFailure);
    router.post("/:userId/change-password").handler(userHandler::changePassword);

    router.route().handler(jwtAuthzHandler).failureHandler(userHandler::sendResponseFailure);
    router.post("/").handler(userHandler::create);
    router.put("/:userId").handler(userHandler::update);
    router.delete("/:userId").handler(userHandler::delete);
  }
}
