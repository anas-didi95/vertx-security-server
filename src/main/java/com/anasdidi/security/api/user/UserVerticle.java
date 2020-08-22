package com.anasdidi.security.api.user;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;

public class UserVerticle extends AbstractVerticle {

  private final Router mainRouter;
  private final MongoClient mongoClient;
  private final UserService userService;
  private final UserController userController;

  public UserVerticle(Router mainRouter, MongoClient mongoClient) {
    this.mainRouter = mainRouter;
    this.mongoClient = mongoClient;
    this.userService = new UserService(mongoClient);
    this.userController = new UserController(userService);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    configureMongoCollection(startPromise);

    Router router = Router.router(vertx);
    router.post("/").handler(userController::create);

    mainRouter.mountSubRouter("/api/users", router);

    System.out.println("[UserVerticle:start] Deployed success");
    startPromise.complete();
  }

  void configureMongoCollection(Promise<Void> startPromise) {
    mongoClient.getCollections(collectionList -> {
      if (collectionList.succeeded()) {
        List<String> resultList = collectionList.result().stream()//
            .filter(collection -> collection.equals(UserConstants.COLLECTION_NAME))//
            .collect(Collectors.toList());

        if (resultList.isEmpty()) {
          mongoClient.createCollection(UserConstants.COLLECTION_NAME, result -> {
            if (result.succeeded()) {
              System.out.println(
                  "[UserVerticle:start] Mongo create collection '" + UserConstants.COLLECTION_NAME + "' succeed.");
            } else {
              System.err.println(
                  "[UserVerticle:start] Mongo create collection '" + UserConstants.COLLECTION_NAME + "' failed.");
              startPromise.fail(result.cause());
            }
          });
        }

        configureMongoCollectionIndexes(startPromise);
      } else {
        System.err.println("[UserVerticle:start] Mongo get collection list failed!");
        startPromise.fail(collectionList.cause());
      }
    });
  }

  void configureMongoCollectionIndexes(Promise<Void> startPromise) {
    mongoClient.listIndexes(UserConstants.COLLECTION_NAME, indexList -> {
      if (indexList.succeeded()) {
        @SuppressWarnings({ "unchecked" })
        Set<String> indexSet = new HashSet<>(indexList.result().getList());

        String idx1 = "idx_username";
        if (!indexSet.contains(idx1)) {
          mongoClient.rxCreateIndexWithOptions(//
              UserConstants.COLLECTION_NAME, //
              new JsonObject().put("username", 1), //
              new IndexOptions().name(idx1).unique(true))//
              .subscribe(() -> System.out.println("[UserVerticle:start] Mongo create index '" + idx1 + "' succeed."));
        }
      } else {
        System.err.println("[UserVerticle:start] Mongo get index list failed!");
        startPromise.fail(indexList.cause());
      }
    });
  }
}
