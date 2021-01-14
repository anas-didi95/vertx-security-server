package com.anasdidi.security.api.jwt;

import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonController;
import com.anasdidi.security.common.CommonUtils;
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
            .put("refreshToken", vo.refreshToken));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_USER_VALIDATE);
  }

  void doCheck(RoutingContext routingContext) {
    final String TAG = "doCheck";
    String requestId = routingContext.get("requestId");

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] principal\n{}", TAG, requestId,
          routingContext.user().principal().encodePrettily());
    }

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject principal = routingContext.user().principal();

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] principal\n{}", TAG, requestId, principal.encodePrettily());
      }

      return principal;
    }).map(json -> new JsonObject()//
        .put("userId", json.getString("sub")));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        JwtConstants.MSG_OK_TOKEN_DECODED);
  }

  void doRefresh(RoutingContext routingContext) {
    final String TAG = "doRefresh";
    String requestId = routingContext.get("requestId");
    String userId = CommonUtils.getUserIdFromToken(routingContext.user());

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();

      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      } else {
        requestBody.put("userId", userId);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n", TAG, requestId, requestBody.encodePrettily());
      }

      return requestBody;
    }).map(json -> JwtVO.fromJson(json))//
        .map(vo -> jwtValidator.validate(JwtValidator.Validate.REFRESH, vo, requestId))
        .flatMap(vo -> jwtService.refresh(vo, requestId)).map(vo -> new JsonObject()//
            .put("accessToken", vo.accessToken)//
            .put("refreshToken", vo.refreshToken));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        JwtConstants.MSG_OK_TOKEN_REFRESHED);
  }

  void doLogout(RoutingContext routingContext) {
    final String TAG = "doLogout";
    String requestId = routingContext.get("requestId");
    String userId = CommonUtils.getUserIdFromToken(routingContext.user());

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject json = new JsonObject().put("userId", userId);

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] json\n{}", TAG, requestId, json.encodePrettily());
      }

      return json;
    }).map(json -> JwtVO.fromJson(json))//
        .flatMap(vo -> jwtService.logout(vo, requestId)).map(vo -> {
          return new JsonObject()//
              .put("lastTokenDate", vo.issuedDate != null ? vo.issuedDate : "");
        });

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_USER_LOGOUT);
  }
}
