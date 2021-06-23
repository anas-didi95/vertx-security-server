package com.anasdidi.security.domain.mongo;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.mongo.MongoClient;

public class MongoVerticle extends AbstractVerticle {

  private final static Logger logger = LogManager.getLogger(MongoVerticle.class);
  private final MongoService mongoService;
  private final MongoEvent mongoEvent;

  public MongoVerticle() {
    this.mongoService = new MongoService();
    this.mongoEvent = new MongoEvent(mongoService);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    Future<Void> future = startFuture.future();
    future.compose(v -> getMongoClient())
        .onComplete(v -> logger.info("[start] Get mongo client completed"))
        .compose(mongoClient -> createCollections(mongoClient))
        .onComplete(v -> logger.info("[start] Create collections completed"));
    future.compose(v -> setupEvent()).onComplete(v -> logger.info("[start] Setup event completed"));

    startFuture.complete();
  }

  private Future<MongoClient> getMongoClient() {
    return Future.future(promise -> {
      ApplicationConfig config = ApplicationConfig.instance();
      MongoClient mongoClient = MongoClient.create(vertx,
          new JsonObject().put("connection_string", config.getMongoConnectionString()));

      mongoService.setMongoClient(mongoClient);
      promise.complete(mongoClient);
    });
  }

  private Future<MongoClient> createCollections(MongoClient mongoClient) {
    return Future.future(promise -> {
      mongoClient.rxGetCollections().subscribe(collectionList -> {
        List<Completable> createCollection = new ArrayList<>();
        if (!collectionList.contains("users")) {
          createCollection.add(mongoClient.rxCreateCollection("users"));
        }

        if (!createCollection.isEmpty()) {
          Completable.merge(createCollection).subscribe(() -> {
            logger.info("[createCollections] Total collection created: {}",
                createCollection.size());
            promise.complete(mongoClient);
          }, error -> promise.fail(error));
        } else {
          promise.complete();
        }
      }, error -> promise.fail(error));
    });
  }

  private Future<Void> setupEvent() {
    return Future.future(promise -> {
      vertx.eventBus().consumer(ApplicationConstants.Event.MONGO_CREATE.address,
          request -> mongoEvent.create(request));
      promise.complete();
    });
  }
}
