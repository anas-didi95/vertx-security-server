package com.anasdidi.security.common;

import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.core.eventbus.Message;

public abstract class BaseService {

  public EventBus eventBus;

  public final void setEventBus(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  protected final Single<Message<Object>> sendRequest(EventMongo event, CollectionRecord collection,
      JsonObject query, JsonObject document, Long version) {
    JsonObject requestBody = new JsonObject().put("collection", collection.name).put("query", query)
        .put("document", document).put("version", version);
    return eventBus.rxRequest(event.address, requestBody);
  }
}
