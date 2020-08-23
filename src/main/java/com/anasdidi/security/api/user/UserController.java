package com.anasdidi.security.api.user;

import com.anasdidi.security.common.CommonController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

public class UserController extends CommonController {

  private final Logger logger = LogManager.getLogger(UserController.class);
  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  void create(RoutingContext routingContext) {
    String tag = "create";
    String requestId = routingContext.get("requestId");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      logger.info("[{}:{}] Get request body", tag, requestId);
      return routingContext.getBodyAsJson();
    }).map(json -> {
      logger.info("[{}:{}] Convert request body to vo", tag, requestId);
      return UserUtils.toVO(json);
    }).flatMap(vo -> {
      logger.info("[{}:{}] Save vo to database", tag, requestId);
      return userService.create(requestId, vo);
    }).map(id -> {
      logger.info("[{}:{}] Construct response data", tag, requestId);
      return new JsonObject().put("id", id);
    });

    sendResponse(subscriber, routingContext, 201, "Record successfully created.");
  }
}
