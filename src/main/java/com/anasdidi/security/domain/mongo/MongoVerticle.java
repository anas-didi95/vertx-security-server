package com.anasdidi.security.domain.mongo;

import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationConstants.EventMongo;
import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;

public class MongoVerticle extends BaseVerticle {

  private final static Logger logger = LogManager.getLogger(MongoVerticle.class);
  private final MongoService mongoService;
  private final MongoHandler mongoHandler;

  public MongoVerticle() {
    this.mongoService = new MongoService();
    this.mongoHandler = new MongoHandler(mongoService);
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    super.start(startFuture);
    mongoService.setMongoClient(getMongoClient());

    logger.info("[start] Verticle started");
    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return null;
  }

  @Override
  protected String getPermission() {
    return null;
  }

  @Override
  protected void setHandler(Router router, EventBus eventBus, JWTAuthHandler jwtAuthHandler,
      Handler<RoutingContext> jwtAuthzHandler) {
    eventBus.consumer(EventMongo.MONGO_CREATE.toString(), mongoHandler::create);
    eventBus.consumer(EventMongo.MONGO_UPDATE.toString(), mongoHandler::update);
    eventBus.consumer(EventMongo.MONGO_DELETE.toString(), mongoHandler::delete);
    eventBus.consumer(EventMongo.MONGO_READ.toString(), mongoHandler::read);
  }

  private MongoClient getMongoClient() {
    ApplicationConfig config = ApplicationConfig.instance();
    return MongoClient.create(vertx,
        new JsonObject().put("connection_string", config.getMongoConnectionString()));
  }
}
