package com.anasdidi.security.api.jwt;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
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

  public JwtVerticle(Router mainRouter, EventBus eventBus, JWTAuth jwtAuth, MongoClient mongoClient, JsonObject cfg) {
    this.mainRouter = mainRouter;
    this.jwtAuth = jwtAuth;
    this.mongoClient = mongoClient;
    this.jwtService = new JwtService(jwtAuth, mongoClient, cfg);
    this.jwtValidator = new JwtValidator();
    this.jwtController = new JwtController(eventBus, jwtService, jwtValidator);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    configureMongoCollection(startPromise);

    Router router = Router.router(vertx);

    // No need token bearer
    router.post("/login").handler(jwtController::doLogin);

    // Need token bearer
    router.route().handler(JWTAuthHandler.create(jwtAuth));
    router.get("/check").handler(jwtController::doCheck);
    router.post("/refresh").handler(jwtController::doRefresh);

    mainRouter.mountSubRouter("/api/jwt", router);

    long periodicCleanup = 1000L * 60 * 1;
    vertx.setPeriodic(periodicCleanup, r -> {
      String tag = "" + System.currentTimeMillis();
      JsonObject query = new JsonObject()//
          .put("createDate", new JsonObject().put("$lt", Instant.now().minusMillis(periodicCleanup)));

      if (logger.isDebugEnabled()) {
        logger.debug("[start] {} Periodic mongo cleanup: periodic={}, query\n{}", tag, periodicCleanup,
            query.encodePrettily());
      }

      mongoClient.rxFind(JwtConstants.COLLECTION_NAME, query).subscribe(resultList -> {
        if (logger.isDebugEnabled()) {
          logger.debug("[start] {} Periodic mongo cleanup: resultList={}", tag,
              (resultList == null ? -1 : resultList.size()));
        }

        resultList.stream().forEach(result -> {
          if (logger.isDebugEnabled()) {
            logger.debug("[start] {} result={}", tag, result.getString("createDate"));
          }
          mongoClient
              .rxFindOneAndDelete(JwtConstants.COLLECTION_NAME, new JsonObject().put("_id", result.getString("_id")))
              .subscribe();
        });
      });
    });

    logger.info("[start] Deployed success");
    startPromise.complete();
  }

  void configureMongoCollection(Promise<Void> startPromise) {
    mongoClient.getCollections(collectionList -> {
      if (collectionList.succeeded()) {
        List<String> resultList = collectionList.result().stream()//
            .filter(collection -> collection.equals(JwtConstants.COLLECTION_NAME))//
            .collect(Collectors.toList());

        if (resultList.isEmpty()) {
          mongoClient.createCollection(JwtConstants.COLLECTION_NAME, result -> {
            if (result.succeeded()) {
              logger.info("[configureMongoCollection] Mongo create collection '{}' succeed.",
                  JwtConstants.COLLECTION_NAME);
            } else {
              logger.error("[configureMongoCollection] Mongo create collection '{}' failed!",
                  JwtConstants.COLLECTION_NAME);
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
  }
}
