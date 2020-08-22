package com.anasdidi.security.api.user;

import com.anasdidi.security.common.CommonController;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

public class UserController extends CommonController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
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
      return userService.create(vo);
    }).map(id -> {
      System.out.println("create:construct response data");
      return new JsonObject().put("id", id);
    });

    sendResponse(subscriber, routingContext, 201, "Record successfully created.");
  }
}
