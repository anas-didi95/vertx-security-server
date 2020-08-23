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
    final String TAG = "[create]";

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      logger.info(TAG + " Get request body");
      return routingContext.getBodyAsJson();
    }).map(json -> {
      logger.info(TAG + " Convert request body to vo");
      logger.debug(TAG + " json=" + json.encode());
      return UserUtils.toVO(json);
    }).flatMap(vo -> {
      logger.info(TAG + " Save vo to database");
      return userService.create(vo);
    }).map(id -> {
      logger.info(TAG + " Construct response data");
      return new JsonObject().put("id", id);
    });

    sendResponse(subscriber, routingContext, 201, "Record successfully created.");
  }
}
