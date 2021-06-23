package com.anasdidi.security.domain.mongo;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationConstants;
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

public class MongoVerticle extends BaseVerticle {

  private final static Logger logger = LogManager.getLogger(MongoVerticle.class);
  private final MongoService mongoService;
  private final MongoEvent mongoEvent;
  private MongoClient mongoClient;

  public MongoVerticle() {
    this.mongoService = new MongoService();
    this.mongoEvent = new MongoEvent(mongoService);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    mongoService.setMongoClient(getMongoClient());

    Future<Void> future = startFuture.future();
    future.compose(v -> Future.succeededFuture(getMongoClient()))
        .onComplete(v -> logger.info("[start] Get mongo client completed"));
    future.compose(v -> createCollections(getMongoClient()))
        .onComplete(v -> logger.info("[start] Create collections completed"))
        .compose(v -> createIndexes(getMongoClient()))
        .onComplete(v -> logger.info("[start] Create indexes completed"));
    future.compose(v -> setEvent(vertx.eventBus()))
        .onComplete(v -> logger.info("[start] Set event completed"));

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

  private MongoClient getMongoClient() {
    if (mongoClient == null) {
      ApplicationConfig config = ApplicationConfig.instance();
      this.mongoClient = MongoClient.create(vertx,
          new JsonObject().put("connection_string", config.getMongoConnectionString()));
    }
    return mongoClient;
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

  private Future<Void> createIndexes(MongoClient mongoClient) {
    return Future.future(promise -> {
      mongoClient.rxListIndexes("users").subscribe(indexList -> {
        List<Completable> completableList = new ArrayList<>();

        indexList.stream().map(o -> (JsonObject) o).forEach(index -> {
          String indexName = index.getString("name");
          if (!indexName.startsWith("_id")) {
            completableList.add(mongoClient.rxDropIndex("users", indexName));
          }
        });

        completableList
            .add(mongoClient.rxCreateIndexWithOptions("users", new JsonObject().put("username", 1),
                new IndexOptions().name("uq_username").unique(true)));

        Completable.mergeDelayError(completableList).subscribe(() -> {
          logger.info("[createIndexes] Done");
          promise.complete();
        }, error -> promise.fail(error));
      }, error -> promise.fail(error));
    });
  }

  private Future<Void> setEvent(EventBus eventBus) {
    return Future.future(promise -> {
      eventBus.consumer(ApplicationConstants.Event.MONGO_CREATE.address,
          request -> mongoEvent.create(request));
      promise.complete();
    });
  }
}
