package com.anasdidi.security.common;

import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

public abstract class BaseHandler {

  private static final Logger logger = LogManager.getLogger(BaseHandler.class);

  protected void sendResponse(Single<JsonObject> subscriber, RoutingContext routingContext,
      int statusCode) {
    subscriber.subscribe(responseBody -> {
      logger.info("[sendResponse] Success: statusCode={}", statusCode);
      routingContext.response().setStatusCode(statusCode).headers()
          .addAll(ApplicationConstants.HEADERS);
      routingContext.response().end(responseBody.encode());
    }, error -> {
      String responseBody = null;

      if (error instanceof ApplicationException) {
        responseBody = error.getMessage();
      } else if (error.getSuppressed().length > 0) {
        responseBody = Arrays.asList(error.getSuppressed()).stream()
            .filter(e -> e instanceof ApplicationException).findFirst().get().getMessage();
      } else {
        responseBody = new JsonObject().put("message", error.getMessage()).encode();
      }

      logger.error("[sendResponse] Error! {}", responseBody);
      routingContext.response().setStatusCode(400).headers().addAll(ApplicationConstants.HEADERS);
      routingContext.response().end(responseBody);
    });
  }

  protected Single<JsonObject> getRequestBody(RoutingContext routingContext, String... jsonKeys) {
    return Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();

      if (requestBody == null || requestBody.isEmpty()) {
        String error = String.format("Required keys: %s", String.join(",", jsonKeys));
        throw new ApplicationException(ApplicationConstants.ErrorValue.REQUEST_BODY_EMPTY, error);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[create] requestBody {}", requestBody.encode());
      }

      return requestBody;
    });
  }
}
