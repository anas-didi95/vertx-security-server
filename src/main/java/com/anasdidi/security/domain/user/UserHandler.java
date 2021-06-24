package com.anasdidi.security.domain.user;

import com.anasdidi.security.common.ApplicationConstants.HttpStatus;
import com.anasdidi.security.common.BaseValidator.ValidateAction;
import com.anasdidi.security.common.BaseHandler;
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
            .map(vo -> userValidator.validate(vo, ValidateAction.CREATE))
            .flatMap(vo -> userService.create(vo)).map(id -> new JsonObject().put("id", id));

    sendResponse(subscriber, routingContext, HttpStatus.CREATED);
  }
}
