package com.anasdidi.security.api.jwt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.anasdidi.security.common.AppConfig;
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

public class JwtVerticle extends AbstractVerticle {

  private final Logger logger = LogManager.getLogger(JwtController.class);
  private final Router mainRouter;
  private final JWTAuth jwtAuth;
  private final MongoClient mongoClient;
  private final JwtService jwtService;
  private final JwtValidator jwtValidator;
  private final JwtController jwtController;

  public JwtVerticle(Router mainRouter, EventBus eventBus, JWTAuth jwtAuth,
      MongoClient mongoClient) {
    this.mainRouter = mainRouter;
    this.jwtAuth = jwtAuth;
    this.mongoClient = mongoClient;
    this.jwtService = new JwtService(jwtAuth, mongoClient);
    this.jwtValidator = new JwtValidator();
    this.jwtController = new JwtController(eventBus, jwtService, jwtValidator);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    AppConfig appConfig = AppConfig.instance();

    if (!appConfig.getIsTest()) {
      configureMongoCollection(startPromise);
    }

    Router router = Router.router(vertx);

    // No need token bearer
    router.post("/login").handler(jwtController::doLogin);

    // Need token bearer
    router.route().handler(JWTAuthHandler.create(jwtAuth));
    router.get("/check").handler(jwtController::doCheck);
    router.get("/logout").handler(jwtController::doLogout);
    router.post("/refresh").handler(jwtController::doRefresh);

    mainRouter.mountSubRouter(JwtConstants.REQUEST_URI, router);

    logger.info("[start] Deployed success");
    startPromise.complete();
  }

  void configureMongoCollection(Promise<Void> startPromise) {
    final String TAG = "configureMongoCollection";

    mongoClient.getCollections(collectionList -> {
      if (collectionList.succeeded()) {
        Set<String> resultSet = new HashSet<>(collectionList.result());

        if (!resultSet.contains(JwtConstants.COLLECTION_NAME)) {
          mongoClient.createCollection(JwtConstants.COLLECTION_NAME, result -> {
            if (result.succeeded()) {
              logger.info("[{}] Mongo create collection '{}' succeed.", TAG,
                  JwtConstants.COLLECTION_NAME);
            } else {
              logger.error("[{}] Mongo create collection '{}' failed!", TAG,
                  JwtConstants.COLLECTION_NAME);
              startPromise.fail(result.cause());
            }
          });
        } else {
          logger.info("[{}] Mongo collection '{}' found.", TAG, JwtConstants.COLLECTION_NAME);
        }

        try {
          configureMongoCollectionIndexes(startPromise);
        } catch (Exception e) {
          startPromise.fail(e);
        }
      } else {
        logger.error("[{}] Mongo get collection list failed!", TAG);
        startPromise.fail(collectionList.cause());
      }
    });
  }

  void configureMongoCollectionIndexes(Promise<Void> startPromise) throws Exception {
    final String TAG = "configureMongoCollectionIndexes";
    AppConfig appConfig = AppConfig.instance();

    mongoClient.rxListIndexes(JwtConstants.COLLECTION_NAME).subscribe(indexList -> {
      List<Completable> completables = new ArrayList<>();

      indexList.stream().map(o -> (JsonObject) o).forEach(index -> {
        String indexName = index.getString("name");

        if (!indexName.startsWith("_id")) {
          completables.add(mongoClient.rxDropIndex(JwtConstants.COLLECTION_NAME, indexName));
        }
      });

      completables.add(mongoClient.rxCreateIndexWithOptions(JwtConstants.COLLECTION_NAME,
          new JsonObject().put("issuedDate", 1), new IndexOptions().name("ttl_issuedDate")
              .expireAfter(appConfig.getRefreshTokenExpireInMinutes(), TimeUnit.MINUTES)));

      Completable.concat(completables).subscribe(() -> {
        logger.info("[{}:{}] Mongo create index succeed.", TAG, JwtConstants.COLLECTION_NAME);
      }, e -> {
        logger.error("[{}:{}] Mongo create index failed!", TAG, JwtConstants.COLLECTION_NAME);
        startPromise.fail(e);
      });
    }, e -> {
      logger.error("[{}:{}] Mongo get index list failed!", TAG, JwtConstants.COLLECTION_NAME);
      startPromise.fail(e);
    });
  }
}
