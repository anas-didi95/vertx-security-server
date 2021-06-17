package com.anasdidi.security.domain.mongo;

import java.util.ArrayList;
import java.util.List;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.mongo.MongoClient;

public class MongoVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    MongoClient mongoClient = MongoClient.create(vertx, new JsonObject()//
        .put("connection_string", "mongodb://mongo:mongo@mongo:27017/security?authSource=admin"));

    createCollections(startFuture, mongoClient);
    startFuture.complete();
  }

  private void createCollections(Promise<Void> startFuture, MongoClient mongoClient) {
    mongoClient.rxGetCollections().subscribe(collectionList -> {
      List<Completable> createCollection = new ArrayList<>();
      if (!collectionList.contains("users")) {
        createCollection.add(mongoClient.rxCreateCollection("users"));
      }

      if (!createCollection.isEmpty()) {
        Completable.merge(createCollection).subscribe(() -> {
          System.out.println("[createCollections] Collections created");
        }, error -> startFuture.fail(error));
      }
    }, error -> startFuture.fail(error));
  }
}
