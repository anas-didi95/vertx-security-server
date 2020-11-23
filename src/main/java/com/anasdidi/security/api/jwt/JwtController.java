package com.anasdidi.security.api.jwt;

import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.web.RoutingContext;

class JwtController extends CommonController {

  private final Logger logger = LogManager.getLogger(JwtController.class);
  private final EventBus eventBus;
  private final JwtService jwtService;
  private final JwtValidator jwtValidator;

  JwtController(EventBus eventBus, JwtService jwtService, JwtValidator jwtValidator) {
    this.eventBus = eventBus;
    this.jwtService = jwtService;
    this.jwtValidator = jwtValidator;
  }

  void doLogin(RoutingContext routingContext) {
    final String TAG = "doLogin";
    String requestId = routingContext.get("requestId");
    JsonObject requestBody = routingContext.getBodyAsJson();
    String username = requestBody != null ? requestBody.getString("username") : "";
    String password = requestBody != null ? requestBody.getString("password") : "";

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", TAG, requestId,
            requestBody.copy().put("password", "*****").encodePrettily());
      }

      return requestBody;
    }).map(json -> JwtVO.fromJson(json))
        .map(vo -> jwtValidator.validate(JwtValidator.Validate.LOGIN, vo, requestId))
        .flatMap(vo -> eventBus.rxRequest(CommonConstants.EVT_USER_GET_BY_USERNAME,
            new JsonObject().put("requestId", requestId).put("username", vo.username)))
        .flatMap(response -> jwtService
            .login(username, password, (JsonObject) response.body(), requestId))
        .map(vo -> new JsonObject()//
            .put("accessToken", vo.accessToken)//
            .put("refreshId", vo.id));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_USER_VALIDATE);
  }

  void doCheck(RoutingContext routingContext) {
    String tag = "doCheck";
    String requestId = routingContext.get("requestId");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] principal={}", tag, requestId, routingContext.user() == null);
      }
      return new JsonObject();
    });

    sendResponse(requestId, subscriber, routingContext, 200, "Ok");
  }

  void doRefresh(RoutingContext routingContext) {
    final String TAG = "doRefresh";
    String requestId = routingContext.get("requestId");
    JsonObject user = routingContext.user().principal();
    JsonObject requestBody = routingContext.getBodyAsJson();

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      } else {
        requestBody//
            .put("username", user.getString("username", ""))
            .put("userId", user.getString("sub", ""));
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
      }

      return requestBody;
    }).map(json -> JwtVO.fromJson(json))
        .map(vo -> jwtValidator.validate(JwtValidator.Validate.REFRESH, vo, requestId))
        .flatMap(vo -> jwtService.refresh(vo, requestId)).map(vo -> new JsonObject()//
            .put("accessToken", vo.accessToken)//
            .put("refreshId", vo.id));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        JwtConstants.MSG_OK_TOKEN_REFRESHED);
  }
}
