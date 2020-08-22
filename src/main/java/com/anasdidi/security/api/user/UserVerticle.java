package com.anasdidi.security.api.user;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

public class UserVerticle extends AbstractVerticle {

  private final Router mainRouter;
  private final MongoClient mongoClient;

  public UserVerticle(Router mainRouter, MongoClient mongoClient) {
    this.mainRouter = mainRouter;
    this.mongoClient = mongoClient;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    mongoClient.getCollections(collectionList -> {
      if (collectionList.succeeded()) {
        String collectionName = "users";
        List<String> resultList = collectionList.result().stream()//
            .filter(collection -> collection.equals(collectionName))//
            .collect(Collectors.toList());

        if (resultList.isEmpty()) {
          mongoClient.createCollection(collectionName, (result) -> {
            if (result.succeeded()) {
              System.out.println("[UserVerticle:start] Mongo create collection 'users' succeed.");
            } else {
              System.err.println("[UserVerticle:start] Mongo create collection 'users' failed.");
              startPromise.fail(result.cause());
            }
          });
        }
      } else {
        System.err.println("[UserVerticle:start] Mongo get collection list failed!");
        startPromise.fail(collectionList.cause());
      }
    });

    Router router = Router.router(vertx);
    router.post("/").handler(this::create);

    mainRouter.mountSubRouter("/api/users", router);

    System.out.println("[UserVerticle:start] Deployed success");
    startPromise.complete();
  }

  void create(RoutingContext routingContext) {
    Single.fromCallable(() -> {
      System.out.println("create:" + routingContext.getBodyAsJson().encodePrettily());
      return routingContext.getBodyAsJson();
    }).map(json -> {
      System.out.println("create:map toVO");
      return UserUtils.toVO(json);
    }).flatMap(vo -> {
      System.out.println("create:flatmap rxsave");
      vo.id = UUID.randomUUID().toString().replace("-", "").toUpperCase();
      vo.version = Long.valueOf(0);
      return mongoClient.rxSave("users", UserUtils.toMongoDocument(vo))//
          .defaultIfEmpty(vo.id)//
          .toSingle();
    }).subscribe(id -> {
      System.out.println("create:send response");
      routingContext.response()//
          .putHeader("Accept", "application/json")//
          .putHeader("Content-Type", "application/json")//
          .setStatusCode(201)//
          .end(new JsonObject()//
              .put("status", new JsonObject()//
                  .put("isSuccess", true)//
                  .put("message", "Record successfully created."))//
              .put("data", new JsonObject()//
                  .put("id", id))//
              .encode());
    }, e -> {
      routingContext.response()//
          .putHeader("Accept", "application/json")//
          .putHeader("Content-Type", "application/json")//
          .setStatusCode(200)//
          .end(new JsonObject()//
              .put("status", new JsonObject()//
                  .put("isSuccess", false)//
                  .put("message", "Request failed!"))
              .put("error", e.getMessage())//
              .encode());
    });
  }
}
