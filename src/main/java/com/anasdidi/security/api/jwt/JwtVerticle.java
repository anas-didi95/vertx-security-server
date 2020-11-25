package com.anasdidi.security.api.jwt;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.anasdidi.security.common.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    configureMongoCollection(startPromise);

    Router router = Router.router(vertx);

    // No need token bearer
    router.post("/login").handler(jwtController::doLogin);
    router.get("/refresh").handler(jwtController::doRefresh);

    // Need token bearer
    router.route().handler(JWTAuthHandler.create(jwtAuth));
    router.get("/check").handler(jwtController::doCheck);

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

    mongoClient.rxListIndexes(JwtConstants.COLLECTION_NAME).subscribe(resultList -> {
      Set<String> indexSet = resultList.stream().map(o -> (JsonObject) o)
          .map(o -> o.getString("name")).collect(Collectors.toSet());

      String indexIssuedDateTTL = "ttl_issuedDate";
      if (!indexSet.contains(indexIssuedDateTTL)) {
        mongoClient
            .rxCreateIndexWithOptions(JwtConstants.COLLECTION_NAME,
                new JsonObject().put("issuedDate", 1),
                new IndexOptions().name(indexIssuedDateTTL)
                    .expireAfter(appConfig.getRefreshTokenExpireInMinutes(), TimeUnit.MINUTES))
            .subscribe(() -> logger.info("[{}:{} Mongo create index '{}' succeed.", TAG,
                JwtConstants.COLLECTION_NAME, indexIssuedDateTTL));
      } else {
        logger.info("[{}:{}] Mongo index '{}' found.", TAG, JwtConstants.COLLECTION_NAME,
            indexIssuedDateTTL);
      }
    }, e -> {
      logger.error("[{}:{}] Mongo get index list failed!", TAG, JwtConstants.COLLECTION_NAME);
      startPromise.fail(e);
    });
  }
}
