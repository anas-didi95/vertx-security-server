package com.anasdidi.security;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.domain.mongo.MongoVerticle;
import com.anasdidi.security.domain.user.UserVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.rxjava3.config.ConfigRetriever;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

  private final static Logger logger = LogManager.getLogger(MainVerticle.class);

  public MainVerticle() {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        Log4j2LogDelegateFactory.class.getName());
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    getConfigRetriever().rxGetConfig().subscribe(json -> {
      ApplicationConfig config = ApplicationConfig.create(json);
      logger.info("[start] Load {}", config);

      Router router = Router.router(vertx);
      router.route().handler(BodyHandler.create());
      MongoClient mongoClient = MongoClient.create(vertx, new JsonObject()//
          .put("connection_string", config.getMongoConnectionString()));
      List<Single<String>> deployer = new ArrayList<>();
      deployer.add(deployVerticle(new MongoVerticle()));
      deployer.add(deployVerticle(new UserVerticle(router, mongoClient)));

      Single.mergeDelayError(deployer).toList().subscribe(verticleList -> {
        logger.info("[start] Total deployed verticle: {}", verticleList.size());
        vertx.createHttpServer().requestHandler(router)
            .listen(config.getAppPort(), config.getAppHost()).subscribe(server -> {
              logger.info("[start] HTTP server started on {}:{}", config.getAppHost(),
                  config.getAppPort());
              startFuture.complete();
            }, error -> startFuture.fail(error));
      }, error -> startFuture.fail(error));
    });
  }

  private ConfigRetriever getConfigRetriever() {
    List<ConfigStoreOptions> storeList = new ArrayList<>();
    storeList.add(new ConfigStoreOptions().setType("env")
        .setConfig(new JsonObject().put("keys", ApplicationConfig.getKeyList())));

    return ConfigRetriever.create(vertx, new ConfigRetrieverOptions().setStores(storeList));
  }

  private Single<String> deployVerticle(Verticle verticle) {
    return vertx.rxDeployVerticle(verticle)
        .doOnSuccess(
            id -> logger.info("[deployVerticle] {} OK: {}", verticle.getClass().getName(), id))
        .doOnError(error -> logger.error("[deployVerticle {} FAILED! {}",
            verticle.getClass().getName(), error));
  }
}
