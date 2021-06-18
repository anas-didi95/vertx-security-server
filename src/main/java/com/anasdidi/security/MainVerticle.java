package com.anasdidi.security;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.domain.mongo.MongoVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.rxjava3.config.ConfigRetriever;
import io.vertx.rxjava3.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  private final static Logger logger = LogManager.getLogger(MainVerticle.class);

  public MainVerticle() {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        Log4j2LogDelegateFactory.class.getName());
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    ConfigRetriever retriever = ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

    retriever.rxGetConfig().subscribe(json -> {
      ApplicationConfig config = ApplicationConfig.create(json);

      List<Single<String>> deployer = new ArrayList<>();
      deployer.add(deployVerticle(new MongoVerticle()));

      Single.mergeDelayError(deployer).toList().subscribe(verticleList -> {
        logger.info("[start] Total deployed verticle: {}", verticleList.size());
        vertx.createHttpServer().requestHandler(req -> {
          req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x!");
        }).listen(5000).subscribe(server -> {
          logger.info("HTTP server started on port 5000");
          startFuture.complete();
        }, error -> startFuture.fail(error));
      }, error -> startFuture.fail(error));
    });
  }

  private Single<String> deployVerticle(Verticle verticle) {
    return vertx.rxDeployVerticle(verticle)
        .doOnSuccess(
            id -> logger.info("[deployVerticle] {} OK: {}", verticle.getClass().getName(), id))
        .doOnError(error -> logger.error("[deployVerticle {} FAILED! {}",
            verticle.getClass().getName(), error));
  }
}
