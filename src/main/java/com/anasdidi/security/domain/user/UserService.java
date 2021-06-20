package com.anasdidi.security.domain.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.eventbus.EventBus;

class UserService {

  private final static Logger logger = LogManager.getLogger(UserService.class);
  private final EventBus eventBus;

  UserService(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  Single<String> create(UserVO vo) {
    JsonObject requestBody = new JsonObject().put("collection", UserConstants.COLLECTION_NAME)
        .put("document", vo.toJson());

    if (logger.isDebugEnabled()) {
      logger.debug("[create] requestBody {}", requestBody.encode());
    }

    return eventBus.rxRequest("mongo-create", requestBody).map(response -> {
      JsonObject responseBody = (JsonObject) response.body();
      return responseBody.getString("id");
    });
  }
}
