package com.anasdidi.security.api.user;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.anasdidi.security.common.CommonConstants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;

public class UserVerticle extends AbstractVerticle {

  private final Logger logger = LogManager.getLogger(UserVerticle.class);
  private final Router mainRouter;
  private final MongoClient mongoClient;
  private final UserValidator userValidator;
  private final UserService userService;
  private final UserController userController;

  public UserVerticle(Router mainRouter, MongoClient mongoClient) {
    this.mainRouter = mainRouter;
    this.mongoClient = mongoClient;
    this.userValidator = new UserValidator();
    this.userService = new UserService(mongoClient);
    this.userController = new UserController(userValidator, userService);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    configureMongoCollection(startPromise);

    Router router = Router.router(vertx);
    router.post("/").handler(userController::create);
    router.put("/:id").handler(userController::update);
    router.delete("/:id").handler(userController::delete);
    mainRouter.mountSubRouter("/api/users", router);

    vertx.eventBus().consumer(CommonConstants.EVT_USER_READ_USERNAME, userController::doUserReadUsername);

    logger.info("[start] Deployed success");
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
              logger.info("[configureMongoCollection] Mongo create collection '{}' succeed.",
                  UserConstants.COLLECTION_NAME);
            } else {
              logger.error("[configureMongoCollection] Mongo create collection '{}' failed!",
                  UserConstants.COLLECTION_NAME);
              startPromise.fail(result.cause());
            }
          });
        }

        configureMongoCollectionIndexes(startPromise);
      } else {
        logger.error("[configureMongoCollection] Mongo get collection list failed!");
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
              .subscribe(() -> logger.info("[configureMongoCollectionIndexes] Mongo create index '{}' succeed.", idx1));
        }
      } else {
        logger.error("[configureMongoCollectionIndexes] Mongo get index list failed!");
        startPromise.fail(indexList.cause());
      }
    });
  }
}
