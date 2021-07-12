package com.anasdidi.security.common;

import java.util.Arrays;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import com.anasdidi.security.common.ApplicationConstants.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.eventbus.Message;
import io.vertx.rxjava3.ext.web.RoutingContext;

public abstract class BaseHandler {

  private static final Logger logger = LogManager.getLogger(BaseHandler.class);

  private void logResponse(RoutingContext routingContext, HttpStatus httpStatus) {
    logResponse(routingContext, httpStatus.code);
  }

  private void logResponse(RoutingContext routingContext, int statusCode) {
    String traceId = routingContext.get("traceId");
    HttpMethod method = routingContext.request().method();
    String path = routingContext.request().path();
    long timeTaken = System.currentTimeMillis() - (Long) routingContext.get("startTime");

    logger.info("[logResponse:{}] method={}, path={}, statusCode={}, timeTaken={}ms", traceId,
        method, path, statusCode, timeTaken);
  }

  private void sendResponse(RoutingContext routingContext, String responseBody,
      HttpStatus httpStatus) {
    sendResponse(routingContext, responseBody, httpStatus.code);
  }

  private void sendResponse(RoutingContext routingContext, String responseBody, int statusCode) {
    routingContext.response().setStatusCode(statusCode).headers()
        .addAll(ApplicationConstants.HEADERS);
    routingContext.response().end(responseBody);
  }

  protected final void sendResponse(Single<JsonObject> subscriber, RoutingContext routingContext,
      HttpStatus httpStatus) {
    String traceId = routingContext.get("traceId");

    subscriber.subscribe(responseBody -> {
      if (logger.isDebugEnabled()) {
        JsonObject copy = responseBody.copy();
        Arrays.asList("accessToken").stream().forEach(key -> {
          if (copy.containsKey(key)) {
            copy.put(key, ApplicationUtils.hideValue(copy.getString(key)));
          }
        });

        logger.debug("[sendResponse:{}] responseBody{}", traceId, copy.encode());
      }

      logResponse(routingContext, httpStatus);
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

      logger.error("[sendResponse:{}] responseBody{}", traceId, responseBody);
      logResponse(routingContext, HttpStatus.BAD_REQUEST);
      sendResponse(routingContext, responseBody, HttpStatus.BAD_REQUEST);
    });
  }

  public final void sendResponseFailure(RoutingContext routingContext) {
    String traceId = routingContext.get("traceId");
    int statusCode = routingContext.statusCode();

    try {
      switch (statusCode) {
        case 401:
          throw new ApplicationException(ErrorValue.AUTHENTICATION, traceId,
              "Lacks valid authentication credentials for resource");
      }
    } catch (ApplicationException error) {
      logger.error("[sendResponseFailure:{}] responseBody{}", traceId, error.getMessage());
      logResponse(routingContext, statusCode);
      sendResponse(routingContext, error.getMessage(), statusCode);
    }
  }

  protected final Single<JsonObject> getRequestBody(RoutingContext routingContext,
      String... jsonKeys) {
    return Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();
      String traceId = routingContext.get("traceId");

      if (requestBody == null || requestBody.isEmpty()) {
        if (jsonKeys.length > 0) {
          String error = String.format("Required keys: %s", String.join(",", jsonKeys));
          throw new ApplicationException(ErrorValue.REQUEST_BODY_EMPTY, traceId, error);
        } else {
          requestBody = new JsonObject();
        }
      }
      requestBody.put("traceId", traceId);

      if (logger.isDebugEnabled()) {
        JsonObject copy = requestBody.copy();
        Arrays.asList("password").stream().forEach(key -> {
          if (copy.containsKey(key)) {
            copy.put(key, ApplicationUtils.hideValue(copy.getString(key)));
          }
        });

        logger.debug("[getRequestBody:{}] requestBody{}", traceId, copy.encode());
      }

      return requestBody;
    });
  }

  protected final Single<JsonObject> getRequestBody(Message<Object> request) {
    return Single.fromCallable(() -> {
      JsonObject requestBody = (JsonObject) request.body();
      return requestBody;
    });
  }
}
