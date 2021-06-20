package com.anasdidi.security.domain.mongo;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.mongo.MongoClient;

public class MongoVerticle extends AbstractVerticle {

  private final static Logger logger = LogManager.getLogger(MongoVerticle.class);
  private final EventBus eventBus;
  private final MongoService mongoService;

  public MongoVerticle(EventBus eventBus, MongoClient mongoClient) {
    this.eventBus = eventBus;
    this.mongoService = new MongoService(mongoClient);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    MongoClient mongoClient = MongoClient.create(vertx, new JsonObject()//
        .put("connection_string", "mongodb://mongo:mongo@mongo:27017/security?authSource=admin"));

    Future<Void> future = startFuture.future();
    future.compose(v -> createCollections(mongoClient))
        .onComplete(v -> logger.info("[start] createCollections completed"));

    eventBus.consumer("mongo-create", request -> {
      Single.fromCallable(() -> {
        JsonObject requestBody = (JsonObject) request.body();
        return requestBody;
      }).map(json -> MongoDTO.fromJson(json)).flatMap(dto -> mongoService.create(dto))
          .subscribe(id -> request.reply(new JsonObject().put("id", id)));
    });
    startFuture.complete();
  }

  private Future<Void> createCollections(MongoClient mongoClient) {
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
}
