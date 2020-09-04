package com.anasdidi.security.api.user;

import java.util.stream.Collectors;

import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

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
    String tag = "doCreate";
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
        logger.debug("[{}:{}] requestBody\n{}", tag, requestId,
            requestBody.copy().put("password", "-").encodePrettily());
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
    }).map(vo -> {
      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] Encrypt password", tag, requestId);
      }
      vo.password = BCrypt.hashpw(vo.password, BCrypt.gensalt());
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

  void doUpdate(RoutingContext routingContext) {
    String tag = "doUpdate";
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

  void doDelete(RoutingContext routingContext) {
    String tag = "doDelete";
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

    sendResponse(requestId, subscriber, routingContext, CommonConstants.STATUS_CODE_OK,
        CommonConstants.MSG_OK_RECORD_DELETE);
  }

  void reqUserReadUsername(Message<Object> message) {
    String tag = "reqUserReadUsername";
    JsonObject body = new JsonObject((String) message.body());
    String requestId = body.getString("requestId");

    Single.just(body)//
        .map(json -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert body to vo", tag, requestId);
          }
          return UserUtils.toVO(json);
        }).flatMap(vo -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Get user by username", tag, requestId);
          }
          return userService.readByUsername(vo);
        }).map(vo -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert vo to reply message", tag, requestId);
          }
          return UserUtils.toJson(vo).encode();
        }).subscribe(reply -> {
          message.reply(reply);
        });
  }

  void reqReadUser(Message<Object> message) {
    String tag = "reqReadUser";
    JsonObject body = new JsonObject((String) message.body());
    String requestId = body.getString("requestId");

    Single.just(body)//
        .map(json -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert body to vo", tag, requestId);
          }
          return UserUtils.toVO(json);
        })//
        .flatMap(vo -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Get user", tag, requestId);
          }
          return userService.read(vo);
        })//
        .map(resultList -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] Convert resultList to reply message", tag, requestId);
          }
          return new JsonArray(resultList.stream().map(vo -> UserUtils.toJson(vo)).collect(Collectors.toList()));
        }).subscribe(reply -> message.reply(reply.encode()));
  }
}
