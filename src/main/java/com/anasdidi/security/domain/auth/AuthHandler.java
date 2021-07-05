package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseHandler;
import com.anasdidi.security.common.ApplicationConstants.HttpStatus;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

class AuthHandler extends BaseHandler {

  void login(RoutingContext routingContext) {
    Single<JsonObject> subscriber = getRequestBody(routingContext)
        .map(json -> new JsonObject().put("accessToken", System.currentTimeMillis()));

    sendResponse(subscriber, routingContext, HttpStatus.OK);
  }
}
