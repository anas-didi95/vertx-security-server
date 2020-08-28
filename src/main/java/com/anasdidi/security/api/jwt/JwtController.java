package com.anasdidi.security.api.jwt;

import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonController;
import com.anasdidi.security.common.CommonUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

class JwtController extends CommonController {

  private final Logger logger = LogManager.getLogger(JwtController.class);

  void validate(RoutingContext routingContext) {
    String tag = "validate";
    String requestId = routingContext.get("requestId");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get request body", tag, requestId);
      }
      return routingContext.getBodyAsJson();
    }).map(json -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Construct response body", tag, requestId);
      }
      return new JsonObject().put("accessToken", CommonUtils.generateId());
    });

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_USER_VALIDATE);
  }
}
