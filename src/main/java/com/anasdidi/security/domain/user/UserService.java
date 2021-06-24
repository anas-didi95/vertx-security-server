package com.anasdidi.security.domain.user;

import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import com.anasdidi.security.common.ApplicationConstants.EventValue;
import com.anasdidi.security.common.ApplicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.eventbus.EventBus;

class UserService {

  private final static Logger logger = LogManager.getLogger(UserService.class);
  private EventBus eventBus;

  void setEventBus(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  Single<String> create(UserVO vo) {
    JsonObject requestBody =
        new JsonObject().put("collection", CollectionRecord.USER.name).put("document", vo.toJson());

    if (logger.isDebugEnabled()) {
      logger.debug("[create] requestBody {}", requestBody.encode());
    }

    return eventBus.rxRequest(EventValue.MONGO_CREATE.address, requestBody).doOnError(error -> {
      logger.error("[create] requestBody {}", requestBody.encode());
      logger.error("[create] {}", error.getMessage());
      error.addSuppressed(new ApplicationException(ErrorValue.USER_CREATE,
          "Unable to create user with username: " + vo.username));
    }).map(response -> {
      JsonObject responseBody = (JsonObject) response.body();
      return responseBody.getString("id");
    });
  }
}
