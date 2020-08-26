package com.anasdidi.security.api.user;

import com.anasdidi.security.common.CommonController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

class UserController extends CommonController {

  private final Logger logger = LogManager.getLogger(UserController.class);
  private final UserValidator userValidator;
  private final UserService userService;

  UserController(UserValidator userValidator, UserService userService) {
    this.userValidator = userValidator;
    this.userService = userService;
  }

  void create(RoutingContext routingContext) {
    String tag = "create";
    String requestId = routingContext.get("requestId");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get request body", tag, requestId);
        logger.debug("[{}:{}] requestBody\n{}", tag, requestId, requestBody.encodePrettily());
      }
      return requestBody;
    }).map(json -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Convert to vo", tag, requestId);
      }
      return UserUtils.toVO(json);
    }).map(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Validate vo", tag, requestId);
      }
      userValidator.validate(requestId, UserValidator.Validate.CREATE, vo);
      return vo;
    }).flatMap(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Save vo to database", tag, requestId);
      }
      return userService.create(requestId, vo);
    }).map(id -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Construct response data", tag, requestId);
      }
      return new JsonObject().put("id", id);
    });

    sendResponse(requestId, subscriber, routingContext, 201, "Record successfully created.");
  }

  void update(RoutingContext routingContext) {
    String tag = "update";
    String requestId = routingContext.get("requestId");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get request body", tag, requestId);
        logger.debug("[{}:{}] requestBody\n{}", tag, requestId, requestBody.encodePrettily());
      }
      return routingContext.getBodyAsJson();
    }).map(json -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Convert to vo", tag, requestId);
      }
      return UserUtils.toVO(json);
    }).flatMap(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Update vo to database", tag, requestId);
      }
      return userService.update(requestId, vo);
    }).map(id -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Construct response body", tag, requestId);
      }
      return new JsonObject().put("id", id);
    });

    sendResponse(requestId, subscriber, routingContext, 200, "Record successfully updated.");
  }
}
