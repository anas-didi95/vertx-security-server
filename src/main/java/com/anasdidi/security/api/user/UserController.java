package com.anasdidi.security.api.user;

import java.util.UUID;

import com.anasdidi.security.common.CommonController;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.RoutingContext;

public class UserController extends CommonController {

  private final MongoClient mongoClient;

  UserController(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  void create(RoutingContext routingContext) {
    Single<JsonObject> subscriber = Single.fromCallable(() -> {
      System.out.println("create:" + routingContext.getBodyAsJson().encodePrettily());
      return routingContext.getBodyAsJson();
    }).map(json -> {
      System.out.println("create:map toVO");
      return UserUtils.toVO(json);
    }).flatMap(vo -> {
      System.out.println("create:flatmap rxsave");
      vo.id = UUID.randomUUID().toString().replace("-", "").toUpperCase();
      vo.version = Long.valueOf(0);
      return mongoClient.rxSave("users", UserUtils.toMongoDocument(vo))//
          .defaultIfEmpty(vo.id)//
          .toSingle();
    }).map(id -> {
      System.out.println("create:construct response data");
      return new JsonObject().put("id", id);
    });

    sendResponse(subscriber, routingContext, 201, "Record successfully created.");
  }
}
