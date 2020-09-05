package com.anasdidi.security.api.graphql;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  void getUserList(DataFetchingEnvironment env, Promise<List<Map<String, Object>>> future) {
    String tag = "getUserList";
    String requestId = CommonUtils.generateId();
    JsonObject message = new JsonObject()//
        .put("requestId", requestId);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] message\n{}", tag, requestId, message.encodePrettily());
    }

    eventBus.rxRequest(CommonConstants.EVT_USER_GET_LIST, message.encode()).subscribe(reply -> {
      JsonArray resultList = new JsonArray((String) reply.body());
      future.complete(
          resultList.stream().map(o -> (JsonObject) o).map(json -> json.getMap()).collect(Collectors.toList()));
    }, e -> future.fail(e));
  }

  void getUserById(DataFetchingEnvironment env, Promise<Map<String, Object>> future) {
    String tag = "getUserById";
    String requestId = CommonUtils.generateId();
    String id = env.getArgument("id");
    JsonObject message = new JsonObject()//
        .put("requestId", requestId)//
        .put("id", id);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}:{}] message\n{}", tag, requestId, message.encodePrettily());
    }

    eventBus.rxRequest(CommonConstants.EVT_USER_GET_BY_ID, message.encode()).subscribe(reply -> {
      JsonObject body = new JsonObject((String) reply.body());
      future.complete(body.getMap());
    }, e -> future.fail(e));
  }
}
