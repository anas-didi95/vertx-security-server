package com.anasdidi.security.api.user;

import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

class UserController extends CommonController {

  private final Logger logger = LogManager.getLogger(UserController.class);
  private final UserValidator userValidator;
  private final UserService userService;

  UserController(UserValidator userValidator, UserService userService) {
    this.userValidator = userValidator;
    this.userService = userService;
  }

  void create(RoutingContext routingContext) {
    String tag = "create";
    String requestId = routingContext.get("requestId");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get request body", tag, requestId);
      }

      JsonObject requestBody = routingContext.getBodyAsJson();
      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", tag, requestId, requestBody.encodePrettily());
      }

      return requestBody;
    }).map(json -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Convert to vo", tag, requestId);
      }
      return UserUtils.toVO(json);
    }).map(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Validate vo", tag, requestId);
      }
      userValidator.validate(requestId, UserValidator.Validate.CREATE, vo);
      return vo;
    }).flatMap(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Save vo to database", tag, requestId);
      }
      return userService.create(requestId, vo);
    }).map(id -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Construct response data", tag, requestId);
      }
      return new JsonObject().put("id", id);
    });

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_CREATED,
        CommonConstants.MSG_OK_RECORD_CREATED);
  }

  void update(RoutingContext routingContext) {
    String tag = "update";
    String requestId = routingContext.get("requestId");

    String paramId = routingContext.request().getParam("id");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get request body", tag, requestId);
      }

      JsonObject requestBody = routingContext.getBodyAsJson();
      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      } else {
        requestBody.put("id", paramId);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", tag, requestId, requestBody.encodePrettily());
      }

      return routingContext.getBodyAsJson();
    }).map(json -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Convert to vo", tag, requestId);
      }
      return UserUtils.toVO(json);
    }).map(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Validate vo", tag, requestId);
      }
      userValidator.validate(requestId, UserValidator.Validate.UPDATE, vo);
      return vo;
    }).flatMap(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Update vo to database", tag, requestId);
      }
      return userService.update(requestId, vo);
    }).map(id -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Construct response body", tag, requestId);
      }
      return new JsonObject().put("id", id);
    });

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_RECORD_UPDATE);
  }

  void delete(RoutingContext routingContext) {
    String tag = "delete";
    String requestId = routingContext.get("requestId");

    String paramId = routingContext.request().getParam("id");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Get request body", tag, requestId);
      }

      JsonObject requestBody = routingContext.getBodyAsJson();
      requestBody.put("id", paramId);

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", tag, requestId, requestBody.encodePrettily());
      }

      return requestBody;
    }).map(json -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Convert to vo", tag, requestId);
      }
      return UserUtils.toVO(json);
    }).map(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Validate vo", tag, requestId);
      }
      userValidator.validate(requestId, UserValidator.Validate.DELETE, vo);
      return vo;
    }).flatMap(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Delete vo from database", tag, requestId);
      }
      return userService.delete(requestId, vo);
    }).map(id -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Construct response body", tag, requestId);
      }
      return new JsonObject().put("id", id);
    });

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK, CommonConstants.MSG_OK_RECORD_DELETE);
  }
}
