package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseHandler;
import com.anasdidi.security.common.ApplicationConstants.HttpStatus;
import com.anasdidi.security.common.BaseValidator.ValidateAction;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

class AuthHandler extends BaseHandler {

  private final AuthService authService;
  private final AuthValidator authValidator;

  AuthHandler(AuthService authService, AuthValidator authValidator) {
    this.authService = authService;
    this.authValidator = authValidator;
  }

  void login(RoutingContext routingContext) {
    Single<JsonObject> subscriber =
        getRequestBody(routingContext, "username", "password").map(json -> AuthVO.fromJson(json))
            .flatMap(vo -> authValidator.validate(vo, ValidateAction.LOGIN))
            .flatMap(vo -> authService.login(vo)).map(vo -> new JsonObject()
                .put("accessToken", vo.accessToken).put("refreshToken", vo.refreshToken));

    sendResponse(subscriber, routingContext, HttpStatus.OK);
  }

  void check(RoutingContext routingContext) {
    Single<JsonObject> subscriber =
        getRequestBody(routingContext).map(json -> AuthVO.fromJson(json, routingContext.user()))
            .flatMap(vo -> authValidator.validate(vo, ValidateAction.CHECK))
            .flatMap(vo -> authService.check(vo));

    sendResponse(subscriber, routingContext, HttpStatus.OK);
  }

  void refresh(RoutingContext routingContext) {
    Single<JsonObject> subscriber =
        getRequestBody(routingContext).map(json -> AuthVO.fromJson(json, routingContext.user()))
            .flatMap(vo -> authValidator.validate(vo, ValidateAction.REFRESH))
            .flatMap(vo -> authService.refresh(vo)).map(vo -> new JsonObject()
                .put("accessToken", vo.accessToken).put("refreshToken", vo.refreshToken));

    sendResponse(subscriber, routingContext, HttpStatus.OK);
  }
}
