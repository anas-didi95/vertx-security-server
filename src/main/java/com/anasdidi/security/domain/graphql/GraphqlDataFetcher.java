package com.anasdidi.security.domain.graphql;

import java.util.HashMap;
import java.util.Map;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Promise;

class GraphqlDataFetcher {

  void ping(DataFetchingEnvironment env, Promise<Map<String, Object>> promise) {
    // final String TAG = "ping";
    // String requestId = CommonUtils.generateUUID(env.getExecutionId());
    String value = env.getArgument("value");

    // if (logger.isDebugEnabled()) {
    // logger.debug("[{}:{}] value={}", TAG, requestId, value);
    // }

    Map<String, Object> result = new HashMap<>();
    result.put("isSuccess", true);
    result.put("testValue", value);

    promise.complete(result);
  }
}
