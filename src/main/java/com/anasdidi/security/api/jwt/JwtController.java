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
    String tag = "doLogin";
    String requestId = routingContext.get("requestId");

    JsonObject requestBody = routingContext.getBodyAsJson();
    String username = requestBody != null ? requestBody.getString("username") : "";
    String password = requestBody != null ? requestBody.getString("password") : "";

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get request body", tag, requestId);
      }

      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", tag, requestId,
            requestBody.copy().put("password", "-").encodePrettily());
      }

      return requestBody;
    }).map(json -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Convert to vo", tag, requestId);
      }
      return JwtUtils.toVO(json);
    }).map(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Validate vo", tag, requestId);
      }
      jwtValidator.validate(requestId, JwtValidator.Validate.LOGIN, vo);
      return vo;
    }).flatMap(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Request event to get user", tag, requestId);
      }
      return eventBus.rxRequest(CommonConstants.EVT_USER_GET_BY_USERNAME, new JsonObject()//
          .put("requestId", requestId)//
          .put("username", vo.username)//
          .encode());
    }).map(msg -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get reply from event", tag, requestId);
      }
      return new JsonObject((String) msg.body());
    }).flatMap(user -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Login user", tag, requestId);
      }
      return jwtService.login(requestId, username, password, user);
    }).map(jwt -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Construct response body", tag, requestId);
      }
      return new JsonObject()//
          .put("accessToken", jwt.getString("accessToken"))//
          .put("refreshId", jwt.getString("id"));
    });

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
}
