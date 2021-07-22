package com.anasdidi.security.domain.graphql;

import java.util.HashMap;
import java.util.Map;
import com.anasdidi.security.common.ApplicationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Promise;

class GraphqlDataFetcher {

  private static final Logger logger = LogManager.getLogger(GraphqlDataFetcher.class);

  void ping(DataFetchingEnvironment env, Promise<Map<String, Object>> promise) {
    String traceId = ApplicationUtils.getFormattedUUID(env.getExecutionId().toString());
    String value = env.getArgument("value");

    if (logger.isDebugEnabled()) {
      logger.debug("[ping:{}] value={}", traceId, value);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("isSuccess", true);
    result.put("testValue", value);

    promise.complete(result);
  }
}
