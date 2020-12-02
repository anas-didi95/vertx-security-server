package com.anasdidi.security.api.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.anasdidi.security.common.AppConfig;
import com.anasdidi.security.common.CommonConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.Completable;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;

public class UserVerticle extends AbstractVerticle {

  private final Logger logger = LogManager.getLogger(UserVerticle.class);
  private final Router mainRouter;
  private final MongoClient mongoClient;
  private final JWTAuth jwtAuth;
  private final EventBus eventBus;
  private final UserValidator userValidator;
  private final UserService userService;
  private final UserController userController;

  public UserVerticle(Router mainRouter, MongoClient mongoClient, JWTAuth jwtAuth,
      EventBus eventBus) {
    this.mainRouter = mainRouter;
    this.mongoClient = mongoClient;
    this.jwtAuth = jwtAuth;
    this.eventBus = eventBus;
    this.userValidator = new UserValidator();
    this.userService = new UserService(mongoClient);
    this.userController = new UserController(userValidator, userService);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    AppConfig appConfig = AppConfig.instance();

    if (!appConfig.getIsTest()) {
      configureMongoCollection(startPromise);
    }

    Router router = Router.router(vertx);
    router.route().handler(JWTAuthHandler.create(jwtAuth));
    router.post("/").handler(userController::doCreate);
    router.put("/:id").handler(userController::doUpdate);
    router.delete("/:id").handler(userController::doDelete);
    mainRouter.mountSubRouter(UserConstants.REQUEST_URI, router);

    eventBus.consumer(CommonConstants.EVT_USER_GET_BY_USERNAME,
        userController::doGetUserByUsername);
    eventBus.consumer(CommonConstants.EVT_USER_GET_LIST, userController::doGetUserList);
    eventBus.consumer(CommonConstants.EVT_USER_GET_BY_ID, userController::doGetUserById);

    logger.info("[start] Deployed success");
    startPromise.complete();
  }

  void configureMongoCollection(Promise<Void> startPromise) {
    final String TAG = "configureMongoCollection";

    mongoClient.getCollections(collectionList -> {
      if (collectionList.succeeded()) {
        Set<String> resultSet = new HashSet<>(collectionList.result());

        if (!resultSet.contains(UserConstants.COLLECTION_NAME)) {
          mongoClient.createCollection(UserConstants.COLLECTION_NAME, result -> {
            if (result.succeeded()) {
              logger.info("[{}] Mongo create collection '{}' succeed.", TAG,
                  UserConstants.COLLECTION_NAME);
            } else {
              logger.error("[{}] Mongo create collection '{}' failed!", TAG,
                  UserConstants.COLLECTION_NAME);
              startPromise.fail(result.cause());
            }
          });
        } else {
          logger.info("[{}] Mongo collection '{}' found.", TAG, UserConstants.COLLECTION_NAME);
        }

        configureMongoCollectionIndexes(startPromise);
      } else {
        logger.error("[{}] Mongo get collection list failed!", TAG);
        startPromise.fail(collectionList.cause());
      }
    });
  }

  void configureMongoCollectionIndexes(Promise<Void> startPromise) {
    final String TAG = "configureMongoCollectionIndexes";

    mongoClient.rxListIndexes(UserConstants.COLLECTION_NAME).subscribe(indexList -> {
      List<Completable> completables = new ArrayList<>();

      indexList.stream().map(o -> (JsonObject) o).forEach(index -> {
        String indexName = index.getString("name");
        if (!indexName.startsWith("_id")) {
          completables.add(mongoClient.rxDropIndex(UserConstants.COLLECTION_NAME, indexName));
        }
      });

      completables.add(mongoClient.rxCreateIndexWithOptions(UserConstants.COLLECTION_NAME,
          new JsonObject().put("username", 1),
          new IndexOptions().name("uq_username").unique(true)));

      completables.add(mongoClient.rxCreateIndexWithOptions(UserConstants.COLLECTION_NAME,
          new JsonObject().put("telegramId", 1),
          new IndexOptions().name("uq_telegramId").unique(true)));

      Completable.concat(completables).subscribe(() -> {
        logger.info("[{}:{}] Mongo create index succeed.", TAG, UserConstants.COLLECTION_NAME);
      }, e -> {
        logger.error("[{}:{}] Mongo create index failed!", TAG, UserConstants.COLLECTION_NAME);
        startPromise.fail(e);
      });
    }, e -> {
      logger.error("[{}:{}] Mongo get index list failed!", TAG, UserConstants.COLLECTION_NAME);
      startPromise.fail(e);
    });
  }
}
