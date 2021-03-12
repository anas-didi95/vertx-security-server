package com.anasdidi.security.api.user;

import java.util.stream.Collectors;
import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonController;
import com.anasdidi.security.common.CommonUtils;
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
    String userId = CommonUtils.getUserIdFromToken(routingContext.user());

    Single<JsonObject> subscriber = CommonUtils
        .isAuthorized(routingContext.user(), CommonConstants.PERMISSION_USER_WRITE, requestId)
        .map(user -> {
          JsonObject requestBody = routingContext.getBodyAsJson();
          if (requestBody == null || requestBody.isEmpty()) {
            throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
                CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
          } else {
            requestBody.put("lastModifiedBy", userId);
          }

          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] requestBody\n{}", TAG, requestId,
                requestBody.copy().put("password", "*****").encodePrettily());
          }

          return requestBody;
        }).map(json -> UserVO.fromJson(json))
        .map(vo -> userValidator.validate(UserValidator.Validate.CREATE, vo, requestId))
        .flatMap(vo -> userService.create(vo, requestId)).map(id -> new JsonObject().put("id", id));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_CREATED,
        CommonConstants.MSG_OK_RECORD_CREATED);
  }

  void doUpdate(RoutingContext routingContext) {
    final String TAG = "doUpdate";
    String requestId = routingContext.get("requestId");
    String paramId = routingContext.request().getParam("id");
    String userId = CommonUtils.getUserIdFromToken(routingContext.user());

    Single<JsonObject> subscriber = CommonUtils
        .isAuthorized(routingContext.user(), CommonConstants.PERMISSION_USER_WRITE, requestId)
        .map(user -> {
          JsonObject requestBody = routingContext.getBodyAsJson();
          if (requestBody == null || requestBody.isEmpty()) {
            throw new ApplicationException(CommonConstants.MSG_ERR_REQUEST_FAILED, requestId,
                CommonConstants.MSG_ERR_REQUEST_BODY_EMPTY);
          } else {
            requestBody.put("id", paramId).put("lastModifiedBy", userId);
          }

          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
          }

          return requestBody;
        }).map(json -> UserVO.fromJson(json))
        .map(vo -> userValidator.validate(UserValidator.Validate.UPDATE, vo, requestId))
        .flatMap(vo -> userService.update(vo, requestId)).map(id -> new JsonObject().put("id", id));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_RECORD_UPDATE);
  }

  void doDelete(RoutingContext routingContext) {
    final String TAG = "doDelete";
    String requestId = routingContext.get("requestId");
    String paramId = routingContext.request().getParam("id");

    Single<JsonObject> subscriber = CommonUtils
        .isAuthorized(routingContext.user(), CommonConstants.PERMISSION_USER_WRITE, requestId)
        .map(user -> {
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
        .map(vo -> userValidator.validate(UserValidator.Validate.DELETE, vo, requestId))
        .flatMap(vo -> userService.delete(vo, requestId)).map(id -> new JsonObject().put("id", id));

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_RECORD_DELETE);
  }

  void doChangePassword(RoutingContext routingContext) {
    // final String TAG = "doUpdate";
    String requestId = routingContext.get("requestId");

    Single<JsonObject> subscriber = Single.just(new JsonObject());

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        UserConstants.MSG_OK_USER_CHANGE_PASSWORD);
  }

  void doGetUserByUsername(Message<Object> request) {
    final String TAG = "doGetUserByUsername";
    JsonObject requestBody = (JsonObject) request.body();
    String requestId = requestBody.getString("requestId");

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
    }

    Single.just(requestBody)//
        .map(json -> UserVO.fromJson(json)).flatMap(vo -> userService.getUserByUsername(vo))
        .map(vo -> UserVO.toJson(vo)).subscribe(response -> request.reply(response));
  }

  void doGetUserList(Message<Object> request) {
    final String TAG = "doGetUserList";
    JsonObject requestBody = (JsonObject) request.body();
    String requestId = requestBody.getString("requestId");

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
    }

    Single.just(requestBody)//
        .map(json -> UserVO.fromJson(json))//
        .flatMap(vo -> userService.getUserList(vo))//
        .map(resultList -> new JsonArray(
            resultList.stream().map(vo -> UserVO.toJson(vo)).collect(Collectors.toList())))
        .subscribe(response -> request.reply(response));
  }

  void doGetUserById(Message<Object> request) {
    final String TAG = "doGetUserById";
    JsonObject requestBody = (JsonObject) request.body();
    String requestId = requestBody.getString("requestId");

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
    }

    Single.just(requestBody)//
        .map(json -> UserVO.fromJson(json))//
        .flatMap(vo -> userService.getUserById(vo))//
        .map(result -> UserVO.toJson(result)).subscribe(response -> request.reply(response));
  }
}
