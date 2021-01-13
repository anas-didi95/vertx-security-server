package com.anasdidi.security.api.graphql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.anasdidi.security.api.graphql.dto.UserDTO;
import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;

class GraphqlDataFetcher {

  private final Logger logger = LogManager.getLogger(GraphqlDataFetcher.class);
  private final EventBus eventBus;

  GraphqlDataFetcher(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  void ping(DataFetchingEnvironment env, Promise<Map<String, Object>> promise) {
    final String TAG = "ping";
    String requestId = CommonUtils.generateUUID(env.getExecutionId());
    String value = env.getArgument("value");

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] value={}", TAG, requestId, value);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("isSuccess", true);
    result.put("testValue", value);

    promise.complete(result);
  }

  void getUserList(DataFetchingEnvironment env, Promise<List<UserDTO>> future) {
    final String TAG = "getUserList";
    String requestId = CommonUtils.generateUUID(env.getExecutionId());
    JsonObject requestBody = new JsonObject()//
        .put("requestId", requestId);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
    }

    eventBus.rxRequest(CommonConstants.EVT_USER_GET_LIST, requestBody).subscribe(response -> {
      JsonArray responseBody = (JsonArray) response.body();

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] responseBody\n{}", TAG, requestId, responseBody.encodePrettily());
      }

      future.complete(responseBody.stream().map(o -> (JsonObject) o)
          .map(json -> UserDTO.fromJson(json)).collect(Collectors.toList()));
    }, e -> future.fail(e));
  }

  void getUserById(DataFetchingEnvironment env, Promise<UserDTO> future) {
    final String TAG = "getUserById";
    String requestId = CommonUtils.generateUUID(env.getExecutionId());
    JsonObject requestBody = new JsonObject()//
        .put("requestId", requestId)//
        .put("id", (String) env.getArgument("id"));

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
    }

    eventBus.rxRequest(CommonConstants.EVT_USER_GET_BY_ID, requestBody).subscribe(response -> {
      JsonObject responseBody = (JsonObject) response.body();

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] responseBody\n{}", TAG, requestId, responseBody.encodePrettily());
      }

      future.complete(UserDTO.fromJson(responseBody));
    }, e -> future.fail(e));
  }

  void getUserByUsername(DataFetchingEnvironment env, Promise<UserDTO> future) {
    final String TAG = "getUserByUsername";
    String requestId = CommonUtils.generateUUID(env.getExecutionId());
    JsonObject requestBody = new JsonObject()//
        .put("requestId", requestId)//
        .put("username", (String) env.getArgument("username"));

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
    }

    eventBus.rxRequest(CommonConstants.EVT_USER_GET_BY_USERNAME, requestBody)
        .subscribe(response -> {
          JsonObject responseBody = (JsonObject) response.body();

          if (logger.isDebugEnabled()) {
            logger.debug("[{}:{}] responseBody\n{}", TAG, requestId, responseBody.encodePrettily());
          }

          future.complete(UserDTO.fromJson(responseBody));
        }, e -> future.fail(e));
  }

  void getLastModifiedBy(DataFetchingEnvironment env, Promise<UserDTO> promise) {
    final String TAG = "getLastModifiedBy";
    String requestId = CommonUtils.generateUUID(env.getExecutionId());
    UserDTO dto = env.getSource();
    String id = dto.getLastModifiedBy();
    JsonObject requestBody = new JsonObject()//
        .put("requestId", requestId)//
        .put("id", id);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] requestBody\n{}", TAG, requestId, requestBody.encodePrettily());
    }

    eventBus.rxRequest(CommonConstants.EVT_USER_GET_BY_ID, requestBody).subscribe(response -> {
      JsonObject responseBody = (JsonObject) response.body();
      String respId = responseBody.getString("id");

      if (respId == null || respId.isBlank()) {
        responseBody.put("id", id);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("[{}:{}] responseBody\n{}", TAG, requestId, responseBody.encodePrettily());
      }

      promise.complete(UserDTO.fromJson(responseBody));
    });
  }

  void getPermissionList(DataFetchingEnvironment env, Promise<List<String>> promise) {
    final String TAG = "getLastModifiedBy";
    String requestId = CommonUtils.generateUUID(env.getExecutionId());

    List<String> permissionList = new ArrayList<>();
    permissionList.add(CommonConstants.PERMISSION_USER_WRITE);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] permissionList={}", TAG, requestId, permissionList);
    }

    promise.complete(permissionList);
  }
}
