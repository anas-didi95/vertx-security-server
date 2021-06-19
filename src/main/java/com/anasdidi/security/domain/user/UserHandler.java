package com.anasdidi.security.domain.user;

import io.vertx.rxjava3.ext.web.RoutingContext;

class UserHandler {

  void create(RoutingContext routingContext) {
    routingContext.response().putHeader("Content-Type", "application/json").setStatusCode(201)
        .end();
  }
}
