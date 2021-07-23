package com.anasdidi.security.domain.graphql;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.anasdidi.security.common.ApplicationUtils;
import com.anasdidi.security.domain.graphql.dto.UserDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

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

  void getUserList(DataFetchingEnvironment env, Promise<List<UserDTO>> promise) {
    UserDTO dto = UserDTO.fromJson(new JsonObject());
    List<UserDTO> resultList = Arrays.asList(dto);

    promise.complete(resultList);
  }
}
