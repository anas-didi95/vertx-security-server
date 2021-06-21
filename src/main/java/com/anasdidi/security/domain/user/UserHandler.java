package com.anasdidi.security.domain.user;

import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.ApplicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

class UserHandler {

  private final Logger logger = LogManager.getLogger(UserHandler.class);
  private final UserService userService;

  UserHandler(UserService userService) {
    this.userService = userService;
  }

  void create(RoutingContext routingContext) {
    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();

      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException("E001", "Request body is empty!", "Required keys {}");
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[create] requestBody {}", requestBody.encode());
      }

      return requestBody;
    }).map(json -> UserVO.fromJson(json)).flatMap(vo -> userService.create(vo))
        .map(id -> new JsonObject().put("id", id));

    subscriber.subscribe(responseBody -> {
      routingContext.response().setStatusCode(201).headers().addAll(ApplicationConstants.HEADERS);
      routingContext.response().end(responseBody.encode());
    }, error -> {
      routingContext.response().setStatusCode(400).headers().addAll(ApplicationConstants.HEADERS);
      routingContext.response().end(error.getMessage());
    });
  }
}
