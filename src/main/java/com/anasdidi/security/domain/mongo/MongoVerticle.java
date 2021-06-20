package com.anasdidi.security.domain.mongo;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.mongo.MongoClient;

public class MongoVerticle extends AbstractVerticle {

  private final static Logger logger = LogManager.getLogger(MongoVerticle.class);
  private final EventBus eventBus;
  private final MongoClient mongoClient;
  private final MongoService mongoService;
  private final MongoEvent mongoEvent;

  public MongoVerticle(EventBus eventBus, MongoClient mongoClient) {
    this.eventBus = eventBus;
    this.mongoClient = mongoClient;
    this.mongoService = new MongoService(mongoClient);
    this.mongoEvent = new MongoEvent(mongoService);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    Future<Void> future = startFuture.future();
    future.compose(v -> createCollections())
        .onComplete(v -> logger.info("[start] Create collections completed"));
    future.compose(v -> setupEvent()).onComplete(v -> logger.info("[start] Setup event completed"));

    startFuture.complete();
  }

  private Future<Void> createCollections() {
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
            promise.complete();
          }, error -> promise.fail(error));
        } else {
          promise.complete();
        }
      }, error -> promise.fail(error));
    });
  }

  private Future<Void> setupEvent() {
    return Future.future(promise -> {
      eventBus.consumer("mongo-create", request -> mongoEvent.create(request));
      promise.complete();
    });
  }
}
