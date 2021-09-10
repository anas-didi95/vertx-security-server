package com.anasdidi.security.domain.user;

import com.anasdidi.security.common.ApplicationConstants.HttpStatus;
import com.anasdidi.security.common.BaseHandler;
import com.anasdidi.security.common.BaseValidator.ValidateAction;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

class UserHandler extends BaseHandler {

  private final UserService userService;
  private final UserValidator userValidator;

  UserHandler(UserService userService, UserValidator userValidator) {
    this.userService = userService;
    this.userValidator = userValidator;
  }

  void create(RoutingContext routingContext) {
    Single<JsonObject> subscriber =
        getRequestBody(routingContext, "username", "password", "fullName", "email", "telegramId")
            .map(json -> UserVO.fromJson(json))
            .flatMap(vo -> userValidator.validate(vo, ValidateAction.CREATE))
            .flatMap(vo -> userService.create(vo)).map(id -> new JsonObject().put("id", id));

    sendResponse(subscriber, routingContext, HttpStatus.CREATED);
  }

  void update(RoutingContext routingContext) {
    String userId = routingContext.pathParam("userId");

    Single<JsonObject> subscriber =
        getRequestBody(routingContext, "fullName", "email", "telegramId", "version")
            .map(json -> UserVO.fromJson(json, userId))
            .flatMap(vo -> userValidator.validate(vo, ValidateAction.UPDATE))
            .flatMap(vo -> userService.update(vo)).map(id -> new JsonObject().put("id", id));

    sendResponse(subscriber, routingContext, HttpStatus.OK);
  }

  void delete(RoutingContext routingContext) {
    String userId = routingContext.pathParam("userId");

    Single<JsonObject> subscriber =
        getRequestBody(routingContext, "version").map(json -> UserVO.fromJson(json, userId))
            .flatMap(vo -> userValidator.validate(vo, ValidateAction.DELETE))
            .flatMap(vo -> userService.delete(vo)).map(id -> new JsonObject().put("id", id));

    sendResponse(subscriber, routingContext, HttpStatus.OK);
  }

  void changePassword(RoutingContext routingContext) {
    String userId = routingContext.pathParam("userId");

    Single<JsonObject> subscriber =
        getRequestBody(routingContext, "version", "oldPassword", "newPassword")
            .map(json -> UserVO.fromJson(json, userId))
            .flatMap(vo -> userValidator.validate(vo, ValidateAction.CHANGE_PASSWORD))
            .flatMap(vo -> userService.changePassword(vo))
            .map(id -> new JsonObject().put("id", id));

    sendResponse(subscriber, routingContext, HttpStatus.OK);
  }
}
