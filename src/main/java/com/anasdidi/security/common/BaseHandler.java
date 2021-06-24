package com.anasdidi.security.common;

import java.util.Arrays;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import com.anasdidi.security.common.ApplicationConstants.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

public abstract class BaseHandler {

  private static final Logger logger = LogManager.getLogger(BaseHandler.class);

  private void sendResponse(RoutingContext routingContext, String responseBody,
      HttpStatus httpStatus) {
    routingContext.response().setStatusCode(httpStatus.code).headers()
        .addAll(ApplicationConstants.HEADERS);
    routingContext.response().end(responseBody);
  }

  protected void sendResponse(Single<JsonObject> subscriber, RoutingContext routingContext,
      HttpStatus httpStatus) {
    String traceId = routingContext.get("traceId");
    long timeTaken = System.currentTimeMillis() - (Long) routingContext.get("startTime");

    subscriber.subscribe(responseBody -> {
      logger.info("[sendResponse:{}] Success ({}ms): httpStatus={}", traceId, timeTaken,
          httpStatus);
      sendResponse(routingContext, responseBody.encode(), httpStatus);
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

      logger.error("[sendResponse:{}] Error ({}ms): {}", traceId, timeTaken, responseBody);
      sendResponse(routingContext, responseBody, HttpStatus.BAD_REQUEST);
    });
  }

  protected Single<JsonObject> getRequestBody(RoutingContext routingContext, String... jsonKeys) {
    return Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();
      String traceId = routingContext.get("traceId");

      if (requestBody == null || requestBody.isEmpty()) {
        String error = String.format("Required keys: %s", String.join(",", jsonKeys));
        throw new ApplicationException(ErrorValue.REQUEST_BODY_EMPTY, traceId, error);
      } else {
        requestBody.put("traceId", traceId);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[create:{}] requestBody {}", traceId, requestBody.encode());
      }

      return requestBody;
    });
  }
}
