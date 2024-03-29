package com.anasdidi.security.domain.graphql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import com.anasdidi.security.common.ApplicationUtils;
import com.anasdidi.security.domain.graphql.dto.PermissionDTO;
import com.anasdidi.security.domain.graphql.dto.UserDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.eventbus.Message;

class GraphqlDataFetcher {

  private static final Logger logger = LogManager.getLogger(GraphqlDataFetcher.class);
  private final EventBus eventBus;

  public GraphqlDataFetcher(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  void ping(DataFetchingEnvironment env, Promise<Map<String, Object>> promise) {
    String traceId = getTraceId(env);
    String value = env.getArgument("value");

    if (logger.isDebugEnabled()) {
      logger.debug("[ping:{}] value={}", traceId, value);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("isSuccess", true);
    result.put("testValue", value);

    promise.complete(result);
  }

  void getUserList(DataFetchingEnvironment env, Promise<List<UserDTO>> promise) {
    String traceId = getTraceId(env);
    JsonObject query = new JsonObject();

    if (logger.isDebugEnabled()) {
      logger.debug("[getUserList:{}] query{}", traceId, query.encode());
    }

    sendRequest(EventMongo.MONGO_READ_MANY, CollectionRecord.USER, query).subscribe(response -> {
      JsonObject responseBody = getResponseBody(response);
      List<UserDTO> resultList =
          responseBody.getJsonArray("resultList").stream().map(o -> (JsonObject) o)
              .map(json -> UserDTO.fromJson(json)).collect(Collectors.toList());

      if (logger.isDebugEnabled()) {
        logger.debug("[getUserList:{}] resultList.size={}", traceId, resultList.size());
      }

      promise.complete(resultList);
    }, error -> {
      logger.error("[getUserList:{}] query{}", traceId, query.encode());
      logger.error("[getUserList:{}] {}", traceId, error.getMessage());
      promise.fail(error);
    });
  }

  void getUserById(DataFetchingEnvironment env, Promise<UserDTO> promise) {
    String traceId = getTraceId(env);
    String userId = env.getArgument("id");
    JsonObject query = new JsonObject().put("_id", userId);

    if (logger.isDebugEnabled()) {
      logger.debug("[getUserById:{}] query{}", traceId, query.encode());
    }

    sendRequest(EventMongo.MONGO_READ_ONE, CollectionRecord.USER, query).subscribe(response -> {
      JsonObject responseBody = getResponseBody(response);
      UserDTO result = UserDTO.fromJson(responseBody);

      if (logger.isDebugEnabled()) {
        logger.debug("[getUserById:{}] {}", traceId, result);
      }

      promise.complete(result);
    }, error -> {
      logger.error("[getUserById:{}] query{}", traceId, query.encode());
      logger.error("[getUserById:{}] {}", traceId, error.getMessage());
      promise.fail(error);
    });
  }

  void getUserByUsername(DataFetchingEnvironment env, Promise<UserDTO> promise) {
    String traceId = getTraceId(env);
    String username = env.getArgument("username");
    JsonObject query = new JsonObject().put("username", username);

    if (logger.isDebugEnabled()) {
      logger.debug("[getUserByUsername:{}] query{}", traceId, query.encode());
    }

    sendRequest(EventMongo.MONGO_READ_ONE, CollectionRecord.USER, query).subscribe(response -> {
      JsonObject responseBody = getResponseBody(response);
      UserDTO result = UserDTO.fromJson(responseBody);

      if (logger.isDebugEnabled()) {
        logger.debug("[getUserByUsername:{}] {}", traceId, result);
      }

      promise.complete(result);
    }, error -> {
      logger.error("[getUserByUsername:{}] query{}", traceId, query.encode());
      logger.error("[getUserByUsername:{}] {}", traceId, error.getMessage());
      promise.fail(error);
    });
  }

  void getPermissionList(DataFetchingEnvironment env, Promise<List<PermissionDTO>> promise) {
    String traceId = getTraceId(env);
    JsonObject query = new JsonObject();

    if (logger.isDebugEnabled()) {
      logger.debug("[getPermissionList:{}] query{}", traceId, query.encode());
    }

    sendRequest(EventMongo.MONGO_READ_MANY, CollectionRecord.PERMISSION, query)
        .subscribe(response -> {
          JsonObject responseBody = (JsonObject) response.body();
          List<PermissionDTO> resultList =
              responseBody.getJsonArray("resultList").stream().map(o -> (JsonObject) o)
                  .map(json -> PermissionDTO.fromJson(json)).collect(Collectors.toList());

          if (logger.isDebugEnabled()) {
            logger.debug("[getPermissionList:{}] resultList.size={}", traceId, resultList.size());
          }

          promise.complete(resultList);
        }, error -> {
          logger.error("[getPermissionList:{}] query{}", traceId, query.encode());
          logger.error("[getPermissionList:{}] {}", traceId, error.getMessage());
          promise.fail(error);
        });
  }

  void getLastModifiedBy(DataFetchingEnvironment env, Promise<UserDTO> promise) {
    String traceId = getTraceId(env);
    UserDTO source = env.getSource();
    String lastModifiedBy = source.getLastModifiedBy();
    JsonObject query = new JsonObject().put("_id", lastModifiedBy);

    if (logger.isDebugEnabled()) {
      logger.debug("[getLastModifiedBy:{}] query{}", traceId, query);
    }

    sendRequest(EventMongo.MONGO_READ_ONE, CollectionRecord.USER, query).subscribe(response -> {
      JsonObject responseBody = (JsonObject) response.body();
      if (responseBody.isEmpty()) {
        responseBody.put("id", lastModifiedBy);
      }

      UserDTO result = UserDTO.fromJson(responseBody);
      if (logger.isDebugEnabled()) {
        logger.debug("[getLastModifiedBy:{}] {}", traceId, result);
      }

      promise.complete(result);
    }, error -> {
      logger.error("[getLastModifiedBy:{}] query{}", traceId, query);
      logger.error("[getLastModifiedBy:{}] {}", traceId, error.getMessage());
      promise.fail(error);
    });
  }

  private String getTraceId(DataFetchingEnvironment env) {
    return ApplicationUtils.getFormattedUUID(env.getExecutionId().toString());
  }

  private Single<Message<Object>> sendRequest(EventMongo event, CollectionRecord collection,
      JsonObject query) {
    JsonObject requestBody =
        new JsonObject().put("collection", collection.name).put("query", query);
    return eventBus.rxRequest(event.toString(), requestBody);
  }

  private JsonObject getResponseBody(Message<Object> response) {
    JsonObject responseBody = (JsonObject) response.body();
    return responseBody;
  }
}
