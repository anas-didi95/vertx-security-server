package com.anasdidi.security.api.user;

import java.util.stream.Collectors;
import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.RoutingContext;

class UserController extends CommonController {

  private final Logger logger = LogManager.getLogger(UserController.class);
  private final UserValidator userValidator;
  private final UserService userService;

  UserController(UserValidator userValidator, UserService userService) {
    this.userValidator = userValidator;
    this.userService = userService;
  }

  void doCreate(RoutingContext routingContext) {
    final String TAG = "doCreate";
    String requestId = routingContext.get("requestId");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();
      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", TAG, requestId,
            requestBody.copy().put("password", "*****").encodePrettily());
      }

      return requestBody;
    }).map(json -> UserVO.fromJson(json))
        .map(vo -> userValidator.validate(requestId, UserValidator.Validate.CREATE, vo))
        .flatMap(vo -> userService.create(vo, requestId)).map(id -> new JsonObject().put("id", id));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_CREATED,
        CommonConstants.MSG_OK_RECORD_CREATED);
  }

  void doUpdate(RoutingContext routingContext) {
    final String TAG = "doUpdate";
    String requestId = routingContext.get("requestId");
    String paramId = routingContext.request().getParam("id");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();
      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      } else {
        requestBody.put("id", paramId);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
      }

      return requestBody;
    }).map(json -> UserVO.fromJson(json))
        .map(vo -> userValidator.validate(requestId, UserValidator.Validate.UPDATE, vo))
        .flatMap(vo -> userService.update(vo, requestId)).map(id -> new JsonObject().put("id", id));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_RECORD_UPDATE);
  }

  void doDelete(RoutingContext routingContext) {
    final String TAG = "doDelete";
    String requestId = routingContext.get("requestId");
    String paramId = routingContext.request().getParam("id");

    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      JsonObject requestBody = routingContext.getBodyAsJson();
      if (requestBody == null || requestBody.isEmpty()) {
        throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
            CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
      } else {
        requestBody.put("id", paramId);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
      }

      return requestBody;
    }).map(json -> UserVO.fromJson(json))
        .map(vo -> userValidator.validate(requestId, UserValidator.Validate.DELETE, vo))
        .flatMap(vo -> userService.delete(requestId, vo)).map(id -> new JsonObject().put("id", id));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_RECORD_DELETE);
  }

  void reqGetUserByUsername(Message<Object> message) {
    String tag = "reqGetUserByUsername";
    JsonObject body = new JsonObject((String) message.body());
    String requestId = body.getString("requestId");

    Single.just(body)//
        .map(json -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert body to vo", tag, requestId);
          }
          return UserVO.fromJson(json);
        }).flatMap(vo -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Get user by username", tag, requestId);
          }
          return userService.getUserByUsername(vo);
        }).map(vo -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert vo to reply message", tag, requestId);
          }
          return UserUtils.toJson(vo).encode();
        }).subscribe(reply -> {
          message.reply(reply);
        });
  }

  void reqGetUserList(Message<Object> message) {
    String tag = "reqGetUserList";
    JsonObject body = new JsonObject((String) message.body());
    String requestId = body.getString("requestId");

    Single.just(body)//
        .map(json -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert body to vo", tag, requestId);
          }
          return UserVO.fromJson(json);
        })//
        .flatMap(vo -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Get user list", tag, requestId);
          }
          return userService.getUserList(vo);
        })//
        .map(resultList -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert resultList to reply message", tag, requestId);
          }
          return new JsonArray(
              resultList.stream().map(vo -> UserUtils.toJson(vo)).collect(Collectors.toList()));
        }).subscribe(reply -> message.reply(reply.encode()));
  }

  void reqGetUserById(Message<Object> message) {
    String tag = "reqGetUserById";
    JsonObject body = new JsonObject((String) message.body());
    String requestId = body.getString("requestId");

    Single.just(body)//
        .map(json -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert body to vo", tag, requestId);
          }
          return UserVO.fromJson(json);
        })//
        .flatMap(vo -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Get user by id", tag, requestId);
          }
          return userService.getUserById(vo);
        })//
        .map(result -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert result to reply message", tag, requestId);
          }
          return UserUtils.toJson(result);
        }).subscribe(reply -> message.reply(reply.encode()));
  }
}
