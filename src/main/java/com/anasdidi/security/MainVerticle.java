package com.anasdidi.security;

import java.util.ArrayList;
import java.util.List;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.mongo.MongoClient;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    getMongoClient("mongodb://mongo:mongo@mongo:27017/security?authSource=admin");
    vertx.createHttpServer().requestHandler(req -> {
      req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x!");
    }).listen(5000).subscribe(server -> {
      System.out.println("HTTP server started on port 5000");
      startPromise.complete();
    }, error -> {
      startPromise.fail(error);
    });
  }

  private MongoClient getMongoClient(String connectionString) {
    MongoClient mongoClient = MongoClient.create(vertx, new JsonObject()//
        .put("connection_string", connectionString));

    mongoClient.rxGetCollections().subscribe(collectionList -> {
      List<Completable> createCollection = new ArrayList<>();
      if (!collectionList.contains("users")) {
        createCollection.add(mongoClient.rxCreateCollection("users"));
      }

      if (!createCollection.isEmpty()) {
        Completable.merge(createCollection).subscribe();
      }
    });

    return mongoClient;
  }
}
