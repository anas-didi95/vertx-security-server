package com.anasdidi.security.api.jwt;

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

  JwtController(EventBus eventBus, JwtService jwtService) {
    this.eventBus = eventBus;
    this.jwtService = jwtService;
  }

  void validate(RoutingContext routingContext) {
    String tag = "validate";
    String requestId = routingContext.get("requestId");

    JsonObject requestBody = routingContext.getBodyAsJson();
    String username = requestBody.getString("username");
    String password = requestBody.getString("password");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get request body", tag, requestId);
      }
      return routingContext.getBodyAsJson();
    }).flatMap(json -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Request event to get user", tag, requestId);
      }
      JsonObject message = new JsonObject().put("username", json.getString("username"));
      return eventBus.rxRequest("user-read-username", message.encode());
    }).map(msg -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get reply from event", tag, requestId);
      }
      JsonObject user = new JsonObject((String) msg.body());
      return user;
    }).flatMap(user -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Validate user", tag, requestId);
      }
      return jwtService.validate(username, password, user);
    }).map(accessToken -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Construct response body", tag, requestId);
      }
      return new JsonObject().put("accessToken", accessToken);
    });

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_USER_VALIDATE);
  }
}
