package com.anasdidi.security;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.domain.mongo.MongoVerticle;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.rxjava3.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    List<Single<String>> deployer = new ArrayList<>();
    deployer.add(deployVerticle(new MongoVerticle()));

    Single.mergeDelayError(deployer).toList().subscribe(verticleList -> {
      System.out.println("[start] Total deployed verticle: " + verticleList.size());
      vertx.createHttpServer().requestHandler(req -> {
        req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x!");
      }).listen(5000).subscribe(server -> {
        System.out.println("HTTP server started on port 5000");
        startFuture.complete();
      }, error -> startFuture.fail(error));
    }, error -> startFuture.fail(error));
  }

  private Single<String> deployVerticle(Verticle verticle) {
    return vertx.rxDeployVerticle(verticle)
        .doOnSuccess(id -> System.out
            .println("[deployVerticle] " + verticle.getClass().getName() + " OK: " + id))
        .doOnError(error -> System.err
            .println("[deployVerticle] " + verticle.getClass().getName() + " FAILED! " + error));
  }
}
