package com.anasdidi.security.api.graphql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.eventbus.EventBus;

class GraphqlDataFetcher {

  private final Logger logger = LogManager.getLogger(GraphqlDataFetcher.class);
  private final EventBus eventBus;

  GraphqlDataFetcher(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  void getUser(DataFetchingEnvironment env, Promise<List<Map<String, Object>>> future) {
    String tag = "getUser";
    if (logger.isDebugEnabled()) {
      logger.debug("[{}] Start ...", tag);
    }

    Map<String, Object> data = new HashMap<>();
    data.put("id", "Hello world");

    List<Map<String, Object>> resultList = new ArrayList<>();
    resultList.add(data);

    if (logger.isDebugEnabled()) {
      logger.debug("[{}} End ... resultList.size={}", tag, (resultList == null ? -1 : resultList.size()));
    }
    future.complete(resultList);
  }
}
