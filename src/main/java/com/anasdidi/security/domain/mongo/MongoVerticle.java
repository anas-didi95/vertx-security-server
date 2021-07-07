package com.anasdidi.security.domain.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;

public class MongoVerticle extends BaseVerticle {

  private final static Logger logger = LogManager.getLogger(MongoVerticle.class);
  private final MongoService mongoService;
  private final MongoHandler mongoHandler;

  public MongoVerticle() {
    this.mongoService = new MongoService();
    this.mongoHandler = new MongoHandler(mongoService);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    super.start(startFuture);
    mongoService.setMongoClient(getMongoClient());

    Future<Void> future = startFuture.future();
    future.compose(v -> createCollections(getMongoClient()))
        .onComplete(v -> logger.info("[start] Create collections completed"))
        .compose(v -> createIndexes(getMongoClient()))
        .onComplete(v -> logger.info("[start] Create indexes completed"));

    logger.info("[start] Verticle started");
    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return null;
  }

  @Override
  public Router getRouter() {
    return null;
  }

  @Override
  protected void setHandler(Router router, EventBus eventBus, JWTAuthHandler jwtAuthHandler) {
    eventBus.consumer(EventMongo.MONGO_CREATE.toString(), mongoHandler::create);
    eventBus.consumer(EventMongo.MONGO_UPDATE.toString(), mongoHandler::update);
    eventBus.consumer(EventMongo.MONGO_DELETE.toString(), mongoHandler::delete);
  }

  private Future<Void> createCollections(MongoClient mongoClient) {
    return Future.future(promise -> {
      mongoClient.rxGetCollections().subscribe(collectionList -> {
        List<Completable> createCollection = Arrays.asList(CollectionRecord.values()).stream()
            .filter(collection -> !collectionList.contains(collection.name))
            .map(collection -> mongoClient.rxCreateCollection(collection.name))
            .collect(Collectors.toList());

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

  private Future<Void> createIndexes(MongoClient mongoClient) {
    return Future.future(promise -> {
      mongoClient.rxListIndexes(CollectionRecord.USER.name).subscribe(indexList -> {
        List<Completable> completableList = new ArrayList<>();

        indexList.stream().map(o -> (JsonObject) o).forEach(index -> {
          String indexName = index.getString("name");
          if (!indexName.startsWith("_id")) {
            completableList.add(mongoClient.rxDropIndex(CollectionRecord.USER.name, indexName));
          }
        });

        completableList.add(mongoClient.rxCreateIndexWithOptions(
            ApplicationConstants.CollectionRecord.USER.name, new JsonObject().put("username", 1),
            new IndexOptions().name("uq_username").unique(true)));

        Completable.concatDelayError(completableList).subscribe(() -> {
          logger.info("[createIndexes] Total process finished: {}", completableList.size());
          promise.complete();
        }, error -> promise.fail(error));
      }, error -> promise.fail(error));
    });
  }
}
